package org.codehaus.jackson.map.introspect;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Tests to verify that annotations are shared and merged between members
 * of a property (getter and setter and so on)
 * 
 * @since 1.9
 */
public class TestAnnotationMerging extends BaseMapTest
{
    static class Wrapper
    {
        protected Object value;

        public Wrapper() { }
        public Wrapper(Object o) { value = o; }
        
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object getValue() { return value; }

        public void setValue(Object o) { value = o; }
    }

    static class SharedName {
        @JsonProperty("x")
        protected int value;

        public SharedName(int v) { value = v; }
        
        public int getValue() { return value; }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSharedNames() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("{\"x\":6}", mapper.writeValueAsString(new SharedName(6)));
    }
    
    public void testSharedTypeInfo() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new Wrapper(13L));
        Wrapper result = mapper.readValue(json, Wrapper.class);
        assertEquals(Long.class, result.value.getClass());
    }
}
