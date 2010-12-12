package org.codehaus.jackson.map.deser;

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
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.ser.TestContextualSerialization.Prefix;

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

    static class ContextualArrayBean
    {
        @Prefix("array->")
        public String[] beans;
    }
    
    static class ContextualListBean
    {
        @Prefix("list->")
        public List<String> beans;
    }
    
    static class ContextualMapBean
    {
        @Prefix("map->")
        public Map<String, String> beans;
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
    }

    public void testSimpleWithAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(ContextualType.class, new AnnotatedContextualDeserializer());
        mapper.registerModule(module);
        ContextualBean bean = mapper.readValue("{\"a\":\"1\",\"b\":\"2\"}", ContextualBean.class);
        assertEquals("NameA=1", bean.a.value);
        assertEquals("NameB=2", bean.b.value);
    }

    public void testSimpleWithClassAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(ContextualType.class, new AnnotatedContextualDeserializer());
        mapper.registerModule(module);
        ContextualClassBean bean = mapper.readValue("{\"a\":\"1\",\"b\":\"2\"}", ContextualClassBean.class);
        assertEquals("Class=1", bean.a.value);
        assertEquals("NameB=2", bean.b.value);
    }
    
    public void testAnnotatedCtor() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(ContextualType.class, new AnnotatedContextualDeserializer());
        mapper.registerModule(module);
        ContextualCtorBean bean = mapper.readValue("{\"a\":\"foo\",\"b\":\"bar\"}", ContextualCtorBean.class);
        assertEquals("CtorA=foo", bean.a);
        assertEquals("CtorB=bar", bean.b);
    }

    public void testAnnotatedArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(ContextualType.class, new AnnotatedContextualDeserializer());
        mapper.registerModule(module);
        ContextualArrayBean bean = mapper.readValue("{\"beans\":[\"x\"]}", ContextualArrayBean.class);
        assertEquals(1, bean.beans.length);
        assertEquals("array->x", bean.beans[0]);
    }

    public void testAnnotatedList() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(ContextualType.class, new AnnotatedContextualDeserializer());
        mapper.registerModule(module);
        ContextualListBean bean = mapper.readValue("{\"beans\":[\"x\"]}", ContextualListBean.class);
        assertEquals(1, bean.beans.size());
        assertEquals("list->x", bean.beans.get(0));
    }

    public void testAnnotatedMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(ContextualType.class, new AnnotatedContextualDeserializer());
        mapper.registerModule(module);
        ContextualMapBean bean = mapper.readValue("{\"beans\":{\"a\":\"b\"}}", ContextualMapBean.class);
        assertEquals(1, bean.beans.size());
        Map.Entry<String,String> entry = bean.beans.entrySet().iterator().next();
        assertEquals("a", entry.getKey());
        assertEquals("map->b", entry.getKey());
    }
}
