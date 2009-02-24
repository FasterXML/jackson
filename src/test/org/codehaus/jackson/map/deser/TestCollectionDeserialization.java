package org.codehaus.jackson.map;

import org.codehaus.jackson.map.BaseMapTest;

import java.util.*;

import org.codehaus.jackson.type.TypeReference;

public class TestCollectionDeserialization
    extends BaseMapTest
{
    enum Key {
        KEY1, KEY2, WHATEVER;
    }

    public void testUntypedList() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get "untyped" default List, pass Object.class
        String JSON = "[ \"text!\", true, null, 23 ]";

        /* Not a guaranteed cast theoretically, but will work:
         * (since we know that Jackson will construct an ArrayList here...)
         */
        Object value = mapper.readValue(JSON, Object.class);
        assertNotNull(value);
        assertTrue(value instanceof ArrayList);
        List<?> result = (List<?>) value;

        assertEquals(4, result.size());

        assertEquals("text!", result.get(0));
        assertEquals(Boolean.TRUE, result.get(1));
        assertNull(result.get(2));
        assertEquals(Integer.valueOf(23), result.get(3));
    }

    public void testExactStringCollection() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get typing, must use type reference
        String JSON = "[ \"a\", \"b\" ]";
        List<String> result = mapper.readValue(JSON, new TypeReference<ArrayList<String>>() { });

        assertNotNull(result);
        assertEquals(ArrayList.class, result.getClass());
        assertEquals(2, result.size());

        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    public void testHashSet() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String JSON = "[ \"KEY1\", \"KEY2\" ]";

        EnumSet<Key> result = mapper.readValue(JSON, new TypeReference<EnumSet<Key>>() { });
        assertNotNull(result);
        assertTrue(EnumSet.class.isAssignableFrom(result.getClass()));
        assertEquals(2, result.size());

        assertTrue(result.contains(Key.KEY1));
        assertTrue(result.contains(Key.KEY2));
        assertFalse(result.contains(Key.WHATEVER));
    }
}
