package org.codehaus.jackson.map.convert;

import java.util.*;

import static org.junit.Assert.*;

import org.codehaus.jackson.map.*;

public class TestStringConversions
    extends org.codehaus.jackson.map.BaseMapTest
{
    final ObjectMapper mapper = new ObjectMapper();

    public void testSimple()
    {
        assertEquals(Boolean.TRUE, mapper.convertValue("true", Boolean.class));
        assertEquals(Integer.valueOf(-3), mapper.convertValue("  -3 ", Integer.class));
        assertEquals(Long.valueOf(77), mapper.convertValue("77", Long.class));

        int[] ints = { 1, 2, 3 };
        List<Integer> Ints = new ArrayList<Integer>();
        Ints.add(1);
        Ints.add(2);
        Ints.add(3);
        
        assertArrayEquals(ints, mapper.convertValue(Ints, int[].class));
    }

    public void testStringsToInts()
    {
        // let's verify our "neat trick" actually works...
        assertArrayEquals(new int[] { 1, 2, 3, 4, -1, 0 },
                          mapper.convertValue("1  2 3    4  -1 0".split("\\s+"), int[].class));
    }
}
