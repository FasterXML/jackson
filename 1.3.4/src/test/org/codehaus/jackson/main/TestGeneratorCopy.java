package org.codehaus.jackson.main;

import main.BaseTest;

import org.codehaus.jackson.*;

import java.io.*;

/**
 * Set of basic unit tests for verifying that copy-through methods
 * of {@link JsonGenerator} work as expected.
 */
public class TestGeneratorCopy
    extends BaseTest
{
    public void testCopyRootTokens()
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        final String DOC = "\"text\\non two lines\" true false 2.0";
        JsonParser jp = jf.createJsonParser(new StringReader(DOC));
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createJsonGenerator(sw);

        JsonToken t;

        while ((t = jp.nextToken()) != null) {
            gen.copyCurrentEvent(jp);
            // should not change parser state:
            assertToken(t, jp.getCurrentToken());
        }
        jp.close();
        gen.close();

        assertEquals("\"text\\non two lines\" true false 2.0", sw.toString());
    }

    public void testCopyArrayTokens()
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        final String DOC = "123 [ 1, null, [ false ] ]";
        JsonParser jp = jf.createJsonParser(new StringReader(DOC));
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createJsonGenerator(sw);

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        gen.copyCurrentEvent(jp);
        // should not change parser state:
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.getCurrentToken());
        assertEquals(123, jp.getIntValue());

        // And then let's copy the array
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        gen.copyCurrentStructure(jp);
        // which will advance parser to matching close Array
        assertToken(JsonToken.END_ARRAY, jp.getCurrentToken());
        jp.close();
        gen.close();

        assertEquals("123 [1,null,[false]]", sw.toString());
    }

    public void testCopyObjectTokens()
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        final String DOC = "{ \"a\":1, \"b\":[{ \"c\" : null }] }";
        JsonParser jp = jf.createJsonParser(new StringReader(DOC));
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createJsonGenerator(sw);

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        gen.copyCurrentStructure(jp);
        // which will advance parser to matching end Object
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());
        jp.close();
        gen.close();

        assertEquals("{\"a\":1,\"b\":[{\"c\":null}]}", sw.toString());
    }
}
