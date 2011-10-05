package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.jsontype.impl.TypeNameIdResolver;
import org.codehaus.jackson.map.type.TypeFactory;

public class TestDefaultForMaps 
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    static class MapKey {
        public String key;

        public MapKey(String k) { key = k; }

        @Override public String toString() { return key; }
    }

    static class MapKeyDeserializer extends KeyDeserializer
    {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new MapKey(key);
        }
    }
    
    static class MapHolder
    {
        @JsonDeserialize(keyAs=MapKey.class, keyUsing=MapKeyDeserializer.class)
        public Map<MapKey,List<Object>> map;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testJackson428() throws Exception
    {
        ObjectMapper serMapper = new ObjectMapper();

        TypeResolverBuilder<?> serializerTyper = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL);
        serializerTyper = serializerTyper.init(JsonTypeInfo.Id.NAME, createTypeNameIdResolver(true));
        serializerTyper = serializerTyper.inclusion(JsonTypeInfo.As.PROPERTY);
        serMapper.setDefaultTyping(serializerTyper);

        // Let's start by constructing something to serialize first
        MapHolder holder = new MapHolder();
        holder.map = new HashMap<MapKey,List<Object>>();
        List<Object> ints = new ArrayList<Object>();
        ints.add(Integer.valueOf(3));
        holder.map.put(new MapKey("key"), ints);
        String json = serMapper.writeValueAsString(holder);

        // Then deserialize: need separate mapper to initialize type id resolver appropriately
        ObjectMapper deserMapper = new ObjectMapper();
        TypeResolverBuilder<?> deserializerTyper = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL);
        deserializerTyper = deserializerTyper.init(JsonTypeInfo.Id.NAME, createTypeNameIdResolver(false));
        deserializerTyper = deserializerTyper.inclusion(JsonTypeInfo.As.PROPERTY);
        deserMapper.setDefaultTyping(deserializerTyper);

        MapHolder result = deserMapper.readValue(json, MapHolder.class);
        assertNotNull(result);
        Map<?,?> map = result.map;
        assertEquals(1, map.size());
        Map.Entry<?,?> entry = map.entrySet().iterator().next();
        Object key = entry.getKey();
        assertEquals(MapKey.class, key.getClass());
        Object value = entry.getValue();
        assertTrue(value instanceof List<?>);
        List<?> list = (List<?>) value;
        assertEquals(1, list.size());
        assertEquals(Integer.class, list.get(0).getClass());
        assertEquals(Integer.valueOf(3), list.get(0));
    }

    protected TypeNameIdResolver createTypeNameIdResolver(boolean forSerialization)
    {
        Collection<NamedType> subtypes = new ArrayList<NamedType>();
        subtypes.add(new NamedType(MapHolder.class, "mapHolder"));
        subtypes.add(new NamedType(ArrayList.class, "AList"));
        subtypes.add(new NamedType(HashMap.class, "HMap"));
        ObjectMapper mapper = new ObjectMapper();
        return TypeNameIdResolver.construct(mapper.getDeserializationConfig(),
                TypeFactory.defaultInstance().constructType(Object.class), subtypes, forSerialization, !forSerialization);
    }
}
