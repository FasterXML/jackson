package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;

import static org.junit.Assert.*;

/**
 * Tests for verifying that accessing base64 encoded content works ok.
 */
public class TestJsonParserBinary
    extends main.BaseTest
{
    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    public void testSimple()
        throws IOException
    {
        // let's test reader (char) based first, then stream (byte)
        _testSimple(false);
        _testSimple(true);
    }

    public void testInArray()
        throws IOException
    {
        // let's test reader (char) based first, then stream (byte)
        _testInArray(false);
        _testInArray(true);
    }

    public void testWithEscaped() throws IOException
    {
        // let's test reader (char) based first, then stream (byte)
        _testEscaped(false);
        _testEscaped(true);
    }
    
    /*
    /**********************************************************************
    /* Actual test methods
    /**********************************************************************
     */

    private void _testSimple(boolean useStream)
        throws IOException
    {
        /* The usual sample input string, from Thomas Hobbes's "Leviathan"
         * (via Wikipedia)
         */
        final String RESULT = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
        final byte[] RESULT_BYTES = RESULT.getBytes("US-ASCII");

        // And here's what should produce it...
        final String INPUT_STR = 
 "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz"
+"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg"
+"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu"
+"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo"
+"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
            ;

        final String DOC = "\""+INPUT_STR+"\"";
        JsonParser jp = _getParser(DOC, useStream);

        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        byte[] data = jp.getBinaryValue();
        assertNotNull(data);
        assertArrayEquals(RESULT_BYTES, data);
    }

    private void _testInArray(boolean useStream)
        throws IOException
    {
        JsonFactory jf = new JsonFactory();

        final int entryCount = 7;

        StringWriter sw = new StringWriter();
        JsonGenerator jg = jf.createJsonGenerator(sw);
        jg.writeStartArray();

        byte[][] entries = new byte[entryCount][];
        for (int i = 0; i < entryCount; ++i) {
            byte[] b = new byte[200 + i * 100];
            for (int x = 0; x < b.length; ++x) {
                b[x] = (byte) (i + x);
            }
            entries[i] = b;
            jg.writeBinary(b);
        }

        jg.writeEndArray();
        jg.close();

        JsonParser jp = _getParser(sw.toString(), useStream);

        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        for (int i = 0; i < entryCount; ++i) {
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            byte[] b = jp.getBinaryValue();
            assertArrayEquals(entries[i], b);
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
    }

    private void _testEscaped(boolean useStream) throws IOException
    {
        // Input: "Test!" -> "VGVzdCE="

        // First, try with embedded linefeed half-way through:

        String DOC = quote("VGVz\\ndCE="); // note: must double-quote to get linefeed
        JsonParser jp = _getParser(DOC, useStream);
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        byte[] b = jp.getBinaryValue();
        assertEquals("Test!", new String(b, "US-ASCII"));
        assertNull(jp.nextToken());
        jp.close();

        // and then with escaped chars
//        DOC = quote("V\\u0047V\\u007AdCE="); // note: must escape backslash...
        DOC = quote("V\\u0047V\\u007AdCE="); // note: must escape backslash...
        jp = _getParser(DOC, useStream);
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        b = jp.getBinaryValue();
        assertEquals("Test!", new String(b, "US-ASCII"));
        assertNull(jp.nextToken());
        jp.close();
    }
    
    /*
    /**********************************************************************
    /* Other helper methods
    /**********************************************************************
     */
    
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
