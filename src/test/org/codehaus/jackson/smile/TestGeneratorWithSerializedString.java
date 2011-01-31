package org.codehaus.jackson.smile;

import java.io.ByteArrayOutputStream;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.io.SerializedString;

public class TestGeneratorWithSerializedString extends SmileTestBase
{
    final static String NAME_WITH_QUOTES = "\"name\"";
    final static String NAME_WITH_LATIN1 = "P\u00f6ll\u00f6";

    private final SerializedString quotedName = new SerializedString(NAME_WITH_QUOTES);
    private final SerializedString latin1Name = new SerializedString(NAME_WITH_LATIN1);
    
    public void testSimple() throws Exception
    {
        SmileFactory sf = new SmileFactory();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jgen = sf.createJsonGenerator(out);
        _writeSimple(jgen);
        jgen.close();
        byte[] smileB = out.toByteArray();
        _verifySimple(sf.createJsonParser(smileB));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void _writeSimple(JsonGenerator jgen) throws Exception
    {
        // Let's just write array of 2 objects
        jgen.writeStartArray();

        jgen.writeStartObject();
        jgen.writeFieldName(quotedName);
        jgen.writeString("a");
        jgen.writeFieldName(latin1Name);
        jgen.writeString("b");
        jgen.writeEndObject();

        jgen.writeStartObject();
        jgen.writeFieldName(latin1Name);
        jgen.writeString("c");
        jgen.writeFieldName(quotedName);
        jgen.writeString("d");
        jgen.writeEndObject();
        
        jgen.writeEndArray();
    }

    private void _verifySimple(JsonParser jp) throws Exception
    {
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(NAME_WITH_QUOTES, jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("a", jp.getText());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(NAME_WITH_LATIN1, jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("b", jp.getText());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(NAME_WITH_LATIN1, jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("c", jp.getText());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(NAME_WITH_QUOTES, jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("d", jp.getText());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
    }
}
