package org.codehaus.jackson.jaxb;

import java.io.*;
import java.math.BigDecimal;
import java.util.Map;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
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
    /**********************************************************
    /* Helper beans
    /**********************************************************
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

    @XmlRootElement(name="bah")
    public static class JaxbAnnotatedObject {

        private BigDecimal number;

        public JaxbAnnotatedObject() { }
        
        public JaxbAnnotatedObject(String number) {
            this.number = new BigDecimal(number);
        }

        @XmlElement
        public void setNumber(BigDecimal number) {
            this.number = number;
        }

        @XmlTransient
        public BigDecimal getNumber() {
            return number;
        }

        @XmlElement(name = "number")
        public BigDecimal getNumberString() {
            return number;
        }
    }

    public static class DualAnnotationObjectMapper extends ObjectMapper {

        public DualAnnotationObjectMapper() {
            super();
            AnnotationIntrospector primary = new JaxbAnnotationIntrospector();
            AnnotationIntrospector secondary = new JacksonAnnotationIntrospector();

            // make de/serializer use JAXB annotations first, then jackson ones
            AnnotationIntrospector pair = new AnnotationIntrospector.Pair(primary, secondary);
            setAnnotationIntrospector(pair);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testAutoDetectDisable() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        Jackson183Bean bean = new Jackson183Bean();
        Map<String,Object> result;

        // Ok: by default, should see 2 fields:
        result = writeAndMap(mapper, bean);
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("b", result.get("b"));

        // But when disabling auto-detection, just one
        mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        mapper.configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, false);
        result = writeAndMap(mapper, bean);
        assertEquals(1, result.size());
        assertNull(result.get("a"));
        assertEquals("b", result.get("b"));
    }

    // @since 1.5
    public void testIssue246() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        Identified id = new Identified();
        id.id = "123";
        assertEquals("{\"id\":\"123\"}", mapper.writeValueAsString(id));
    }

    // [JACKSON-556]
    public void testJaxbAnnotatedObject() throws Exception
    {
        JaxbAnnotatedObject original = new JaxbAnnotatedObject("123");
        ObjectMapper mapper = new DualAnnotationObjectMapper();
        String json = mapper.writeValueAsString(original);
        assertFalse("numberString field in JSON", json.contains("numberString")); // kinda hack-y :)
        JaxbAnnotatedObject result = mapper.readValue(json, JaxbAnnotatedObject.class);
        assertEquals(new BigDecimal("123"), result.number);
    }

    /*
    public void testJaxbAnnotatedObjectXML() throws Exception
    {
        JAXBContext ctxt = JAXBContext.newInstance(JaxbAnnotatedObject.class);
        JaxbAnnotatedObject original = new JaxbAnnotatedObject("123");
        StringWriter sw = new StringWriter();
        ctxt.createMarshaller().marshal(original, sw);
        String xml = sw.toString();
        JaxbAnnotatedObject result = (JaxbAnnotatedObject) ctxt.createUnmarshaller().unmarshal(new StringReader(xml));
        assertEquals(new BigDecimal("123"), result.number);
    }
    */
}
