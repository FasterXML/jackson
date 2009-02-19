package org.codehaus.jackson.map;

import main.BaseTest;

import java.util.*;

import org.codehaus.jackson.type.TypeReference;

/**
 * Unit tests for verifying handling of simple structured
 * types; Maps, Lists, arrays.
 */
public class TestObjectMapperContainerDeserializer
    extends BaseTest
{
    /*
    ///////////////////////////////////////////////////////
    // Helper classes/enums
    ///////////////////////////////////////////////////////
     */

    enum Key {
        KEY1, KEY2, WHATEVER;
    }

    /*
    ///////////////////////////////////////////////////////
    // Map tests
    ///////////////////////////////////////////////////////
     */

	public void testUntypedMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get "untyped" default map-to-map, pass Object.class
        String JSON = "{ \"foo\" : \"bar\", \"crazy\" : true, \"null\" : null }";

        // Not a guaranteed cast theoretically, but will work:
        @SuppressWarnings("unchecked")
        Map<String,Object> result = (Map<String,Object>)mapper.readValue(JSON, Object.class);
        assertNotNull(result);
        assertTrue(result instanceof Map);

        assertEquals(3, result.size());

        assertEquals("bar", result.get("foo"));
        assertEquals(Boolean.TRUE, result.get("crazy"));
        assertNull(result.get("null"));

        // Plus, non existing:
        assertNull(result.get("bar"));
        assertNull(result.get(3));
    }

    public void testExactStringIntMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get typing, must use type reference
        String JSON = "{ \"foo\" : 13, \"bar\" : -39, \n \"\" : 0 }";
        Map<String,Integer> result = mapper.readValue
            (JSON, new TypeReference<HashMap<String,Integer>>() { });

        assertNotNull(result);
        assertEquals(HashMap.class, result.getClass());
        assertEquals(3, result.size());

        assertEquals(Integer.valueOf(13), result.get("foo"));
        assertEquals(Integer.valueOf(-39), result.get("bar"));
        assertEquals(Integer.valueOf(0), result.get(""));
        assertNull(result.get("foobar"));
        assertNull(result.get(" "));
    }

    /**
     * Let's also check that it is possible to do type conversions
     * to allow use of non-String Map keys.
     */
    public void testIntBooleanMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get typing, must use type reference
        String JSON = "{ \"1\" : true, \"-1\" : false }";
        Map<String,Integer> result = mapper.readValue
            (JSON, new TypeReference<HashMap<Integer,Boolean>>() { });

        assertNotNull(result);
        assertEquals(HashMap.class, result.getClass());
        assertEquals(2, result.size());

        assertEquals(Boolean.TRUE, result.get(Integer.valueOf(1)));
        assertEquals(Boolean.FALSE, result.get(Integer.valueOf(-1)));
        assertNull(result.get("foobar"));
        assertNull(result.get(0));
    }

    public void testExactStringStringMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get typing, must use type reference
        String JSON = "{ \"a\" : \"b\" }";
        Map<String,Integer> result = mapper.readValue
            (JSON, new TypeReference<TreeMap<String,String>>() { });

        assertNotNull(result);
        assertEquals(TreeMap.class, result.getClass());
        assertEquals(1, result.size());

        assertEquals("b", result.get("a"));
        assertNull(result.get("b"));
    }

    /**
     * Unit test that verifies that it's ok to have incomplete
     * information about Map class itself, as long as it's something
     * we good guess about: for example, <code>Map.Class</code> will
     * be replaced by something like <code>HashMap.class</code>,
     * if given.
     */
    public void testGenericStringIntMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get typing, must use type reference; but with abstract type
        String JSON = "{ \"a\" : 1, \"b\" : 2, \"c\" : -99 }";
        Map<String,Integer> result = mapper.readValue
            (JSON, new TypeReference<Map<String,Integer>>() { });
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(3, result.size());

        assertEquals(Integer.valueOf(-99), result.get("c"));
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(1), result.get("a"));

        assertNull(result.get(""));
    }

    public void testEnumMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String JSON = "{ \"KEY1\" : \"\", \"WHATEVER\" : null }";

        // to get typing, must use type reference
        EnumMap<Key,String> result = mapper.readValue
            (JSON, new TypeReference<EnumMap<Key,String>>() { });

        assertNotNull(result);
        assertEquals(EnumMap.class, result.getClass());
        assertEquals(2, result.size());

        assertEquals("", result.get(Key.KEY1));
        // null should be ok too...
        assertTrue(result.containsKey(Key.WHATEVER));
        assertNull(result.get(Key.WHATEVER));

        // plus we have nothing for this key
        assertFalse(result.containsKey(Key.KEY2));
        assertNull(result.get(Key.KEY2));
    }

    public void testMapWithEnums() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String JSON = "{ \"KEY2\" : \"WHATEVER\" }";

        // to get typing, must use type reference
        Map<Enum<?>,Enum<?>> result = mapper.readValue
            (JSON, new TypeReference<Map<Key,Key>>() { });

        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(1, result.size());

        assertEquals(Key.WHATEVER, result.get(Key.KEY2));
        assertNull(result.get(Key.WHATEVER));
        assertNull(result.get(Key.KEY1));
    }

    public void testMapError() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Object result = mapper.readValue("[ 1, 2 ]", 
                                             new TypeReference<Map<String,String>>() { });
            fail("Expected an exception, but got result value: "+result);
        } catch (JsonMappingException jex) {
            verifyException(jex, "START_ARRAY");
        }
    }

    /*
    ///////////////////////////////////////////////////////
    // Collection tests
    ///////////////////////////////////////////////////////
     */

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
