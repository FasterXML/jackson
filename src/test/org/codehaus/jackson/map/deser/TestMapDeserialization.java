package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.*;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.type.TypeReference;

public class TestMapDeserialization
    extends BaseMapTest
{
    /*
    ***************************************************
    * Test classes, enums
    ***************************************************
     */

    enum Key {
        KEY1, KEY2, WHATEVER;
    }

    @SuppressWarnings("serial")
    static class BrokenMap
        extends HashMap<Object,Object>
    {
        // No default ctor, nor @JsonCreators
        public BrokenMap(boolean dummy) { super(); }
    }

    /*
    ***************************************************
    * Test methods, untyped (Object valued) maps
    ***************************************************
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

    /**
     * Let's also try another way to express "gimme a Map" deserialization;
     * this time by specifying a Map class, to reduce need to cast
     */
    public void testUntypedMap2() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get "untyped" default map-to-map, pass Object.class
        String JSON = "{ \"a\" : \"x\" }";

        @SuppressWarnings("unchecked")
        HashMap<String,Object> result = /*(HashMap<String,Object>)*/ mapper.readValue(JSON, HashMap.class);
        assertNotNull(result);
        assertTrue(result instanceof Map);

        assertEquals(1, result.size());

        assertEquals("x", result.get("a"));
    }

    /**
     * Unit test for [JACKSON-185]
     */
    public void testUntypedMap3() throws Exception
    {
        String JSON = "{\"a\":[{\"a\":\"b\"},\"value\"]}";
        ObjectMapper m = new ObjectMapper();
        Map<?,?> result = m.readValue(JSON, Map.class);
        assertTrue(result instanceof Map);
        assertEquals(1, result.size());
        Object ob = result.get("a");
        assertNotNull(ob);
        Collection<?> list = (Collection<?>)ob;
        assertEquals(2, list.size());

        JSON = "{ \"var1\":\"val1\", \"var2\":\"val2\", "
            +"\"subvars\": ["
            +" {  \"subvar1\" : \"subvar2\", \"x\" : \"y\" }, "
            +" { \"a\":1 } ]"
            +" }"
            ;
        result = m.readValue(JSON, Map.class);
        assertTrue(result instanceof Map);
        assertEquals(3, result.size());
    }

    private static final String UNTYPED_MAP_JSON =
            "{ \"double\":42.0, \"string\":\"string\","
            +"\"boolean\":true, \"list\":[\"list0\"],"
            +"\"null\":null }";
    
    @SuppressWarnings("serial")
    static class ObjectWrapperMap extends HashMap<String, ObjectWrapper> { }
    
    public void testSpecialMap() throws IOException
    {
       final ObjectMapper mapper = new ObjectMapper();
       final ObjectWrapperMap map = mapper.readValue(UNTYPED_MAP_JSON, ObjectWrapperMap.class);
       _doTestUntyped(map);
    }

    public void testGenericMap() throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, ObjectWrapper> map = mapper.readValue
            (UNTYPED_MAP_JSON,
             new TypeReference<Map<String, ObjectWrapper>>() { });
       _doTestUntyped(map);
    }
    
    private void _doTestUntyped(final Map<String, ObjectWrapper> map)
    {
       assertEquals(Double.valueOf(42), map.get("double").getObject());
       assertEquals("string", map.get("string").getObject());
       assertEquals(Boolean.TRUE, map.get("boolean").getObject());
       assertEquals(Collections.singletonList("list0"), map.get("list").getObject());
       assertTrue(map.containsKey("null"));
       assertNull(map.get("null"));
       assertEquals(5, map.size());
    }

    /*
    ***************************************************
    * Test methods, typed maps
    ***************************************************
     */

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

    /*
    ***************************************************
    * Test methods, maps with enums
    ***************************************************
     */

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

    /*
    ////////////////////////////////////////////////////////////
    // Error tests
    ////////////////////////////////////////////////////////////
     */

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

    public void testNoCtorMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            BrokenMap result = mapper.readValue("{ \"a\" : 3 }", BrokenMap.class);
            // should never get here; assert added to remove compiler warning
            assertNull(result);
        } catch (JsonMappingException e) {
            // instead, should get this exception:
            verifyException(e, "no default/delegating constructor or factory method");
        }
    }
}
