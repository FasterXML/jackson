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
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

}
