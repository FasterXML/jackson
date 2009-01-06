package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeReference;

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

    /*
    ///////////////////////////////////////////////////////
    // Collection tests
    ///////////////////////////////////////////////////////
     */

}
