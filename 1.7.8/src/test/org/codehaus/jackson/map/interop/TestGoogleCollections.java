package org.codehaus.jackson.map.interop;

import java.util.*;

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
        @JsonValue
        @SuppressWarnings({"unchecked"})
        public ImmutableMap toMap()
        {
            return new ImmutableMap.Builder().put("a", 1).build();
        }
    }
    
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
        assertEquals("{}", new ObjectMapper().writeValueAsString(new MapBean()));
    }
}
