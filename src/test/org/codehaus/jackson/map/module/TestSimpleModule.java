package org.codehaus.jackson.map.module;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ser.SerializerBase;
import org.codehaus.jackson.map.type.ArrayType;
import org.codehaus.jackson.map.type.CollectionType;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.type.JavaType;

public class TestSimpleModule extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes; module support
    /**********************************************************
     */

    protected static class TestModule extends Module
    {
        private Deserializers _deserializers;
        private Serializers _serializers;

        public TestModule(Deserializers d, Serializers s)
        {
            _deserializers = d;
            _serializers = s;
        }
        
        @Override
        public String getModuleName() { return "test"; }

        @Override
        public Version version() { return new Version(1, 0, 0, null); }

        @Override
        public void setupModule(SetupContext context)
        {
            if (_deserializers != null) {
                context.addDeserializers(_deserializers);
            }
            if (_serializers != null) {
                context.addSerializers(_serializers);
            }
        }
    }

    protected static class MySerializers implements Serializers
    {
        protected final HashMap<Class<?>, JsonSerializer<?>> _serializers = 
            new HashMap<Class<?>, JsonSerializer<?>>();

        public MySerializers(JsonSerializer<?>... sers) {
            for (JsonSerializer<?> ser : sers) {
                _serializers.put(ser.handledType(), ser);
            }
        }
        
        @Override
        public JsonSerializer<?> findSerializer(JavaType type,
                SerializationConfig config, BeanDescription beanDesc)
        {
            return _serializers.get(type.getRawClass());
        }
        
    }

    protected static class MyDeserializers implements Deserializers
    {
        protected final HashMap<Class<?>, JsonDeserializer<?>> _deserializers = 
            new HashMap<Class<?>, JsonDeserializer<?>>();

        public MyDeserializers() { }
        
        public void add(Class<?> type, JsonDeserializer<?> deser) {
            _deserializers.put(type, deser);
        }

        @Override
        public JsonDeserializer<?> findArrayDeserializer(ArrayType type,
                DeserializationConfig config, DeserializerProvider provider,
                TypeDeserializer elementTypeDeserializer,
                JsonDeserializer<?> elementDeserializer) {
            return null;
        }

        @Override
        public JsonDeserializer<?> findBeanDeserializer(JavaType type,
                DeserializationConfig config, DeserializerProvider provider,
                BeanDescription beanDesc) {
            return _deserializers.get(type.getRawClass());
        }

        @Override
        public JsonDeserializer<?> findCollectionDeserializer(
                CollectionType type, DeserializationConfig config,
                DeserializerProvider provider, BeanDescription beanDesc,
                TypeDeserializer elementTypeDeserializer,
                JsonDeserializer<?> elementDeserializer) {
            return null;
        }

        @Override
        public JsonDeserializer<?> findEnumDeserializer(Class<?> type,
                DeserializationConfig config, BeanDescription beanDesc) {
            return null;
        }

        @Override
        public JsonDeserializer<?> findMapDeserializer(MapType type,
                DeserializationConfig config, DeserializerProvider provider,
                BeanDescription beanDesc, KeyDeserializer keyDeserializer,
                TypeDeserializer elementTypeDeserializer,
                JsonDeserializer<?> elementDeserializer) {
            return null;
        }

        @Override
        public JsonDeserializer<?> findTreeNodeDeserializer(
                Class<? extends JsonNode> nodeType, DeserializationConfig config) {
            return null;
        }
    }
    
    /*
    /**********************************************************
    /* Helper classes; simple beans and their handlers
    /**********************************************************
     */
    
    /**
     * Trivial bean that requires custom serializer and deserializer
     */
    final static class CustomBean
    {
        protected String str;
        protected int num;
        
        public CustomBean(String s, int i) {
            str = s;
            num = i;
        }
    }

    // Extend SerializerBase to get access to declared handledType
    static class CustomBeanSerializer extends SerializerBase<CustomBean>
    {
        public CustomBeanSerializer() {
            super(CustomBean.class);
        }

        @Override
        public void serialize(CustomBean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            // We will write it as a String, with '|' as delimiter
            jgen.writeString(value.str + "|" + value.num);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException
        {
            return null;
        }
    }

    static class CustomBeanDeserializer extends JsonDeserializer<CustomBean>
    {
        @Override
        public CustomBean deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            String text = jp.getText();
            int ix = text.indexOf('|');
            if (ix < 0) {
                throw new IOException("Failed to parse String value of \""+text+"\"");
            }
            String str = text.substring(0, ix);
            int num = Integer.parseInt(text.substring(ix+1));
            return new CustomBean(str, num);
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests; first, verifying need for custom handlers
    /**********************************************************
     */

    /**
     * Basic test to ensure we do not have functioning default
     * serializers for custom types used in tets.
     */
    public void testWithoutModule()
    {
        ObjectMapper mapper = new ObjectMapper();
        // first: serialization failure:
        try {
            mapper.writeValueAsString(new CustomBean("foo", 3));
            fail("Should have caused an exception");
        } catch (IOException e) {
            verifyException(e, "No serializer found");
        }

        // then deserialization
        try {
            mapper.readValue("{\"str\":\"ab\",\"num\":2}", CustomBean.class);
            fail("Should have caused an exception");
        } catch (IOException e) {
            verifyException(e, "No suitable constructor found");
        }
    }

    /*
    /**********************************************************
    /* Unit tests; simple serializers, deserializers
    /**********************************************************
     */
    
    public void testSimpleWithSerializers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        MySerializers ser = new MySerializers(new CustomBeanSerializer());
        mapper.registerModule(new TestModule( null, ser));
        assertEquals(quote("abcde|5"),
               mapper.writeValueAsString(new CustomBean("abcde", 5)));
    }

    public void testSimpleWithDeserializers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        MyDeserializers deser = new MyDeserializers();
        deser.add(CustomBean.class, new CustomBeanDeserializer());
        mapper.registerModule(new TestModule(deser, null));
        CustomBean bean = mapper.readValue(quote("xyz|3"), CustomBean.class);
        assertEquals("xyz", bean.str);
        assertEquals(3, bean.num);
    }

}
