package main;

import org.codehaus.jackson.*;

import java.io.*;

/**
 * Set of basic unit tests that verify that the closing (or not) of
 * the underlying source occurs as expected and specified
 * by documentation.
 */
public class TestParserClosing
    extends BaseTest
{
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    public void testNoAutoCloseReader()
        throws Exception
    {
        final String DOC = "[ 1 ]";

        JsonFactory f = new JsonFactory();

        // Check the default settings
        assertTrue(f.isParserFeatureEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        // then change
        f.disableParserFeature(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        assertFalse(f.isParserFeatureEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        MyReader input = new MyReader(DOC);
        JsonParser jp = f.createJsonParser(input);

        // shouldn't be closed to begin with...
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        // normally would be closed now
        assertFalse(input.isClosed());
        // regular close won't close it either:
        jp.close();
        assertFalse(input.isClosed());

    }

    public void testAutoCloseReader() throws Exception
    {
        final String DOC = "[ 1 ]";

        JsonFactory f = new JsonFactory();
        f.enableParserFeature(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        assertTrue(f.isParserFeatureEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        MyReader input = new MyReader(DOC);
        JsonParser jp = f.createJsonParser(input);
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        // but can close half-way through
        jp.close();
        assertTrue(input.isClosed());

        // And then let's test implicit close at the end too:
        input = new MyReader(DOC);
        jp = f.createJsonParser(input);
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        assertTrue(input.isClosed());
    }

    public void testNoAutoCloseInputStream()
        throws Exception
    {
        final String DOC = "[ 1 ]";
        JsonFactory f = new JsonFactory();

        f.disableParserFeature(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        MyStream input = new MyStream(DOC.getBytes("UTF-8"));
        JsonParser jp = f.createJsonParser(input);

        // shouldn't be closed to begin with...
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        // normally would be closed now
        assertFalse(input.isClosed());
        // regular close won't close it either:
        jp.close();
        assertFalse(input.isClosed());
    }

    /*
    ///////////////////////////////////////////////
    // Helper classes
    ///////////////////////////////////////////////
     */

    final static class MyReader extends StringReader
    {
        boolean mIsClosed = false;

        public MyReader(String contents) {
            super(contents);
        }

        public void close() {
            mIsClosed = true;
            super.close();
        }

        public boolean isClosed() { return mIsClosed; }
    }

    final static class MyStream extends ByteArrayInputStream
    {
        boolean mIsClosed = false;

        public MyStream(byte[] data) {
            super(data);
        }

        public void close() throws IOException {
            mIsClosed = true;
            super.close();
        }

        public boolean isClosed() { return mIsClosed; }
    }

}
