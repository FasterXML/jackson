package org.codehaus.jackson.map.introspect;

import java.io.StringWriter;
import java.util.*;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.JsonNode;
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

    // for [JACKSON-348
    static class ShortListHolder {
         @XmlElement(name="id", type=Short.class)
         public List<Short> ids;
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

     // [JACKSON-348]
     public void testShortList() throws Exception
     {
         ShortListHolder holder = getJaxbMapper().readValue("{\"id\":[1,2,3]}",
                 ShortListHolder.class);
         assertNotNull(holder.ids);
         assertEquals(3, holder.ids.size());
         assertSame(Short.valueOf((short)1), holder.ids.get(0));
         assertSame(Short.valueOf((short)2), holder.ids.get(1));
         assertSame(Short.valueOf((short)3), holder.ids.get(2));
     }
}
