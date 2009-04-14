package main;

import org.codehaus.jackson.io.NumberInput;

/**
 * Set of basic unit tests for verifying that the low-level number
 * handling methods work as expected.
 */
public class TestNumberParsing
    extends BaseTest
{
    public void testIntParsing()
        throws Exception
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

    public void testLongParsing()
        throws Exception
    {
        char[] testChars = "123456789012345678".toCharArray();

        assertEquals(123456789012345678L, NumberInput.parseLong(testChars, 0, 18));
    }
}
