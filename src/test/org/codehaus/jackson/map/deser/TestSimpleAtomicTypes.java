package org.codehaus.jackson.map.deser;

import java.util.concurrent.atomic.*;

import org.codehaus.jackson.map.ObjectMapper;

public class TestSimpleAtomicTypes
    extends org.codehaus.jackson.map.BaseMapTest
{
    public void testAtomicBoolean() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AtomicBoolean b = mapper.readValue("true", AtomicBoolean.class);
        assertTrue(b.get());
    }

    public void testAtomicInt() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AtomicInteger value = mapper.readValue("13", AtomicInteger.class);
        assertEquals(13, value.get());
    }

    public void testAtomicLong() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AtomicLong value = mapper.readValue("12345678901", AtomicLong.class);
        assertEquals(12345678901L, value.get());
    }

    public void testAtomicReference() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<int[]> value = mapper.readValue("[1,2]",
                new org.codehaus.jackson.type.TypeReference<AtomicReference<int[]>>() { });
        int[] ints = value.get();
        assertNotNull(ints);
        assertEquals(2, ints.length);
        assertEquals(1, ints[0]);
        assertEquals(2, ints[1]);
    }
}
