package org.codehaus.jackson.jaxb;

import java.util.*;
import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Tests for verifying auto-detection settings with JAXB annotations.
 *
 * @author Tatu Saloranta
 */
public class TestJaxbAutoDetect
    extends org.codehaus.jackson.map.BaseMapTest
{
    /*
    /////////////////////////////////////////////////////
    // Helper beans
    /////////////////////////////////////////////////////
     */

    /* Bean for testing problem [JACKSON-183]: with normal
     * auto-detect enabled, 2 fields visible; if disabled, just 1.
     * NOTE: should NOT include "XmlAccessorType", since it will
     * have priority over global defaults
     */
    static class Jackson183Bean {
        public String getA() { return "a"; }

        @XmlElement public String getB() { return "b"; }

        // JAXB (or Bean introspection) mandates use of matching setters...
        public void setA(String str) { }
        public void setB(String str) { }
    }

    static class Identified
    {
        Object id;
        
        @XmlAttribute(name="id")
        public Object getIdObject() {
            return id;
        }
        public void setId(Object id) { this.id = id; }
    }

    /*
    /////////////////////////////////////////////////////
    // Unit tests
    /////////////////////////////////////////////////////
     */

    public void testAutoDetectDisable() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        Jackson183Bean bean = new Jackson183Bean();
        Map<String,Object> result;

        // Ok: by default, should see 2 fields:
        result = writeAndMap(mapper, bean);
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("b", result.get("b"));

        // But when disabling auto-detection, just one
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        mapper.configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, false);
        result = writeAndMap(mapper, bean);
        assertEquals(1, result.size());
        assertNull(result.get("a"));
        assertEquals("b", result.get("b"));
    }

    // @since 1.5
    public void testBug246() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        Identified id = new Identified();
        id.id = "123";
        assertEquals("{\"id\":\"123\"}", mapper.writeValueAsString(id));
    }
}
