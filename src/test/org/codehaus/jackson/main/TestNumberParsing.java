package org.codehaus.jackson.main;

import org.codehaus.jackson.io.NumberInput;

/**
 * Set of basic unit tests for verifying that the low-level number
 * handling methods work as expected.
 */
public class TestNumberParsing
    extends main.BaseTest
{
    public void testIntParsing() throws Exception
    {
        char[] testChars = "123456789".toCharArray();

        assertEquals(3, NumberInput.parseInt(testChars, 2, 1));
        assertEquals(123, NumberInput.parseInt(testChars, 0, 3));
        assertEquals(2345, NumberInput.parseInt(testChars, 1, 4));
        assertEquals(9, NumberInput.parseInt(testChars, 8, 1));
        assertEquals(456789, NumberInput.parseInt(testChars, 3, 6));
        assertEquals(23456, NumberInput.parseInt(testChars, 1, 5));
        assertEquals(123456789, NumberInput.parseInt(testChars, 0, 9));

        testChars = "32".toCharArray();
        assertEquals(32, NumberInput.parseInt(testChars, 0, 2));
        testChars = "189".toCharArray();
        assertEquals(189, NumberInput.parseInt(testChars, 0, 3));

        testChars = "10".toCharArray();
        assertEquals(10, NumberInput.parseInt(testChars, 0, 2));
        assertEquals(0, NumberInput.parseInt(testChars, 1, 1));
    }

    public void testIntParsingWithStrings() throws Exception
    {
        assertEquals(3, NumberInput.parseInt("3"));
        assertEquals(0, NumberInput.parseInt("0"));
        assertEquals(-3, NumberInput.parseInt("-3"));
        assertEquals(27, NumberInput.parseInt("27"));
        assertEquals(-31, NumberInput.parseInt("-31"));
        assertEquals(271, NumberInput.parseInt("271"));
        assertEquals(-131, NumberInput.parseInt("-131"));
        assertEquals(2709, NumberInput.parseInt("2709"));
        assertEquals(-9999, NumberInput.parseInt("-9999"));
        assertEquals(Integer.MIN_VALUE, NumberInput.parseInt(""+Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, NumberInput.parseInt(""+Integer.MAX_VALUE));
    }
    
    public void testLongParsing() throws Exception
    {
        char[] testChars = "123456789012345678".toCharArray();

        assertEquals(123456789012345678L, NumberInput.parseLong(testChars, 0, testChars.length));
    }

    // Unit test for [JACKSON-491]
    public void testLongBoundsChecks() throws Exception
    {
        String minLong = String.valueOf(Long.MIN_VALUE).substring(1);
        String maxLong = String.valueOf(Long.MAX_VALUE);
        final String VALUE_491 = "1323372036854775807"; // is within range (JACKSON-491)
        final String OVERFLOW =  "9999999999999999999"; // and this one is clearly out

        assertTrue(NumberInput.inLongRange(minLong, true));
        assertTrue(NumberInput.inLongRange(maxLong, false));
        assertTrue(NumberInput.inLongRange(VALUE_491, true));
        assertTrue(NumberInput.inLongRange(VALUE_491, false));
        assertFalse(NumberInput.inLongRange(OVERFLOW, false));
        assertFalse(NumberInput.inLongRange(OVERFLOW, true));

        char[] cbuf = minLong.toCharArray();
        assertTrue(NumberInput.inLongRange(cbuf, 0, cbuf.length, true));
        cbuf = maxLong.toCharArray();
        assertTrue(NumberInput.inLongRange(cbuf, 0, cbuf.length, false));
        cbuf = VALUE_491.toCharArray();
        assertTrue(NumberInput.inLongRange(cbuf, 0, cbuf.length, true));
        assertTrue(NumberInput.inLongRange(cbuf, 0, cbuf.length, false));
        cbuf = OVERFLOW.toCharArray();
        assertFalse(NumberInput.inLongRange(cbuf, 0, cbuf.length, true));
        assertFalse(NumberInput.inLongRange(cbuf, 0, cbuf.length, false));
    }
    
}
