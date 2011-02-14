package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonFilter;
import org.codehaus.jackson.map.ser.impl.*;

/**
 * Tests for verifying that bean property filtering (new with 1.7)
 * works as expected.
 * 
 * @since 1.7
 */
public class TestFiltering extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    @JsonFilter("RootFilter")
    static class Bean {
        public String a = "a";
        public String b = "b";
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimpleInclusionFilter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("a"));
        assertEquals("{\"a\":\"a\"}", mapper.filteredWriter(prov).writeValueAsString(new Bean()));
    }

    public void testSimpleExclusionFilter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("a"));
        assertEquals("{\"b\":\"b\"}", mapper.filteredWriter(prov).writeValueAsString(new Bean()));
    }

    // should handle missing case gracefully
    public void testMissingFilter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValueAsString(new Bean());
            fail("Should have failed without configured filter");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not resolve BeanPropertyFilter with id 'RootFilter'");
        }
    }
    
    // defaulting, as per [JACKSON-449]
    public void testDefaultFilter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().setDefaultFilter(SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"b\"}", mapper.filteredWriter(prov).writeValueAsString(new Bean()));
    }
}
