package org.codehaus.jackson.xml;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Although XML-backed data binding does not rely (or directly build) on JAXB
 * annotations, it should be possible to use them similar to how they are used
 * with default Jackson JSON data binding. Let's verify this is the case.
 */
public class TestWithJAXBAnnotations extends XmlTestBase
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    @XmlRootElement(name="bean")
    public static class SimpleBean
    {
        public String value = "text";
        
        @XmlAttribute
        public String attr = "3";
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimpleSerialize() throws Exception
    {
        XmlMapper mapper = getJaxbAndJacksonMapper();
        String xml = mapper.writeValueAsString(new SimpleBean());
        assertEquals("<bean attr=\"3\"><value>text</value></bean>", xml);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected XmlMapper getJaxbAndJacksonMapper()
    {
        XmlMapper mapper = new XmlMapper();
        AnnotationIntrospector intr = new AnnotationIntrospector.Pair(new JaxbAnnotationIntrospector(),
                        new JacksonAnnotationIntrospector());
        mapper.getDeserializationConfig().setAnnotationIntrospector(intr);
        mapper.getSerializationConfig().setAnnotationIntrospector(intr);
        return mapper;
    }
}
