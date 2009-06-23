package org.codehaus.jackson.map.introspect;

import java.util.*;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Simple testing that {@link AnnotationIntrospector.Pair} works as
 * expected, when used with Jackson and JAXB-based introspector.
 *
 * @author Tatu Saloranta
 */
public class TestIntrospectorPair
    extends org.codehaus.jackson.map.BaseMapTest
{
    /*
    /////////////////////////////////////////////////////
    // Helper beans
    /////////////////////////////////////////////////////
     */

    /**
     * Simple test bean for verifying basic field detection and property
     * naming annotation handling
     */
    @SuppressWarnings("unused")
    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    static class NamedBean
    {
        @JsonProperty
            private String jackson = "1";

        @XmlElement(name="jaxb")
            protected String jaxb = "2";

        @JsonProperty("bothJackson")
            @XmlElement(name="bothJaxb")
            private String bothString = "3";


        public String notAGetter() { return "xyz"; }
    }

    /**
     * Another bean for verifying details of property naming
     */
   @SuppressWarnings("unused")
    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    static class NamedBean2
    {
        @JsonProperty("")
        @XmlElement(name="jaxb")
        public String foo = "abc";

        @JsonProperty("jackson")
        @XmlElement()
        public String getBar() { return "123"; }

        // JAXB, alas, requires setters for all properties too
        public void setBar(String v) { }
    }

    /*
    /////////////////////////////////////////////////////
    // Unit tests
    /////////////////////////////////////////////////////
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper;
        AnnotationIntrospector jacksonAI = new JacksonAnnotationIntrospector();
        AnnotationIntrospector jaxbAI = new JaxbAnnotationIntrospector();
        AnnotationIntrospector pair;
        Map<String,Object> result;

        mapper = new ObjectMapper();
        // first: test with Jackson/Jaxb pair (jackson having precedence)
        pair = new AnnotationIntrospector.Pair(jacksonAI, jaxbAI);
        mapper.getSerializationConfig().setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean());
        assertEquals(3, result.size());
        assertEquals("1", result.get("jackson"));
        assertEquals("2", result.get("jaxb"));
        // jackson one should have priority
        assertEquals("3", result.get("bothJackson"));

        mapper = new ObjectMapper();
        pair = new AnnotationIntrospector.Pair(jaxbAI, jacksonAI);
        mapper.getSerializationConfig().setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean());
        assertEquals(3, result.size());
        assertEquals("1", result.get("jackson"));
        assertEquals("2", result.get("jaxb"));
        // JAXB one should have priority
        assertEquals("3", result.get("bothJaxb"));
    }

    public void testNaming() throws Exception
    {
        ObjectMapper mapper;
        AnnotationIntrospector jacksonAI = new JacksonAnnotationIntrospector();
        AnnotationIntrospector jaxbAI = new JaxbAnnotationIntrospector();
        AnnotationIntrospector pair;
        Map<String,Object> result;

        mapper = new ObjectMapper();
        // first: test with Jackson/Jaxb pair (jackson having precedence)
        pair = new AnnotationIntrospector.Pair(jacksonAI, jaxbAI);
        mapper.getSerializationConfig().setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean2());
        assertEquals(2, result.size());
        // order shouldn't really matter here...
        assertEquals("123", result.get("jackson"));
        assertEquals("abc", result.get("jaxb"));

        mapper = new ObjectMapper();
        pair = new AnnotationIntrospector.Pair(jaxbAI, jacksonAI);
        mapper.getSerializationConfig().setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean2());
        /* Hmmh. Not 100% sure what JAXB would dictate.... thus...
         */
        assertEquals(2, result.size());
        assertEquals("abc", result.get("jaxb"));
        //assertEquals("123", result.get("jackson"));
    }
}
