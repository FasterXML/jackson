package main;

import org.codehaus.jackson.*;

import java.io.*;

/**
 * Set of basic unit tests that verify that the closing (or not) of
 * the underlying target occurs as expected and specified
 * by documentation.
 */
public class TestGeneratorClosing
    extends BaseTest
{
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    public void testNoAutoCloseGenerator()
        throws Exception
    {
        JsonFactory f = new JsonFactory();

        // Check the default settings
        assertTrue(f.isGeneratorFeatureEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET));
        // then change
        f.disableGeneratorFeature(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        assertFalse(f.isGeneratorFeatureEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET));
        MyWriter output = new MyWriter();
        JsonGenerator jg = f.createJsonGenerator(output);

        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        jg.writeNumber(39);
        // regular close won't close it either:
        jg.close();
        assertFalse(output.isClosed());
    }

    public void testCloseGenerator()
        throws Exception
    {
        JsonFactory f = new JsonFactory();

        f.enableGeneratorFeature(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        MyWriter output = new MyWriter();
        JsonGenerator jg = f.createJsonGenerator(output);

        // shouldn't be closed to begin with...
        assertFalse(output.isClosed());
        jg.writeNumber(39);
        // but close() should now close the writer
        jg.close();
        assertTrue(output.isClosed());
    }

    public void testNoAutoCloseOutputStream()
        throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.disableGeneratorFeature(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        MyStream output = new MyStream();
        JsonGenerator jg = f.createJsonGenerator(output, JsonEncoding.UTF8);

        assertFalse(output.isClosed());
        jg.writeNumber(39);
        jg.close();
        assertFalse(output.isClosed());
    }

    /*
    ///////////////////////////////////////////////
    // Helper classes
    ///////////////////////////////////////////////
     */

    final static class MyWriter extends StringWriter
    {
        boolean mIsClosed = false;

        public MyWriter() { }

        public void close() throws IOException {
            mIsClosed = true;
            super.close();
        }
        public boolean isClosed() { return mIsClosed; }
    }

    final static class MyStream extends ByteArrayOutputStream
    {
        boolean mIsClosed = false;

        public MyStream() { }

        public void close() throws IOException {
            mIsClosed = true;
            super.close();
        }
        public boolean isClosed() { return mIsClosed; }
    }

}
