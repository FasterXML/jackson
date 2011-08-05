package org.codehaus.jackson.jaxb;

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.map.ObjectMapper;

public class TestJaxbTypeCoercion extends BaseJaxbTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    /**
     * Unit test related to [JACKSON-416]
     */
    static class Jackson416Bean
    {
        @XmlElement(type=Jackson416Base.class)
        public Jackson416Base value = new Jackson416Sub();
    }

    static class Jackson416Base
    {
        public String foo = "foo";
    }

    static class Jackson416Sub extends Jackson416Base
    {
        public String bar = "bar";
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testIssue416() throws Exception
    {
        ObjectMapper mapper = getJaxbAndJacksonMapper();
        Jackson416Bean bean = new Jackson416Bean();
        String json = mapper.writeValueAsString(bean);
        assertEquals("{\"value\":{\"foo\":\"foo\"}}", json);
    }
}
