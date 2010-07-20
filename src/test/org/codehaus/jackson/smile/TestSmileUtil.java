package org.codehaus.jackson.smile;

public class TestSmileUtil
    extends main.BaseTest
{
    /**
     * Verification of helper methods used to handle with zigzag encoding
     */
    public void testZigZagInt()
    {
        // simple encode
        assertEquals(0, SmileUtil.zigzagEncode(0));
        assertEquals(1, SmileUtil.zigzagEncode(-1));
        assertEquals(2, SmileUtil.zigzagEncode(1));
        assertEquals(0xFFFFFFFF, SmileUtil.zigzagEncode(Integer.MIN_VALUE));
        assertEquals(0xFFFFFFFE, SmileUtil.zigzagEncode(Integer.MAX_VALUE));

        // simple decode
        assertEquals(0, SmileUtil.zigzagDecode(0));
        assertEquals(-1, SmileUtil.zigzagDecode(1));
        assertEquals(1, SmileUtil.zigzagDecode(2));
        assertEquals(0x7fffFFFF, SmileUtil.zigzagDecode(0xFFFFFFFE));
        assertEquals(Integer.MIN_VALUE, SmileUtil.zigzagDecode(0xFFFFFFFF));

        // round-trip
        assertEquals(Integer.MIN_VALUE, SmileUtil.zigzagDecode(SmileUtil.zigzagEncode(Integer.MIN_VALUE)));
        assertEquals(Integer.MAX_VALUE, SmileUtil.zigzagDecode(SmileUtil.zigzagEncode(Integer.MAX_VALUE)));
    }

    public void testZigZagLong()
    {
        assertEquals(0L, SmileUtil.zigzagEncode(0L));
        assertEquals(-1L, SmileUtil.zigzagEncode(Long.MIN_VALUE));
        assertEquals(-2L, SmileUtil.zigzagEncode(Long.MAX_VALUE));

        assertEquals(Long.MAX_VALUE, SmileUtil.zigzagDecode(-2L));
        assertEquals(Long.MIN_VALUE, SmileUtil.zigzagDecode(-1L));

        // round-trip
        assertEquals(Long.MIN_VALUE, SmileUtil.zigzagDecode(SmileUtil.zigzagEncode(Long.MIN_VALUE)));
        assertEquals(Long.MAX_VALUE, SmileUtil.zigzagDecode(SmileUtil.zigzagEncode(Long.MAX_VALUE)));
}
}
