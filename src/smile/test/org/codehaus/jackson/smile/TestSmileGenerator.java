package org.codehaus.jackson.smile;

import java.io.*;

import org.junit.Assert;

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
        SmileGenerator gen = _generator(out, false);
        gen.writeBoolean(true);
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_TRUE);

        // false, no header or frame marker
        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeBoolean(false);
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_FALSE);

        // null, no header or frame marker
        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNull();
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_NULL);

        // And then with some other combinations:
        // true, but with header
        out = new ByteArrayOutputStream();
        gen = _generator(out, true);
        gen.writeBoolean(true);
        gen.close();
        _verifyBytes(out.toByteArray(),
                HEADER_BYTE_1, HEADER_BYTE_2, HEADER_BYTE_3, HEADER_BYTE_4,
                SmileConstants.TOKEN_LITERAL_TRUE);

        // null, with header and end marker
        out = new ByteArrayOutputStream();
        gen = _generator(out, true);
        gen.enable(SmileGenerator.Feature.WRITE_END_MARKER);
        gen.writeNull();
        gen.close();
        _verifyBytes(out.toByteArray(),
                HEADER_BYTE_1, HEADER_BYTE_2, HEADER_BYTE_3, HEADER_BYTE_4,
                TOKEN_LITERAL_NULL, BYTE_MARKER_END_OF_CONTENT);
    }

    public void testSimpleArray() throws Exception
    {
    	// First: empty array (2 bytes)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = _generator(out, false);
        gen.writeStartArray();
        gen.writeEndArray();
        gen.close();
        _verifyBytes(out.toByteArray(), SmileConstants.TOKEN_LITERAL_START_ARRAY,
        		SmileConstants.TOKEN_LITERAL_END_ARRAY);

        // then simple array with 3 literals
        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeStartArray();
        gen.writeBoolean(true);
        gen.writeNull();
        gen.writeBoolean(false);
        gen.writeEndArray();
        gen.close();
        assertEquals(5, out.toByteArray().length);

        // and then array containing another array and short String
        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
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
        SmileGenerator gen = _generator(out, false);
        gen.writeString("abc");
        gen.close();
        _verifyBytes(out.toByteArray(), (byte)0x42, (byte) 'a', (byte) 'b', (byte) 'c');
    }

    public void testSmallInts() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = _generator(out, false);
        gen.writeNumber(3);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(3)));

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(0);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(0)));

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(-6);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(-6)));

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(15);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(15)));

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(-16);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(-16)));
    }

    public void testOtherInts() throws Exception
    {
    	// beyond tiny ints, 6-bit values take 2 bytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = _generator(out, false);
        gen.writeNumber(16);
        gen.close();
        assertEquals(2, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(-17);
        gen.close();
        assertEquals(2, out.toByteArray().length);

        // and up to 13-bit values take 3 bytes
        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(0xFFF);
        gen.close();
        assertEquals(3, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(-4096);
        gen.close();
        assertEquals(3, out.toByteArray().length);
        
        // up to 20, 4 bytes... and so forth
        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(0x1000);
        gen.close();
        assertEquals(4, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(500000);
        gen.close();
        assertEquals(4, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(Integer.MAX_VALUE);
        gen.close();
        assertEquals(6, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(Integer.MIN_VALUE);
        gen.close();
        assertEquals(6, out.toByteArray().length);
        
        // up to longest ones, taking 11 bytes
        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(Long.MAX_VALUE);
        gen.close();
        assertEquals(11, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = _generator(out, false);
        gen.writeNumber(Long.MIN_VALUE);
        gen.close();
        assertEquals(11, out.toByteArray().length);
    }

    public void testTrivialObject() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = _generator(out, false);
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
        SmileGenerator gen = _generator(out, false);
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

    /*
    public void testAnotherObject() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = _generator(out, false);
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
        assertEquals(10, out.toByteArray().length);
    }
    */

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

}
