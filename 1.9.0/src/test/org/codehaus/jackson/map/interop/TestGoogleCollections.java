package org.codehaus.jackson.map.interop;

import com.google.common.collect.*;

import org.codehaus.jackson.annotate.JsonValue;
import org.codehaus.jackson.map.*;

/**
 * NOTE: this is bogus test currently (as of Jackson 1.6) -- not support
 * has been added!
 * 
 * @author tatu
 */
public class TestGoogleCollections
    extends org.codehaus.jackson.map.BaseMapTest
{
    static class MapBean
    {
        // both class and method left non-public to verify that access is overridden
        @JsonValue
        protected ImmutableMap<String,Integer> toMap()
        {
            return new ImmutableMap.Builder<String,Integer>().put("a", 1).build();
        }
    }

    // !!! NOTE: does not test that it produces anything useful...
    public void testTrivialMultiMapSerialize() throws Exception
    {
        Multimap<String,String> map = HashMultimap.create();
        map.put("a", "1");
        String json = new ObjectMapper().writeValueAsString(map);
        assertNotNull(json);

        /*
        assertEquals("{\"a\":[\"1\"]}", json);
        */
    }

    public void testMapWithJsonValue() throws Exception
    {
        assertEquals("{\"a\":1}", new ObjectMapper().writeValueAsString(new MapBean()));
    }

    /*// fails similarly, for same reason
    static class StdMapBean
    {
        @JsonValue
        public Map<String,String> toMap()
        {
            HashMap<String,String> map = new HashMap<String,String>();
            map.put("a", "1");
            return map;
        }
    }
    
    public void testMapWithJsonValue2() throws Exception
    {
        assertEquals("{\"a\":1}", new ObjectMapper().writeValueAsString(new StdMapBean()));
    }
    */
}
