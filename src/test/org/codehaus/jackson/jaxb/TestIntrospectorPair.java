package org.codehaus.jackson.jaxb;

import java.util.*;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Simple testing that <code>AnnotationIntrospector.Pair</code> works as
 * expected, when used with Jackson and JAXB-based introspector.
 *
 * @author Tatu Saloranta
 */
@SuppressWarnings("deprecation")
public class TestIntrospectorPair
    extends org.codehaus.jackson.map.BaseMapTest
{
    final static AnnotationIntrospector _jacksonAI = new JacksonAnnotationIntrospector();
    final static AnnotationIntrospector _jaxbAI = new JaxbAnnotationIntrospector();

    public TestIntrospectorPair() { super(); }
    
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
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

    /**
     * And a bean to check how "ignore" annotations work with
     * various combinations of annotation introspectors
     */
    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    @JsonWriteNullProperties
    static class IgnoreBean
    {
        @JsonIgnore
        public int getNumber() { return 13; }

        @XmlTransient
        public String getText() { return "abc"; }

        public boolean getAny() { return true; }
    }

    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    static class IgnoreFieldBean
    {
        @JsonIgnore public int number = 7;
        @XmlTransient public String text = "123";
        public boolean any = true;
    }

    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    @XmlRootElement(name="test", namespace="urn:whatever")
    static class NamespaceBean
    {
        public String string;
    }

    // Testing [JACKSON-495]
    static class CreatorBean {
        @JsonCreator
        public CreatorBean(@JsonProperty("name") String name) {
            ;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper;
        AnnotationIntrospector pair;
        Map<String,Object> result;

        mapper = new ObjectMapper();
        // first: test with Jackson/Jaxb pair (jackson having precedence)
        pair = new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI);
        mapper.getSerializationConfig().setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean());
        assertEquals(3, result.size());
        assertEquals("1", result.get("jackson"));
        assertEquals("2", result.get("jaxb"));
        // jackson one should have priority
        assertEquals("3", result.get("bothJackson"));

        mapper = new ObjectMapper();
        pair = new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI);
        mapper.getSerializationConfig().setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean());
        assertEquals(3, result.size());
        assertEquals("1", result.get("jackson"));
        assertEquals("2", result.get("jaxb"));
        // JAXB one should have priority
        assertEquals("3", result.get("bothJaxb"));
    }

    public void testProperties() throws Exception
    {
        AnnotationIntrospector pair = new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI);
        assertTrue(pair.isHandled(NamespaceBean.class.getAnnotation(XmlRootElement.class)));
        assertTrue(pair.isHandled(IgnoreBean.class.getAnnotation(JsonWriteNullProperties.class)));

        /* won't work without actually getting class annotations etc
        AnnotatedConstructor con = new AnnotatedConstructor(getClass().getConstructor(), null, null);
        assertFalse(pair.isIgnorableConstructor(con));
        */
    }

    public void testNaming() throws Exception
    {
        ObjectMapper mapper;
        AnnotationIntrospector pair;
        Map<String,Object> result;

        mapper = new ObjectMapper();
        // first: test with Jackson/Jaxb pair (jackson having precedence)
        pair = new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI);
        mapper.getSerializationConfig().setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean2());
        assertEquals(2, result.size());
        // order shouldn't really matter here...
        assertEquals("123", result.get("jackson"));
        assertEquals("abc", result.get("jaxb"));

        mapper = new ObjectMapper();
        pair = new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI);
        mapper.getSerializationConfig().setAnnotationIntrospector(pair);

        result = writeAndMap(mapper, new NamedBean2());
        /* Hmmh. Not 100% sure what JAXB would dictate.... thus...
         */
        assertEquals(2, result.size());
        assertEquals("abc", result.get("jaxb"));
        //assertEquals("123", result.get("jackson"));
    }

    public void testSimpleIgnore() throws Exception
    {
        // first: only Jackson introspector (default)
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> result = writeAndMap(mapper, new IgnoreBean());
        assertEquals(2, result.size());
        assertEquals("abc", result.get("text"));
        assertEquals(Boolean.TRUE, result.get("any"));

        // Then JAXB only
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(_jaxbAI);

        // jackson one should have priority
        result = writeAndMap(mapper, new IgnoreBean());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(13), result.get("number"));
        assertEquals(Boolean.TRUE, result.get("any"));

        // then both, Jackson first
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI));

        result = writeAndMap(mapper, new IgnoreBean());
        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result.get("any"));

        // then both, JAXB first
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI));

        result = writeAndMap(mapper, new IgnoreBean());
        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result.get("any"));
    }

    public void testSimpleFieldIgnore() throws Exception
    {
        ObjectMapper mapper;

        // first: only Jackson introspector (default)
        mapper = new ObjectMapper();
        Map<String,Object> result = writeAndMap(mapper, new IgnoreFieldBean());
        assertEquals(2, result.size());
        assertEquals("123", result.get("text"));
        assertEquals(Boolean.TRUE, result.get("any"));

        // Then JAXB only
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(_jaxbAI);

        // jackson one should have priority
        result = writeAndMap(mapper, new IgnoreFieldBean());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(7), result.get("number"));
        assertEquals(Boolean.TRUE, result.get("any"));

        // then both, Jackson first
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI));

        result = writeAndMap(mapper, new IgnoreFieldBean());
        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result.get("any"));

        // then both, JAXB first
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI));

        result = writeAndMap(mapper, new IgnoreFieldBean());
        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result.get("any"));
    }

    public void testSimpleOther() throws Exception
    {
        // Let's use Jackson+JAXB comb
        AnnotationIntrospector ann = new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI);

        AnnotatedClass testClass = AnnotatedClass.construct(NamedBean.class, ann, null);
        assertNull(ann.findCachability(testClass));
        //assertNull(ann.findSerializationInclusion(testClass, null));

        JavaType type = TypeFactory.type(Object.class);
        assertNull(ann.findDeserializationType(testClass, type, null));
        assertNull(ann.findDeserializationContentType(testClass, type, null));
        assertNull(ann.findDeserializationKeyType(testClass, type, null));
        assertNull(ann.findSerializationType(testClass));

        assertNull(ann.findDeserializer(testClass));
        assertNull(ann.findContentDeserializer(testClass));
        assertNull(ann.findKeyDeserializer(testClass));

        assertFalse(ann.hasCreatorAnnotation(testClass));
    }
    
    public void testRootName() throws Exception
    {
        // first: test with Jackson/Jaxb pair (jackson having precedence)
        AnnotationIntrospector pair = new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI);
        assertNull(pair.findRootName(AnnotatedClass.construct(NamedBean.class, pair, null)));
        assertEquals("test", pair.findRootName(AnnotatedClass.construct(NamespaceBean.class, pair, null)));

        // then reverse; should make no difference
        pair = new AnnotationIntrospector.Pair(_jaxbAI, _jacksonAI);
        assertNull(pair.findRootName(AnnotatedClass.construct(NamedBean.class, pair, null)));
        assertEquals("test", pair.findRootName(AnnotatedClass.construct(NamespaceBean.class, pair, null)));
    }

    /**
     * Test that will just use Jackson annotations, but did trigger [JACKSON-495] due to a bug
     * in JAXB annotation introspector.
     */
    public void testIssue495() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().setAnnotationIntrospector(new AnnotationIntrospector.Pair(_jacksonAI, _jaxbAI));
        CreatorBean bean = mapper.readValue("{\"name\":\"foo\"}", CreatorBean.class);
        assertNotNull(bean);
    }
}
