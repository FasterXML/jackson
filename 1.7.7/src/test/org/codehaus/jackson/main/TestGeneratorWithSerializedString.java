package org.codehaus.jackson.main;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.SerializedString;

public class TestGeneratorWithSerializedString
    extends main.BaseTest
{
    final static String NAME_WITH_QUOTES = "\"name\"";
    final static String NAME_WITH_LATIN1 = "P\u00f6ll\u00f6";

    private final SerializedString quotedName = new SerializedString(NAME_WITH_QUOTES);
    private final SerializedString latin1Name = new SerializedString(NAME_WITH_LATIN1);
    
    public void testSimple() throws Exception
    {
        JsonFactory jf = new JsonFactory();

        // First using char-backed generator
        StringWriter sw = new StringWriter();
        JsonGenerator jgen = jf.createJsonGenerator(sw);
        _writeSimple(jgen);
        jgen.close();
        String json = sw.toString();
        _verifySimple(jf.createJsonParser(json));

        // then using UTF-8
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        jgen = jf.createJsonGenerator(out, JsonEncoding.UTF8);
        _writeSimple(jgen);
        jgen.close();
        byte[] jsonB = out.toByteArray();
        _verifySimple(jf.createJsonParser(jsonB));
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
