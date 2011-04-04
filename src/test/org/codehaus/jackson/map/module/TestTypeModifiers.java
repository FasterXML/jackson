package org.codehaus.jackson.map.module;

import java.io.IOException;
import java.lang.reflect.Type;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.CollectionLikeType;
import org.codehaus.jackson.map.type.MapLikeType;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.type.TypeModifier;
import org.codehaus.jackson.type.JavaType;

public class TestTypeModifiers extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class ModifierModule extends SimpleModule
        implements Serializers
    {
        public ModifierModule() {
            super("test", Version.unknownVersion());
        }

        @Override
        public void setupModule(SetupContext context)
        {
            context.addSerializers(new Serializers() {
                @Override
                public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type,
                        BeanDescription beanDesc, BeanProperty property)
                {
                    return null;
                }
                
            });
        }

        @Override
        public JsonSerializer<?> findSerializer(SerializationConfig config,
                JavaType type, BeanDescription beanDesc, BeanProperty property) {
            return null;
        }
    }

    interface MapMarker<K,V> { }
    interface CollectionMarker<V> { }
    
    static class MyMapLikeType implements MapMarker<String,Integer> {
        public String key;
        public int value;

        public MyMapLikeType() { }
        public MyMapLikeType(String k, int v) {
            key = k;
            value = v;
        }
    }

    static class MyCollectionLikeType implements CollectionMarker<Integer>
    {
        public int value;

        public MyCollectionLikeType() { }
        public MyCollectionLikeType(int v) {
            value = v;
        }
    }

    static class MyMapSerializer extends JsonSerializer<MyMapLikeType>
    {
        @Override
        public void serialize(MyMapLikeType value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeNumberField(value.key, value.value);
            jgen.writeEndObject();
        }
    }
    static class MyMapDeserializer extends JsonDeserializer<MyMapLikeType>
    {
        @Override
        public MyMapLikeType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            if (jp.getCurrentToken() != JsonToken.START_OBJECT) throw new IOException("Wrong token: "+jp.getCurrentToken());
            if (jp.nextToken() != JsonToken.FIELD_NAME) throw new IOException("Wrong token: "+jp.getCurrentToken());
            String key = jp.getCurrentName();
            if (jp.nextToken() != JsonToken.VALUE_NUMBER_INT) throw new IOException("Wrong token: "+jp.getCurrentToken());
            int value = jp.getIntValue();
            if (jp.nextToken() != JsonToken.END_OBJECT) throw new IOException("Wrong token: "+jp.getCurrentToken());
            return new MyMapLikeType(key, value);
        }        
    }

    static class MyCollectionSerializer extends JsonSerializer<MyCollectionLikeType>
    {
        @Override
        public void serialize(MyCollectionLikeType value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartArray();
            jgen.writeNumber(value.value);
            jgen.writeEndArray();
        }
    }
    static class MyCollectionDeserializer extends JsonDeserializer<MyCollectionLikeType>
    {
        @Override
        public MyCollectionLikeType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) throw new IOException("Wrong token: "+jp.getCurrentToken());
            if (jp.nextToken() != JsonToken.VALUE_NUMBER_INT) throw new IOException("Wrong token: "+jp.getCurrentToken());
            int value = jp.getIntValue();
            if (jp.nextToken() != JsonToken.END_ARRAY) throw new IOException("Wrong token: "+jp.getCurrentToken());
            return new MyCollectionLikeType(value);
        }        
    }
    
    static class MyTypeModifier extends TypeModifier
    {
        @Override
        public JavaType modifyType(JavaType type, Type jdkType, TypeBindings context, TypeFactory typeFactory)
        {
            Class<?> raw = type.getRawClass();
            if (MapMarker.class.isAssignableFrom(raw)) {
                JavaType[] params = typeFactory.findTypeParameters(type, MapMarker.class);
                return typeFactory.constructMapLikeType(raw, params[0], params[1]);
            }
            if (CollectionMarker.class.isAssignableFrom(raw)) {
                JavaType[] params = typeFactory.findTypeParameters(type, CollectionMarker.class);
                return typeFactory.constructCollectionLikeType(raw, params[0]);
            }
            return type;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * Basic test for ensuring that we can get "xxx-like" types recognized.
     */
    public void testLikeTypeConstruction() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTypeFactory(mapper.getTypeFactory().withModifier(new MyTypeModifier()));
        JavaType type = mapper.constructType(MyMapLikeType.class);
        assertTrue(type.isMapLikeType());
        // also, must have resolved type info
        JavaType param = ((MapLikeType) type).getKeyType();
        assertNotNull(param);
        assertSame(String.class, param.getRawClass());
        param = ((MapLikeType) type).getContentType();
        assertNotNull(param);
        assertSame(Integer.class, param.getRawClass());
        
        type = mapper.constructType(MyCollectionLikeType.class);
        assertTrue(type.isCollectionLikeType());
        param = ((CollectionLikeType) type).getContentType();
        assertNotNull(param);
        assertSame(Integer.class, param.getRawClass());
    }

    public void testCollectionLikeHandling() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTypeFactory(mapper.getTypeFactory().withModifier(new MyTypeModifier()));
        assertEquals("[19]", mapper.writeValueAsString(new MyCollectionLikeType(19)));
    }

    public void testMapLikeHandling() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTypeFactory(mapper.getTypeFactory().withModifier(new MyTypeModifier()));
        assertEquals("{\"x\":3}", mapper.writeValueAsString(new MyMapLikeType("x", 3)));
    }
}
