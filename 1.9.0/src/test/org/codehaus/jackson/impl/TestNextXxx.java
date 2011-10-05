package org.codehaus.jackson.impl;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.SerializableString;
import org.codehaus.jackson.io.SerializedString;

public class TestNextXxx
    extends main.BaseTest
{
    // [JACKSON-653]
    public void testIsNextTokenName() throws Exception
    {
        _testIsNextTokenName1(false);
        _testIsNextTokenName1(true);
        _testIsNextTokenName2(false);
        _testIsNextTokenName2(true);
    }

    private void _testIsNextTokenName1(boolean useStream) throws Exception
    {
        final String DOC = "{\"name\":123,\"name2\":14,\"x\":\"name\"}";
        JsonFactory jf = new JsonFactory();
        JsonParser jp = useStream ?
            jf.createJsonParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : jf.createJsonParser(new StringReader(DOC));
        SerializedString NAME = new SerializedString("name");
        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.START_OBJECT, jp.getCurrentToken());
        assertTrue(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals(NAME.getValue(), jp.getCurrentName());
        assertEquals(NAME.getValue(), jp.getText());
        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.getCurrentToken());
        assertEquals(123, jp.getIntValue());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("name2", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("x", jp.getCurrentName());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_STRING, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertNull(jp.getCurrentToken());

        jp.close();
    }

    private void _testIsNextTokenName2(boolean useStream) throws Exception
    {
        final String DOC = "{\"name\":123,\"name2\":14,\"x\":\"name\"}";
        JsonFactory jf = new JsonFactory();
        JsonParser jp = useStream ?
            jf.createJsonParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
            : jf.createJsonParser(new StringReader(DOC));
        SerializableString NAME = new SerializedString("name");
        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.START_OBJECT, jp.getCurrentToken());
        assertTrue(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals(NAME.getValue(), jp.getCurrentName());
        assertEquals(NAME.getValue(), jp.getText());
        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.getCurrentToken());
        assertEquals(123, jp.getIntValue());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("name2", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.FIELD_NAME, jp.getCurrentToken());
        assertEquals("x", jp.getCurrentName());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.VALUE_STRING, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());

        assertFalse(jp.nextFieldName(NAME));
        assertNull(jp.getCurrentToken());

        jp.close();
    }
    
}
