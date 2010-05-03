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

        // then ranges
    }

    public void testZigZagLong()
    {
        // !!! TODO
    }
}
