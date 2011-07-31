package org.codehaus.jackson.map.contextual;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JacksonAnnotation;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.module.SimpleModule;

/**
 * Test cases to verify that it is possible to define serializers
 * that can use contextual information (like field/method
 * annotations) for configuration.
 * 
 * @since 1.7
 */
public class TestContextualSerialization extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /* NOTE: important; MUST be considered a 'Jackson' annotation to be seen
     * (or recognized otherwise via AnnotationIntrospect.isHandled())
     */
    @Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotation
    public @interface Prefix {
        public String value();
    }

    static class ContextualBean
    {
        protected final String _value;

        public ContextualBean(String s) { _value = s; }

        @Prefix("see:")
        public String getValue() { return _value; }
    }

    // For [JACKSON-569]
    static class AnnotatedContextualBean
    {
        @Prefix("prefix->")
        @JsonSerialize(using=AnnotatedContextualSerializer.class)
        protected final String value;

        public AnnotatedContextualBean(String s) { value = s; }
    }

    
    @Prefix("wrappedBean:")
    static class ContextualBeanWrapper
    {
        @Prefix("wrapped:")
        public ContextualBean wrapped;
        
        public ContextualBeanWrapper(String s) {
            wrapped = new ContextualBean(s);
        }
    }
    
    static class ContextualArrayBean
    {
        @Prefix("array->")
        public final String[] beans;
        
        public ContextualArrayBean(String... strings) {
            beans = strings;
        }
    }
    
    static class ContextualListBean
    {
        @Prefix("list->")
        public final List<String> beans = new ArrayList<String>();

        public ContextualListBean(String... strings) {
            for (String string : strings) {
                beans.add(string);
            }
        }
    }
    
    static class ContextualMapBean
    {
        @Prefix("map->")
        public final Map<String, String> beans = new HashMap<String, String>();
    }
    
    /**
     * Another bean that has class annotations that should be visible for
     * contextualizer, too
     */
    @Prefix("Voila->")
    static class BeanWithClassConfig
    {
        public String value;

        public BeanWithClassConfig(String v) { value = v; }
    }
    
    /**
     * Annotation-based contextual serializer that simply prepends piece of text.
     */
    static class AnnotatedContextualSerializer
        extends JsonSerializer<String>
        implements ContextualSerializer<String>
    {
        protected final String _prefix;
        
        public AnnotatedContextualSerializer() { this(""); }
        public AnnotatedContextualSerializer(String p) {
            _prefix = p;
        }

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            jgen.writeString(_prefix + value);
        }

        @Override
        public JsonSerializer<String> createContextual(SerializationConfig config, BeanProperty property)
                throws JsonMappingException
        {
            String prefix = "UNKNOWN";
            Prefix ann = property.getAnnotation(Prefix.class);
            if (ann == null) {
                ann = property.getContextAnnotation(Prefix.class);
            }
            if (ann != null) {
                prefix = ann.value();
            }
            return new AnnotatedContextualSerializer(prefix);
        }
    }

    static class ContextualAndResolvable
        extends JsonSerializer<String>
        implements ContextualSerializer<String>, ResolvableSerializer
    {
        protected boolean isContextual;
        protected boolean isResolved;

        public ContextualAndResolvable() { this(false); }
        
        public ContextualAndResolvable(boolean contextual)
        {
            isContextual = contextual;
            isResolved = false;
        }
        
        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            jgen.writeString("contextual="+isContextual+",resolved="+isResolved);
        }

        @Override
        public JsonSerializer<String> createContextual(SerializationConfig config, BeanProperty property)
                throws JsonMappingException
        {
            return new ContextualAndResolvable(true);
        }

        @Override
        public void resolve(SerializerProvider provider) {
            isResolved = true;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * Test to verify that contextual serializer can make use of property
     * (method, field) annotations.
     */
    public void testMethodAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        assertEquals("{\"value\":\"see:foobar\"}", mapper.writeValueAsString(new ContextualBean("foobar")));
    }

    /**
     * Test to verify that contextual serializer can also use annotations
     * for enclosing class.
     */
    public void testClassAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        assertEquals("{\"value\":\"Voila->xyz\"}", mapper.writeValueAsString(new BeanWithClassConfig("xyz")));
    }

    public void testWrappedBean() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        assertEquals("{\"wrapped\":{\"value\":\"see:xyz\"}}", mapper.writeValueAsString(new ContextualBeanWrapper("xyz")));
    }
    
    /**
     * Serializer should get passed property context even if contained in an array.
     */
    public void testMethodAnnotationInArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        ContextualArrayBean beans = new ContextualArrayBean("123");
        assertEquals("{\"beans\":[\"array->123\"]}", mapper.writeValueAsString(beans));
    }

    /**
     * Serializer should get passed property context even if contained in a Collection.
     */
    public void testMethodAnnotationInList() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        ContextualListBean beans = new ContextualListBean("abc");
        assertEquals("{\"beans\":[\"list->abc\"]}", mapper.writeValueAsString(beans));
    }

    /**
     * Serializer should get passed property context even if contained in a Collection.
     */
    public void testMethodAnnotationInMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        ContextualMapBean map = new ContextualMapBean();
        map.beans.put("first", "In Map");
        assertEquals("{\"beans\":{\"first\":\"map->In Map\"}}", mapper.writeValueAsString(map));
    }

    public void testContextualViaAnnotation() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AnnotatedContextualBean bean = new AnnotatedContextualBean("abc");
        assertEquals("{\"value\":\"prefix->abc\"}", mapper.writeValueAsString(bean));
    }

    // [JACKSON-647]: is resolve() called for contextual instances?
    public void testResolveOnContextual() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new ContextualAndResolvable());
        mapper.registerModule(module);
        assertEquals(quote("contextual=true,resolved=true"), mapper.writeValueAsString("abc"));
    }
}
