package org.codehaus.jackson.smile;

import java.io.ByteArrayOutputStream;

public class TestSmileGeneratorNumbers
    extends SmileTestBase
{
    public void testSmallInts() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeNumber(3);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(3)));

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(0);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(0)));

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(-6);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(-6)));

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(15);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(15)));

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(-16);
        gen.close();
        _verifyBytes(out.toByteArray(), (byte) (0xC0 + SmileUtil.zigzagEncode(-16)));
    }

    public void testOtherInts() throws Exception
    {
    	// beyond tiny ints, 6-bit values take 2 bytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeNumber(16);
        gen.close();
        assertEquals(2, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(-17);
        gen.close();
        assertEquals(2, out.toByteArray().length);

        // and up to 13-bit values take 3 bytes
        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(0xFFF);
        gen.close();
        assertEquals(3, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(-4096);
        gen.close();
        assertEquals(3, out.toByteArray().length);
        
        // up to 20, 4 bytes... and so forth
        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(0x1000);
        gen.close();
        assertEquals(4, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(500000);
        gen.close();
        assertEquals(4, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(Integer.MAX_VALUE);
        gen.close();
        assertEquals(6, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(Integer.MIN_VALUE);
        gen.close();
        assertEquals(6, out.toByteArray().length);
        
        // up to longest ones, taking 11 bytes
        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(Long.MAX_VALUE);
        gen.close();
        assertEquals(11, out.toByteArray().length);

        out = new ByteArrayOutputStream();
        gen = smileGenerator(out, false);
        gen.writeNumber(Long.MIN_VALUE);
        gen.close();
        assertEquals(11, out.toByteArray().length);
    }

    public void testFloats() throws Exception
    {
        // float length is fixed, 6 bytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeNumber(0.125f);
        gen.close();
        assertEquals(6, out.toByteArray().length);
    }    

    public void testDoubles() throws Exception
    {
        // double length is fixed, 11 bytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeNumber(0.125);
        gen.close();
        assertEquals(11, out.toByteArray().length);
    }    

}
