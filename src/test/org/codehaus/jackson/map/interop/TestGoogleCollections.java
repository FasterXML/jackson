package org.codehaus.jackson.map.interop;

import org.codehaus.jackson.map.*;

import com.google.common.collect.*;

/**
 * NOTE: this is bogus test currently (as of Jackson 1.6) -- not support
 * has been added!
 * 
 * @author tatu
 */
public class TestGoogleCollections
    extends org.codehaus.jackson.map.BaseMapTest
{
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
}
