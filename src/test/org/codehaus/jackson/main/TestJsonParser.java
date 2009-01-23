package org.codehaus.jackson.main;

import java.io.*;

import org.codehaus.jackson.*;

public class TestJsonParser
    extends main.BaseTest
{
    public void testNextValue()
        throws IOException
    {
        // Let's test both byte-backed and Reader-based one
        _testNextValue(false);
        _testNextValue(true);
    }

    /*
    //////////////////////////////////////////////
    // Actual test methods
    //////////////////////////////////////////////
     */

    private void  _testNextValue(boolean useStream)
        throws IOException
    {
        // first array, no change to default
        JsonParser jp = _getParser("[ 1, 2, 3, 4 ]", useStream);
        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        for (int i = 1; i <= 4; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
            assertEquals(i, jp.getIntValue());
        }
        assertToken(JsonToken.END_ARRAY, jp.nextValue());
        assertNull(jp.nextValue());
        jp.close();

        // then Object, is different
        jp = _getParser("{ \"3\" :3, \"4\": 4, \"5\" : 5 }", useStream);
        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        for (int i = 3; i <= 5; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
            assertEquals(i, jp.getIntValue());
        }
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertNull(jp.nextValue());
        jp.close();

        // and then mixed...
        jp = _getParser("[ true, [ ], { \"a\" : 3 } ]", useStream);

        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        assertToken(JsonToken.VALUE_TRUE, jp.nextValue());
        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        assertToken(JsonToken.END_ARRAY, jp.nextValue());

        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertToken(JsonToken.END_ARRAY, jp.nextValue());

        assertNull(jp.nextValue());
        jp.close();
    }

    private JsonParser _getParser(String doc, boolean useStream)
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        if (useStream) {
            return jf.createJsonParser(doc.getBytes("UTF-8"));
        }
        return jf.createJsonParser(new StringReader(doc));
    }
}
