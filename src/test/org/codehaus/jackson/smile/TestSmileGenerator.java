package org.codehaus.jackson.smile;

import java.io.*;

import static org.codehaus.jackson.smile.SmileConstants.*;

public class TestSmileGenerator
    extends SmileTestBase
{
    /**
     * Test for verifying handling of 'true', 'false' and 'null' literals
     */
    public void testSimpleLiterals() throws Exception
    {
        // false, no header (or frame marker)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeBoolean(true);
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_TRUE);

        // false, no header or frame marker
        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeBoolean(false);
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_FALSE);

        // null, no header or frame marker
        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNull();
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_NULL);

        // And then with some other combinations:
        // true, but with header
        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, true);
        gen.writeBoolean(true);
        gen.close();
        
        // note: version, and 'check shared names', but not 'check shared strings' or 'raw binary'
        int b4 = HEADER_BYTE_4 | SmileConstants.HEADER_BIT_HAS_SHARED_NAMES;

    	_verifyBytes(out.toByteArray(),
                HEADER_BYTE_1, HEADER_BYTE_2, HEADER_BYTE_3, (byte) b4,
                SmileConstants.TOKEN_LITERAL_TRUE);

        // null, with header and end marker
        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, true);
        gen.enable(SmileGenerator.Feature.WRITE_END_MARKER);
        gen.writeNull();
        gen.close();
        _verifyBytes(out.toByteArray(),
                HEADER_BYTE_1, HEADER_BYTE_2, HEADER_BYTE_3, (byte) b4,
                TOKEN_LITERAL_NULL, BYTE_MARKER_END_OF_CONTENT);
    }

    public void testSimpleArray() throws Exception
    {
    	// First: empty array (2 bytes)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeStartArray();
        gen.writeEndArray();
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_START_ARRAY,
        		SmileConstants.TOKEN_LITERAL_END_ARRAY);

        // then simple array with 3 literals
        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeStartArray();
        gen.writeBoolean(true);
        gen.writeNull();
        gen.writeBoolean(false);
        gen.writeEndArray();
        gen.close();
        assertEquals(5, out.toByteArray().length);

        // and then array containing another array and short String
        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeStartArray();
        gen.writeStartArray();
        gen.writeEndArray();
        gen.writeString("12");
        gen.writeEndArray();
        gen.close();
        // 4 bytes for start/end arrays; 3 bytes for short ascii string
        assertEquals(7, out.toByteArray().length);
    }

    public void testShortAscii() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeString("abc");
        gen.close();
        _verifyBytes(out.toByteArray(), (byte)0x42, (byte) 'a', (byte) 'b', (byte) 'c');
    }


    public void testTrivialObject() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeStartObject();
        gen.writeNumberField("a", 6);
        gen.writeEndObject();
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_START_OBJECT,
        		(byte) 0x80, (byte) 'a', (byte) (0xC0 + SmileUtil.zigzagEncode(6)),
        		SmileConstants.TOKEN_LITERAL_END_OBJECT);
    }

    public void test2FieldObject() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeStartObject();
        gen.writeNumberField("a", 1);
        gen.writeNumberField("b", 2);
        gen.writeEndObject();
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_START_OBJECT,
        		(byte) 0x80, (byte) 'a', (byte) (0xC0 + SmileUtil.zigzagEncode(1)),
        		(byte) 0x80, (byte) 'b', (byte) (0xC0 + SmileUtil.zigzagEncode(2)),
        		SmileConstants.TOKEN_LITERAL_END_OBJECT);
    }

    public void testAnotherObject() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeStartObject();
        gen.writeNumberField("a", 8);
        gen.writeFieldName("b");
        gen.writeStartArray();
        gen.writeBoolean(true);
        gen.writeEndArray();
        gen.writeFieldName("c");
        gen.writeStartObject();
        gen.writeEndObject();

        gen.writeFieldName("d");
        gen.writeStartObject();
        gen.writeFieldName("3");
        gen.writeNull();
        gen.writeEndObject();
        
        gen.writeEndObject();
        gen.close();
        assertEquals(21, out.toByteArray().length);
    }

    /**
     * Test to verify that 
     */
    public void testSharedStrings() throws Exception
    {
        // first, no sharing, 2 separate Strings
        final String VALUE = "abcde12345";
        byte[] data = writeRepeatedString(false, VALUE);
        int BASE_LEN = 28;
        assertEquals(BASE_LEN, data.length);
        data = writeRepeatedString(true, VALUE);
        if (data.length >= BASE_LEN) { // should be less
            fail("Expected shared String length to be < "+BASE_LEN+", was "+data.length);
        }
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private byte[] writeRepeatedString(boolean shared, String value) throws Exception
    {
        SmileFactory f = new SmileFactory();
        // need header to enable shared string values
        f.configure(SmileGenerator.Feature.WRITE_HEADER, true);
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, shared);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = f.createJsonGenerator(out);
        gen.writeStartArray();
        gen.writeString(value);
        gen.writeString(value);
        gen.writeEndArray();  
        gen.close();
        return out.toByteArray();
    }
}
