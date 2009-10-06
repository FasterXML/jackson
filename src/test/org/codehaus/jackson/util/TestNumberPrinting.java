package org.codehaus.jackson.util;

import java.util.Random;

import org.codehaus.jackson.io.NumberOutput;

/**
 * Set of basic unit tests for verifying that the low-level number
 * printingg methods work as expected.
 */
public class TestNumberPrinting
    extends main.BaseTest
{
    public void testIntPrinting()
        throws Exception
    {
        assertIntPrint(0);
        assertIntPrint(-3);
        assertIntPrint(1234);
        assertIntPrint(-1234);
        assertIntPrint(56789);
        assertIntPrint(-56789);
        assertIntPrint(999999);
        assertIntPrint(-999999);
        assertIntPrint(1000000);
        assertIntPrint(-1000000);
        assertIntPrint(10000001);
        assertIntPrint(-10000001);
        assertIntPrint(-100000012);
        assertIntPrint(100000012);
        assertIntPrint(1999888777);
        assertIntPrint(-1999888777);
        assertIntPrint(Integer.MAX_VALUE);
        assertIntPrint(Integer.MIN_VALUE);

        Random rnd = new Random(12345L);
        for (int i = 0; i < 251000; ++i) {
            assertIntPrint(rnd.nextInt());
        }
    }

    public void testLongPrinting()
        throws Exception
    {
        // First, let's just cover couple of edge cases
        assertLongPrint(0L, 0);
        assertLongPrint(1L, 0);
        assertLongPrint(-1L, 0);
        assertLongPrint(Long.MAX_VALUE, 0);
        assertLongPrint(Long.MIN_VALUE, 0);
        assertLongPrint(Long.MAX_VALUE-1L, 0);
        assertLongPrint(Long.MIN_VALUE+1L, 0);

        Random rnd = new Random(12345L);
        // Bigger value space, need more iterations for long
        for (int i = 0; i < 678000; ++i) {
            long l = ((long) rnd.nextInt() << 32) | (long) rnd.nextInt();
            assertLongPrint(l, i);
        }
    }

    /*
    ////////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////////
     */

    private void assertIntPrint(int value)
    {
        String exp = ""+value;
        String act = printToString(value);

        if (!exp.equals(act)) {
            assertEquals("Expected conversion (exp '"+exp+"', len "+exp.length()+"; act len "+act.length()+")", exp, act);
        }
    }

    private void assertLongPrint(long value, int index)
    {
        String exp = ""+value;
        String act = printToString(value);

        if (!exp.equals(act)) {
            assertEquals("Expected conversion (exp '"+exp+"', len "+exp.length()+"; act len "+act.length()+"; number index "+index+")", exp, act);
        }
    }

    private String printToString(int value)
    {
        char[] buffer = new char[12];
        int offset = NumberOutput.outputInt(value, buffer, 0);
        return new String(buffer, 0, offset);
    }

    private String printToString(long value)
    {
        char[] buffer = new char[22];
        int offset = NumberOutput.outputLong(value, buffer, 0);
        return new String(buffer, 0, offset);
    }
}

