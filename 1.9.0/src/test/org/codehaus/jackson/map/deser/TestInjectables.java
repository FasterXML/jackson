package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JacksonInject;

// @since 1.9
public class TestInjectables extends BaseMapTest
{
    static class InjectedBean
    {
        @JacksonInject
        protected String stuff;

        @JacksonInject("myId")
        protected String otherStuff;

        protected long third;
        
        public int value;

        @JacksonInject
        public void injectThird(long v) {
            third = v;
        }
    }    

    static class BadBean1 {
        @JacksonInject protected String prop1;
        @JacksonInject protected String prop2;
    }

    static class BadBean2 {
        @JacksonInject("x") protected String prop1;
        @JacksonInject("x") protected String prop2;
    }

    static class CtorBean {
        protected String name;
        protected int age;
        
        public CtorBean(@JacksonInject String n, @JsonProperty("age") int a)
        {
            name = n;
            age = a;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "stuffValue")
            .addValue("myId", "xyz")
            .addValue(Long.TYPE, Long.valueOf(37))
            );
        InjectedBean bean = mapper.readValue("{\"value\":3}", InjectedBean.class);
        assertEquals(3, bean.value);
        assertEquals("stuffValue", bean.stuff);
        assertEquals("xyz", bean.otherStuff);
        assertEquals(37L, bean.third);
    }

    public void testWithCtors() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "Bubba")
            );
        CtorBean bean = mapper.readValue("{\"age\":55}", CtorBean.class);
        assertEquals(55, bean.age);
        assertEquals("Bubba", bean.name);
    }
    
    public void testInvalidDup() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue("{}", BadBean1.class);
        } catch (Exception e) {
            verifyException(e, "Duplicate injectable value");
        }
        try {
            mapper.readValue("{}", BadBean2.class);
        } catch (Exception e) {
            verifyException(e, "Duplicate injectable value");
        }
    }
}
