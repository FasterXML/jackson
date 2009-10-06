package org.codehaus.jackson.util;

import org.codehaus.jackson.io.NumberOutput;

import java.util.Random;

public class TestTextBuffer
    extends main.BaseTest
{
    /**
     * Trivially simple basic test to ensure all basic append
     * methods work
     */
    public void testSimple()
    {
        TextBuffer tb = new TextBuffer(new BufferRecycler());
        tb.append('a');
        tb.append(new char[] { 'X', 'b' }, 1, 1);
        tb.append("c", 0, 1);
        assertEquals("abc", tb.toString());
        assertEquals(3, tb.contentsAsArray().length);

        assertNotNull(tb.expandCurrentSegment());
    }
}
