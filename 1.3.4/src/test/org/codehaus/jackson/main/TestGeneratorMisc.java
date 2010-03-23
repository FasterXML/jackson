package org.codehaus.jackson.main;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.MappingJsonFactory;

/**
 * Set of basic unit tests for verifying basic generator
 * features.
 */
public class TestGeneratorMisc
    extends main.BaseTest
{
    final static class Pojo
    {
        public int getX() { return 4; }
    }

    /*
    ///////////////////////////////////////////////
    // Tests for closing, status
    ///////////////////////////////////////////////
     */

    public void testIsClosed()
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        for (int i = 0; i < 2; ++i) {
            boolean stream = ((i & 1) == 0);
            JsonGenerator jg = stream ?
                jf.createJsonGenerator(new StringWriter())
                : jf.createJsonGenerator(new ByteArrayOutputStream(), JsonEncoding.UTF8)
                ;
            assertFalse(jg.isClosed());
            jg.writeStartArray();
            jg.writeNumber(-1);
            jg.writeEndArray();
            assertFalse(jg.isClosed());
            jg.close();
            assertTrue(jg.isClosed());
            jg.close();
            assertTrue(jg.isClosed());
        }
    }

    /*
    ///////////////////////////////////////////////
    // Tests for data binding integration
    ///////////////////////////////////////////////
     */

    public void testPojoWriting()
        throws IOException
    {
        JsonFactory jf = new MappingJsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createJsonGenerator(sw);
        gen.writeObject(new Pojo());
        gen.close();
        // trimming needed if main-level object has leading space
        String act = sw.toString().trim();
        assertEquals("{\"x\":4}", act);
    }

    public void testPojoWritingFailing()
        throws IOException
    {
        // regular factory can't do it, without a call to setCodec()
        JsonFactory jf = new JsonFactory();
        try {
            StringWriter sw = new StringWriter();
            JsonGenerator gen = jf.createJsonGenerator(sw);
            gen.writeObject(new Pojo());
            gen.close();
            fail("Expected an exception: got sw '"+sw.toString()+"'");
        } catch (IllegalStateException e) {
            verifyException(e, "No ObjectCodec defined");
        }
    }

    /*
    ///////////////////////////////////////////////
    // Tests for raw output
    ///////////////////////////////////////////////
     */

    public void testRaw() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createJsonGenerator(sw);
        gen.writeStartArray();
        gen.writeRaw("-123, true");
        gen.writeRaw(", \"x\"  ");
        gen.writeEndArray();
        gen.close();

                
        JsonParser jp = createParserUsingReader(sw.toString());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(-123, jp.getIntValue());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("x", jp.getText());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }

    public void testRawValue() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createJsonGenerator(sw);
        gen.writeStartArray();
        gen.writeRawValue("7");
        gen.writeRawValue("[ null ]");
        gen.writeRawValue("false");
        gen.writeEndArray();
        gen.close();

        JsonParser jp = createParserUsingReader(sw.toString());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(7, jp.getIntValue());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_FALSE, jp.nextToken());

        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }

    /*
    ///////////////////////////////////////////////
    // Tests for binary data
    ///////////////////////////////////////////////
     */

    /**
     * This is really inadequate test, all in all, but should serve
     * as some kind of sanity check. Reader-side should more thoroughly
     * test things, as it does need writers to construct the data first.
     */
    public void testBinaryWrite() throws Exception
    {
        _testBinaryWrite(false);
        _testBinaryWrite(true);
    }

    private void _testBinaryWrite(boolean useCharBased) throws Exception
    {
        /* The usual sample input string, from Thomas Hobbes's "Leviathan"
         * (via Wikipedia)
         */
        final String INPUT = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
        final byte[] INPUT_BYTES = INPUT.getBytes("US-ASCII");
        // as per MIME variant, result minus lfs =
        final String OUTPUT =
 "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz"
+"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg"
+"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu"
+"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo"
+"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
            ;

        /* Let's only test the standard base64 variant; but write
         * values in root, array and object contexts.
         */
        Base64Variant b64v = Base64Variants.getDefaultVariant();
        JsonFactory jf = new JsonFactory();

        for (int i = 0; i < 3; ++i) {
            JsonGenerator gen;
            ByteArrayOutputStream bout = new ByteArrayOutputStream(200);
            if (useCharBased) {
                gen = jf.createJsonGenerator(new OutputStreamWriter(bout, "UTF-8"));
            } else {
                gen = jf.createJsonGenerator(bout, JsonEncoding.UTF8);
            }

            switch (i) {
            case 0: // root
                gen.writeBinary(b64v, INPUT_BYTES, 0, INPUT_BYTES.length);
                break;
            case 1: // array
                gen.writeStartArray();
                gen.writeBinary(b64v, INPUT_BYTES, 0, INPUT_BYTES.length);
                gen.writeEndArray();
                break;
            default: // object
                gen.writeStartObject();
                gen.writeFieldName("field");
                gen.writeBinary(b64v, INPUT_BYTES, 0, INPUT_BYTES.length);
                gen.writeEndObject();
                break;
            }
            gen.close();

            JsonParser jp = jf.createJsonParser(new ByteArrayInputStream(bout.toByteArray()));
            
            // Need to skip other events before binary data:
            switch (i) {
            case 0:
                break;
            case 1:
                assertEquals(JsonToken.START_ARRAY, jp.nextToken());
                break;
            default:
                assertEquals(JsonToken.START_OBJECT, jp.nextToken());
                assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
                break;
            }
            assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
            String actualValue = jp.getText();
            jp.close();
            assertEquals(OUTPUT, actualValue);
        }
    }
}
