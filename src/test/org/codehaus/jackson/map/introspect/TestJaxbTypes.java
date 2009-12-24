package org.codehaus.jackson.map.introspect;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Tests for handling of type-related JAXB annotations 
 */
public class TestJaxbTypes
    extends org.codehaus.jackson.map.BaseMapTest
{
    /*
    **************************************************************
    * Helper beans
    **************************************************************
    */

    static class AbstractWrapper {
        @XmlElement(type=AbstractBeanImpl.class)
        public AbstractBean wrapped;
    }

    abstract static class AbstractBean
    {
        public abstract void setA(int a);
        public abstract void setB(String b);
    }

    static class AbstractBeanImpl
        extends AbstractBean
    {
        int a;
        String b;

        public void setA(int a) { this.a = a; }
        public void setB(String b) { this.b = b; }
    }
    
    /*
    **************************************************************
    * Unit tests
    **************************************************************
    */

    public void testXmlElementTypeDeser() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();
        AbstractWrapper wrapper = mapper.readValue("{\"wrapped\":{\"a\":13,\"b\":\"...\"}}", AbstractWrapper.class);
        assertNotNull(wrapper);
        AbstractBeanImpl bean = (AbstractBeanImpl) wrapper.wrapped;
        assertEquals(13, bean.a);
        assertEquals("...", bean.b);
    }

    /*
    **************************************************************
    * Helper methods
    **************************************************************
    */

    public ObjectMapper getJaxbMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector intr = new JaxbAnnotationIntrospector();
        mapper.getDeserializationConfig().setAnnotationIntrospector(intr);
        mapper.getSerializationConfig().setAnnotationIntrospector(intr);
        return mapper;
    }
}
