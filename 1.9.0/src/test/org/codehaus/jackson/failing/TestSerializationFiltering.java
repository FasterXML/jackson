package org.codehaus.jackson.failing;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonFilter;
import org.codehaus.jackson.map.ser.FilterProvider;
import org.codehaus.jackson.map.ser.impl.SimpleBeanPropertyFilter;
import org.codehaus.jackson.map.ser.impl.SimpleFilterProvider;

/**
 * Currently (1.8) generic filtering does not work for "any getter": it should,
 * ideally, so here's the test.
 */
public class TestSerializationFiltering extends BaseMapTest
{
    @JsonFilter("anyFilter")
    public static class AnyBean
    {
        private Map<String, String> properties = new HashMap<String, String>();
        {
          properties.put("a", "1");
          properties.put("b", "2");
        }

        @JsonAnyGetter
        public Map<String, String> anyProperties()
        {
          return properties;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    // should also work for @JsonAnyGetter, as per [JACKSON-516]
    public void testAnyGetterFiltering() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"a\":\"1\"}", mapper.writer(prov).writeValueAsString(new AnyBean()));
    }
}
