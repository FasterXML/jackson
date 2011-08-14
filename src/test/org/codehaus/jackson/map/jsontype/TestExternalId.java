package org.codehaus.jackson.map.jsontype;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

// Tests for [JACKSON-453]
public class TestExternalId extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */
    
    static class ExternalBean
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public Object bean;

        public ExternalBean() { }
        public ExternalBean(int v) {
            bean = new ValueBean(v);
        }
    }

    @JsonTypeName("vbean")
    static class ValueBean {
        public int value;
        
        public ValueBean() { }
        public ValueBean(int v) { value = v; }
    }

    @JsonTypeName("funk")
    @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
    static class FunkyExternalBean {
        public int i = 3;
    }
    
    /*
    /**********************************************************
    /* Unit tests, serialization
    /**********************************************************
     */

    public void testSimpleSerialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        /* This may look odd, but one implementation nastiness is the fact
         * that we can not properly serialize type id before the object,
         * because call is made after property name (for object) has already
         * been written out. So we'll write it after...
         * Deserializer will work either way as it can not rely on ordering
         * anyway.
         */
        assertEquals("{\"bean\":{\"value\":11},\"extType\":\"vbean\"}",
                mapper.writeValueAsString(new ExternalBean(11)));
    }

    // If trying to use with Class, should just become "PROPERTY" instead:
    public void testImproperExternalIdSerialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("{\"extType\":\"funk\",\"i\":3}",
                mapper.writeValueAsString(new FunkyExternalBean()));
    }

    /*
    /**********************************************************
    /* Unit tests, deserialization
    /**********************************************************
     */
    
    public void testSimpleDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        ExternalBean result = mapper.readValue("{\"bean\":{\"value\":11},\"extType\":\"vbean\"}", ExternalBean.class);
        assertNotNull(result);
        ValueBean vb = (ValueBean) result.bean;
        assertEquals(11, vb.value);
    }

    // If trying to use with Class, should just become "PROPERTY" instead:
    public void testImproperExternalIdDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FunkyExternalBean result = mapper.readValue("{\"extType\":\"funk\",\"i\":3}", FunkyExternalBean.class);
        assertNotNull(result);
        assertEquals(3, result.i);
    }
}
