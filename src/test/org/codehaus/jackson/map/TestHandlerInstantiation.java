package org.codehaus.jackson.map;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeFactory;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonTypeIdResolver;
import org.codehaus.jackson.type.JavaType;

public class TestHandlerInstantiation extends BaseMapTest
{
    /*
    /**********************************************************************
    /* Helper classes, beans
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

    @JsonTypeInfo(use=Id.CUSTOM, include=As.WRAPPER_ARRAY)
    @JsonTypeIdResolver(CustomIdResolver.class)
    static class TypeIdBean {
        public int x;
        
        public TypeIdBean() { }
        public TypeIdBean(int x) { this.x = x; }
    }

    static class TypeIdBeanWrapper {
        public TypeIdBean bean;
        
        public TypeIdBeanWrapper() { this(null); }
        public TypeIdBeanWrapper(TypeIdBean b) { bean = b; }
    }
    
    /*
    /**********************************************************************
    /* Helper classes, serializers/deserializers/resolvers
    /**********************************************************************
     */
    
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
        public MyKeyDeserializer() { }
        
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
    
    // copied from "TestCustomTypeIdResolver"
    static class CustomIdResolver implements TypeIdResolver
    {
        static List<JavaType> initTypes;

        final String _id;
        
        public CustomIdResolver(String idForBean) {
            _id = idForBean;
        }
        
        @Override
        public Id getMechanism() {
            return Id.CUSTOM;
        }

        @Override
        public String idFromValue(Object value)
        {
            if (value.getClass() == TypeIdBean.class) {
                return _id;
            }
            return "unknown";
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> type) {
            return idFromValue(value);
        }
        
        @Override
        public void init(JavaType baseType) {
            if (initTypes != null) {
                initTypes.add(baseType);
            }
        }

        @Override
        public JavaType typeFromId(String id)
        {
            if (id.equals(_id)) {
                return TypeFactory.defaultInstance().constructType(TypeIdBean.class);
            }
            return null;
        }
    }

    /*
    /**********************************************************************
    /* Helper classes, handler instantiator
    /**********************************************************************
     */
    
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
            if (resolverClass == CustomIdResolver.class) {
                return new CustomIdResolver("!!!");
            }
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

    public void testTypeIdResolver() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setHandlerInstantiator(new MyInstantiator("foobar"));
        String json = mapper.writeValueAsString(new TypeIdBeanWrapper(new TypeIdBean(123)));
        // should now use our custom id scheme:
        assertEquals("{\"bean\":[\"!!!\",{\"x\":123}]}", json);
        // and bring it back too:
        TypeIdBeanWrapper result = mapper.readValue(json, TypeIdBeanWrapper.class);
        TypeIdBean bean = result.bean;
        assertEquals(123, bean.x);
    }

}
