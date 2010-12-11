package org.codehaus.jackson.map.deser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.annotate.JacksonAnnotation;
import org.codehaus.jackson.map.*;
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
    @Target({ElementType.FIELD})
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
            String propertyName = (ann == null) ? "UNKNOWN" : ann.value();
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
}
