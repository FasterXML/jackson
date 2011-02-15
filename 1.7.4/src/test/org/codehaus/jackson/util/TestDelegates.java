package org.codehaus.jackson.util;

import java.io.*;

import org.codehaus.jackson.*;

public class TestDelegates extends main.BaseTest
{
    /**
     * Test default, non-overridden parser delegate.
     */
    public void testParserDelegate() throws IOException
    {
        JsonParser jp = new JsonFactory().createJsonParser("[ 1, true ]");
        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals("[", jp.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(1, jp.getIntValue());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertTrue(jp.getBooleanValue());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
        assertTrue(jp.isClosed());
    }

    /**
     * Test default, non-overridden generator delegate.
     */
    public void testGeneratorDelegate() throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator jg = new JsonFactory().createJsonGenerator(sw);
        jg.writeStartArray();
        jg.writeNumber(13);
        jg.writeNull();
        jg.writeBoolean(false);
        jg.writeEndArray();
        jg.close();
        assertTrue(jg.isClosed());        
        assertEquals("[13,null,false]", sw.toString());
    }
}
