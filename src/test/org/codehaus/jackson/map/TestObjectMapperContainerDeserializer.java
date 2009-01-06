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

        // !!! TBI
    }

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

        // !!! TBI
    }
}
