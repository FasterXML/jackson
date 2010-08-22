package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that {@link JsonAnySetter} annotation
 * works as expected.
 */
public class TestAnyProperties
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    static class MapImitator
    {
        HashMap<String,Object> _map;

        public MapImitator() {
            _map = new HashMap<String,Object>();
        }

        @JsonAnySetter
        void addEntry(String key, Object value)
        {
            _map.put(key, value);
        }
    }

    /**
     * Let's also verify that it is possible to define different
     * value: not often useful, but possible.
     */
    static class MapImitatorWithValue
    {
        HashMap<String,int[]> _map;

        public MapImitatorWithValue() {
            _map = new HashMap<String,int[]>();
        }

        @JsonAnySetter
        void addEntry(String key, int[] value)
        {
            _map.put(key, value);
        }
    }

    // Bad; 2 "any setters"
    static class Broken
    {
        @JsonAnySetter
        void addEntry1(String key, Object value) { }
        @JsonAnySetter
        void addEntry2(String key, Object value) { }
    }

    @JsonIgnoreProperties("dummy")
    static class Ignored
    {
        HashMap<String,Object> map = new HashMap<String,Object>();
 
        @JsonIgnore
        public String bogus;
        
        @JsonAnySetter
        void addEntry(String key, Object value)
        {
            map.put(key, value);
        }        
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testSimpleMapImitation() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        MapImitator mapHolder = m.readValue
            ("{ \"a\" : 3, \"b\" : true }", MapImitator.class);
        Map<String,Object> result = mapHolder._map;
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("a"));
        assertEquals(Boolean.TRUE, result.get("b"));
    }

    public void testSimpleTyped() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        MapImitatorWithValue mapHolder = m.readValue
            ("{ \"a\" : [ 3, -1 ], \"b\" : [ ] }", MapImitatorWithValue.class);
        Map<String,int[]> result = mapHolder._map;
        assertEquals(2, result.size());
        assertEquals(new int[] { 3, -1 }, result.get("a"));
        assertEquals(new int[0], result.get("b"));
    }

    public void testBrokenWithDoubleAnnotations() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        try {
            @SuppressWarnings("unused")
            Broken b = m.readValue("{ \"a\" : 3 }", Broken.class);
            fail("Should have gotten an exception");
        } catch (JsonMappingException e) {
            verifyException(e, "Multiple methods with 'any-setter'");
        }
    }

    // [JACKSON-313]
    public void testIgnored() throws Exception
    {
        Ignored bean = new ObjectMapper().readValue("{\"name\":\"bob\", \"bogus\":\"abc\", \"dummy\" : 13 }",
                Ignored.class);
        assertNull(bean.map.get("dummy"));
        assertNull(bean.map.get("bogus"));
        assertEquals("Bob", bean.map.get("name"));
    }
}
