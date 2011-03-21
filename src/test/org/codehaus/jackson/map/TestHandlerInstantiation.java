package org.codehaus.jackson.map;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonTypeIdResolver;
import org.codehaus.jackson.map.annotate.JsonTypeResolver;

public class TestHandlerInstantiation extends BaseMapTest
{
    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    @JsonDeserialize(using=MyBeanDeserializer.class)
    @JsonSerialize(using=MyBeanSerializer.class)
    static class MyBean
    {
        public String value;

        public MyBean() { this(null); }
        public MyBean(String s) { value = s; }
    }

    @SuppressWarnings("serial")
    @JsonDeserialize(keyUsing=MyKeyDeserializer.class)
    static class MyMap extends HashMap<String,String> { }
    
    static class MyBeanDeserializer extends JsonDeserializer<MyBean>
    {
        public String _prefix = "";

        public MyBeanDeserializer(String p) {
            _prefix  = p;
        }
        
        @Override
        public MyBean deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            return new MyBean(_prefix+jp.getText());
        }
    }

    static class MyKeyDeserializer extends KeyDeserializer
    {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            return "KEY";
        }
    }
    
    static class MyBeanSerializer extends JsonSerializer<MyBean>
    {
        public String _prefix = "";

        public MyBeanSerializer(String p) {
            _prefix  = p;
        }
        
        @Override
        public void serialize(MyBean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            jgen.writeString(_prefix + value.value);
        }
    }
    
    static class MyInstantiator extends HandlerInstantiator
    {
        private final String _prefix;
        
        public MyInstantiator(String p) {
            _prefix = p;
        }
        
        @Override
        public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated,
                Class<? extends JsonDeserializer<?>> deserClass)
        {
            if (deserClass == MyBeanDeserializer.class) {
                return new MyBeanDeserializer(_prefix);
            }
            return null;
        }

        @Override
        public KeyDeserializer keyDeserializerInstance(DeserializationConfig config,
                Annotated annotated, Class<? extends KeyDeserializer> keyDeserClass)
        {
System.err.println("DEBUG: seen "+keyDeserClass);            
            if (keyDeserClass == MyKeyDeserializer.class) {
                return new MyKeyDeserializer();
            }
            return null;
            
        }
        
        @Override
        public JsonSerializer<?> serializerInstance(SerializationConfig config,
                Annotated annotated, Class<? extends JsonSerializer<?>> serClass)
        {
            if (serClass == MyBeanSerializer.class) {
                return new MyBeanSerializer(_prefix);
            }
            return null;
        }

        @Override
        public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config,
                Annotated annotated, Class<? extends TypeIdResolver> resolverClass)
        {
            return null;
        }

        @Override
        public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated,
                Class<? extends TypeResolverBuilder<?>> builderClass)
        {
            return null;
        }
    }
    
    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    public void testDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setHandlerInstantiator(new MyInstantiator("abc:"));
        MyBean result = mapper.readValue(quote("123"), MyBean.class);
        assertEquals("abc:123", result.value);
    }

    public void testKeyDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setHandlerInstantiator(new MyInstantiator("abc:"));
        MyMap map = mapper.readValue("{\"a\":\"b\"}", MyMap.class);
        // easiest to test by just serializing...
        assertEquals("{\"KEY\":\"b\"}", mapper.writeValueAsString(map));
    }
    
    public void testSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setHandlerInstantiator(new MyInstantiator("xyz:"));
        assertEquals(quote("xyz:456"), mapper.writeValueAsString(new MyBean("456")));
    }
}
