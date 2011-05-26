package org.codehaus.jackson.jaxb;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class TestRootName  extends BaseJaxbTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    @XmlRootElement(name="rooty")
    static class MyType
    {
        public int value = 37;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testRootName() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();
        mapper.configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
        assertEquals("{\"rooty\":{\"value\":37}}", mapper.writeValueAsString(new MyType()));
    }
}
