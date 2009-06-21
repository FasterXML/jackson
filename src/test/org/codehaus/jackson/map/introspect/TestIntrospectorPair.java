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

    /*
    /////////////////////////////////////////////////////
    // Unit tests
    /////////////////////////////////////////////////////
     */

    public void testSimpleNaming1() throws Exception
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
}
