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
        assertEquals("{\"a\":\"a\"}", mapper.writer(prov).writeValueAsString(new Bean()));

        // [JACKSON-504]: also verify it works via mapper
        mapper = new ObjectMapper();
        mapper.setFilters(prov);
        assertEquals("{\"a\":\"a\"}", mapper.writeValueAsString(new Bean()));
    }

    public void testSimpleExclusionFilter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().addFilter("RootFilter",
                SimpleBeanPropertyFilter.serializeAllExcept("a"));
        assertEquals("{\"b\":\"b\"}", mapper.writer(prov).writeValueAsString(new Bean()));
    }

    // should handle missing case gracefully
    public void testMissingFilter() throws Exception
    {
        // First: default behavior should be to throw an exception
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValueAsString(new Bean());
            fail("Should have failed without configured filter");
        } catch (JsonMappingException e) { // should be resolved to a MappingException (internally may be something else)
            verifyException(e, "Can not resolve BeanPropertyFilter with id 'RootFilter'");
        }
        
        // but when changing behavior, should work difference
        SimpleFilterProvider fp = new SimpleFilterProvider().setFailOnUnknownId(false);
        mapper.setFilters(fp);
        String json = mapper.writeValueAsString(new Bean());
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", json);
    }
    
    // defaulting, as per [JACKSON-449]
    public void testDefaultFilter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().setDefaultFilter(SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"b\"}", mapper.writer(prov).writeValueAsString(new Bean()));
    }
}
