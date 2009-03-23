package org.codehaus.jackson.map.deser;

import main.BaseTest;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that {@link JsonAnySetter} annotation
 * works as expected.
 */
public class TestAnyProperties
    extends BaseTest
{
    /*
    //////////////////////////////////////////////
    // Annotated helper classes
    //////////////////////////////////////////////
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

    /*
    //////////////////////////////////////////////
    // Test methods
    //////////////////////////////////////////////
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

    /*
    //////////////////////////////////////////////
    // Private methods
    //////////////////////////////////////////////
     */
}
