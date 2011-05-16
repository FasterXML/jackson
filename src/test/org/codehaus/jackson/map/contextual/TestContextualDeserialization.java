package org.codehaus.jackson.map.contextual;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.annotate.JacksonAnnotation;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.module.SimpleModule;

/**
 * Test cases to verify that it is possible to define deserializers
 * that can use contextual information (like field/method
 * annotations) for configuration.
 * 
 * @since 1.7
 */
public class TestContextualDeserialization extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /* NOTE: important; MUST be considered a 'Jackson' annotation to be seen
     * (or recognized otherwise via AnnotationIntrospect.isHandled())
     */
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotation
    public @interface Name {
        public String value();
    }
    
    static class ContextualType {
        protected String value;
        
        public ContextualType(String v) { value = v; }
    }
    
    static class ContextualBean
    {
        @Name("NameA")
        public ContextualType a;
        @Name("NameB")
        public ContextualType b;
    }
    
    static class ContextualCtorBean
    {
        protected String a, b;

        @JsonCreator
        public ContextualCtorBean(
                @Name("CtorA") @JsonProperty("a") ContextualType a,
                @Name("CtorB") @JsonProperty("b") ContextualType b)
        {
            this.a = a.value;
            this.b = b.value;
        }
    }

    @Name("Class")
    static class ContextualClassBean
    {
        public ContextualType a;

        @Name("NameB")
        public ContextualType b;
    }

    static class AnnotatedContextualClassBean
    {
        @Name("xyz")
        @JsonDeserialize(using=AnnotatedContextualDeserializer.class)
        public ContextualType value;
    }
    
    static class ContextualArrayBean
    {
        @Name("array")
        public ContextualType[] beans;
    }
    
    static class ContextualListBean
    {
        @Name("list")
        public List<ContextualType> beans;
    }
    
    static class ContextualMapBean
    {
        @Name("map")
        public Map<String, ContextualType> beans;
    }
    
    static class MyContextualDeserializer
        extends JsonDeserializer<ContextualType>
        implements ContextualDeserializer<ContextualType>
    {
        protected final String _fieldName;
        
        public MyContextualDeserializer() { this(""); }
        public MyContextualDeserializer(String fieldName) {
            _fieldName = fieldName;
        }

        @Override
        public ContextualType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            return new ContextualType(""+_fieldName+"="+jp.getText());
        }

        @Override
        public JsonDeserializer<ContextualType> createContextual(DeserializationConfig config,
                BeanProperty property)
            throws JsonMappingException
        {
            return new MyContextualDeserializer(property.getName());
        }
    }

    /**
     * Alternative that uses annotation for choosing name to use
     */
    static class AnnotatedContextualDeserializer
        extends JsonDeserializer<ContextualType>
        implements ContextualDeserializer<ContextualType>
    {
        protected final String _fieldName;
        
        public AnnotatedContextualDeserializer() { this(""); }
        public AnnotatedContextualDeserializer(String fieldName) {
            _fieldName = fieldName;
        }
    
        @Override
        public ContextualType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            return new ContextualType(""+_fieldName+"="+jp.getText());
        }
    
        @Override
        public JsonDeserializer<ContextualType> createContextual(DeserializationConfig config,
                BeanProperty property)
            throws JsonMappingException
        {
            Name ann = property.getAnnotation(Name.class);
            if (ann == null) {
                ann = property.getContextAnnotation(Name.class);
            }
            String propertyName = (ann == null) ?  "UNKNOWN" : ann.value();
            return new MyContextualDeserializer(propertyName);
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(ContextualType.class, new MyContextualDeserializer());
        mapper.registerModule(module);
        ContextualBean bean = mapper.readValue("{\"a\":\"1\",\"b\":\"2\"}", ContextualBean.class);
        assertEquals("a=1", bean.a.value);
        assertEquals("b=2", bean.b.value);

        // try again, to ensure caching etc works
        bean = mapper.readValue("{\"a\":\"3\",\"b\":\"4\"}", ContextualBean.class);
        assertEquals("a=3", bean.a.value);
        assertEquals("b=4", bean.b.value);
    }

    public void testSimpleWithAnnotations() throws Exception
    {
        ObjectMapper mapper = _mapperWithAnnotatedContextual();
        ContextualBean bean = mapper.readValue("{\"a\":\"1\",\"b\":\"2\"}", ContextualBean.class);
        assertEquals("NameA=1", bean.a.value);
        assertEquals("NameB=2", bean.b.value);

        // try again, to ensure caching etc works
        bean = mapper.readValue("{\"a\":\"x\",\"b\":\"y\"}", ContextualBean.class);
        assertEquals("NameA=x", bean.a.value);
        assertEquals("NameB=y", bean.b.value);
    }

    public void testSimpleWithClassAnnotations() throws Exception
    {
        ObjectMapper mapper = _mapperWithAnnotatedContextual();
        ContextualClassBean bean = mapper.readValue("{\"a\":\"1\",\"b\":\"2\"}", ContextualClassBean.class);
        assertEquals("Class=1", bean.a.value);
        assertEquals("NameB=2", bean.b.value);
        // and again
        bean = mapper.readValue("{\"a\":\"123\",\"b\":\"345\"}", ContextualClassBean.class);
        assertEquals("Class=123", bean.a.value);
        assertEquals("NameB=345", bean.b.value);
    }
    
    public void testAnnotatedCtor() throws Exception
    {
        ObjectMapper mapper = _mapperWithAnnotatedContextual();
        ContextualCtorBean bean = mapper.readValue("{\"a\":\"foo\",\"b\":\"bar\"}", ContextualCtorBean.class);
        assertEquals("CtorA=foo", bean.a);
        assertEquals("CtorB=bar", bean.b);

        bean = mapper.readValue("{\"a\":\"1\",\"b\":\"0\"}", ContextualCtorBean.class);
        assertEquals("CtorA=1", bean.a);
        assertEquals("CtorB=0", bean.b);
    }

    public void testAnnotatedArray() throws Exception
    {
        ObjectMapper mapper = _mapperWithAnnotatedContextual();
        ContextualArrayBean bean = mapper.readValue("{\"beans\":[\"x\"]}", ContextualArrayBean.class);
        assertEquals(1, bean.beans.length);
        assertEquals("array=x", bean.beans[0].value);

        bean = mapper.readValue("{\"beans\":[\"a\",\"b\"]}", ContextualArrayBean.class);
        assertEquals(2, bean.beans.length);
        assertEquals("array=a", bean.beans[0].value);
        assertEquals("array=b", bean.beans[1].value);
    }

    public void testAnnotatedList() throws Exception
    {
        ObjectMapper mapper = _mapperWithAnnotatedContextual();
        ContextualListBean bean = mapper.readValue("{\"beans\":[\"x\"]}", ContextualListBean.class);
        assertEquals(1, bean.beans.size());
        assertEquals("list=x", bean.beans.get(0).value);

        bean = mapper.readValue("{\"beans\":[\"x\",\"y\",\"z\"]}", ContextualListBean.class);
        assertEquals(3, bean.beans.size());
        assertEquals("list=x", bean.beans.get(0).value);
        assertEquals("list=y", bean.beans.get(1).value);
        assertEquals("list=z", bean.beans.get(2).value);
    }

    public void testAnnotatedMap() throws Exception
    {
        ObjectMapper mapper = _mapperWithAnnotatedContextual();
        ContextualMapBean bean = mapper.readValue("{\"beans\":{\"a\":\"b\"}}", ContextualMapBean.class);
        assertEquals(1, bean.beans.size());
        Map.Entry<String,ContextualType> entry = bean.beans.entrySet().iterator().next();
        assertEquals("a", entry.getKey());
        assertEquals("map=b", entry.getValue().value);

        bean = mapper.readValue("{\"beans\":{\"x\":\"y\",\"1\":\"2\"}}", ContextualMapBean.class);
        assertEquals(2, bean.beans.size());
        Iterator<Map.Entry<String,ContextualType>> it = bean.beans.entrySet().iterator();
        entry = it.next();
        assertEquals("x", entry.getKey());
        assertEquals("map=y", entry.getValue().value);
        entry = it.next();
        assertEquals("1", entry.getKey());
        assertEquals("map=2", entry.getValue().value);
    }

    // ensure that direct associations also work
    public void testAnnotatedContextual() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AnnotatedContextualClassBean bean = mapper.readValue(
                "{\"value\":\"a\"}",
              AnnotatedContextualClassBean.class);
        assertNotNull(bean);
        assertEquals("xyz=a", bean.value.value);
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private ObjectMapper _mapperWithAnnotatedContextual()
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(ContextualType.class, new AnnotatedContextualDeserializer());
        mapper.registerModule(module);
        return mapper;
    }
}
