package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

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
        public Map<MapKey,List<Integer>> map;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    // !!!! Incomplete: does not reproduce issue, yet
    public void testJackson428() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        /*
        TypeResolverBuilder<?> serializerTyper = new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL);
        serializerTyper = serializerTyper.init(JsonTypeInfo.Id.NAME, createTypeNameIdResolver(true));
        serializerTyper = serializerTyper.inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(serializerTyper);

        MapHolder result = mapper.readValue("{\"map\":{}}", MapHolder.class);
        */
    }
}
