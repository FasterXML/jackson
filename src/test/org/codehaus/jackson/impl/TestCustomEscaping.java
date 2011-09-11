package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.CharacterEscapes;
import org.codehaus.jackson.io.SerializedString;
import org.codehaus.jackson.map.ObjectMapper;

public class TestCustomEscaping  extends main.BaseTest
{
    final static int TWO_BYTE_ESCAPED = 0x111;
    final static int THREE_BYTE_ESCAPED = 0x1111;

    final static SerializedString TWO_BYTE_ESCAPED_STRING = new SerializedString("&111;");
    final static SerializedString THREE_BYTE_ESCAPED_STRING = new SerializedString("&1111;");
    
    /*
    /********************************************************
    /* Helper types
    /********************************************************
     */

    /**
     * Trivial simple custom escape definition set.
     */
    static class MyEscapes extends CharacterEscapes
    {
        
        private final int[] _asciiEscapes;

        public MyEscapes() {
            _asciiEscapes = standardAsciiEscapesForJSON();
            _asciiEscapes['a'] = 'A'; // to basically give us "\A"
            _asciiEscapes['b'] = CharacterEscapes.ESCAPE_STANDARD; // too force "\u0062"
            _asciiEscapes['d'] = CharacterEscapes.ESCAPE_CUSTOM;
        }
        
        @Override
        public int[] getEscapeCodesForAscii() {
            return _asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch)
        {
            if (ch == 'd') {
                return new SerializedString("[D]");
            }
            if (ch == TWO_BYTE_ESCAPED) {
                return TWO_BYTE_ESCAPED_STRING;
            }
            if (ch == THREE_BYTE_ESCAPED) {
                return THREE_BYTE_ESCAPED_STRING;
            }
            return null;
        }
    }
    
    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    /**
     * Test to ensure that it is possible to force escaping
     * of non-ASCII characters.
     * Related to [JACKSON-102]
     */
    public void testAboveAsciiEscapeWithReader() throws Exception
    {
        _testEscapeAboveAscii(false); // reader
    }

    public void testAboveAsciiEscapeWithUTF8Stream() throws Exception
    {
        _testEscapeAboveAscii(true); // stream (utf-8)
    }

    // // // Tests for [JACKSON-106]
    
    public void testEscapeCustomWithReader() throws Exception
    {
        _testEscapeCustom(false); // reader
    }

    public void testEscapeCustomWithUTF8Stream() throws Exception
    {
        _testEscapeCustom(true); // stream (utf-8)
    }

    // for [JACKSON-672]
    public void testEscapingViaMapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
        assertEquals(quote("\\u0101"), mapper.writeValueAsString(String.valueOf((char) 257)));
    }
    
    /*
    /********************************************************
    /* Secondary test methods
    /********************************************************
     */

    private void _testEscapeAboveAscii(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory();
        final String VALUE = "chars: [\u00A0]/[\u1234]";
        final String KEY = "fun:\u0088:\u3456";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator jgen;

        // First: output normally; should not add escaping
        if (useStream) {
            jgen = f.createJsonGenerator(bytes, JsonEncoding.UTF8);
        } else {
            jgen = f.createJsonGenerator(new OutputStreamWriter(bytes, "UTF-8"));
        }
        jgen.writeStartArray();
        jgen.writeString(VALUE);
        jgen.writeEndArray();
        jgen.close();
        String json = bytes.toString("UTF-8");
        
        assertEquals("["+quote(VALUE)+"]", json);

        // And then with forced ASCII; first, values

        bytes = new ByteArrayOutputStream();
        if (useStream) {
            jgen = f.createJsonGenerator(bytes, JsonEncoding.UTF8);
        } else {
            jgen = f.createJsonGenerator(new OutputStreamWriter(bytes, "UTF-8"));
        }
        jgen.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
        jgen.writeStartArray();
        jgen.writeString(VALUE);
        jgen.writeEndArray();
        jgen.close();
        json = bytes.toString("UTF-8");
        assertEquals("["+quote("chars: [\\u00A0]/[\\u1234]")+"]", json);

        // and then keys
        bytes = new ByteArrayOutputStream();
        if (useStream) {
            jgen = f.createJsonGenerator(bytes, JsonEncoding.UTF8);
        } else {
            jgen = f.createJsonGenerator(new OutputStreamWriter(bytes, "UTF-8"));
        }
        jgen.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
        jgen.writeStartObject();
        jgen.writeFieldName(KEY);
        jgen.writeBoolean(true);
        jgen.writeEndObject();
        jgen.close();
        json = bytes.toString("UTF-8");
        assertEquals("{"+quote("fun:\\u0088:\\u3456")+":true}", json);
    }

    private void _testEscapeCustom(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory().setCharacterEscapes(new MyEscapes());
        final String STR_IN = "[abcd/"+((char) TWO_BYTE_ESCAPED)+"/"+((char) THREE_BYTE_ESCAPED)+"]";
        final String STR_OUT = "[\\A\\u0062c[D]/"+TWO_BYTE_ESCAPED_STRING+"/"+THREE_BYTE_ESCAPED_STRING+"]";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator jgen;
        
        // First: output normally; should not add escaping
        if (useStream) {
            jgen = f.createJsonGenerator(bytes, JsonEncoding.UTF8);
        } else {
            jgen = f.createJsonGenerator(new OutputStreamWriter(bytes, "UTF-8"));
        }
        jgen.writeStartObject();
        jgen.writeStringField(STR_IN, STR_IN);
        jgen.writeEndObject();
        jgen.close();
        String json = bytes.toString("UTF-8");
        assertEquals("{"+quote(STR_OUT)+":"+quote(STR_OUT)+"}", json);
    }
}
