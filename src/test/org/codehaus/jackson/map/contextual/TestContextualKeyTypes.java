package org.codehaus.jackson.map.contextual;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.type.TypeFactory;

/**
 * Tests to ensure that we can do contextual key serializers and
 * deserializers as well as value ser/deser.
 * 
 * @since 1.8
 */
public class TestContextualKeyTypes extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class ContextualKeySerializer
        extends JsonSerializer<String>
        implements ContextualSerializer<String>
    {
        protected final String _prefix;
    
        public ContextualKeySerializer() { this(""); }
        public ContextualKeySerializer(String p) {
            _prefix = p;
        }

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            if (_prefix != null) {
                value = _prefix + value;
            }
            jgen.writeFieldName(value);
        }
    
        @Override
        public JsonSerializer<String> createContextual(SerializationConfig config, BeanProperty property)
            throws JsonMappingException
        {
            return new ContextualKeySerializer(_prefix+":");
        }
    }

    static class ContextualDeser
        extends KeyDeserializer
        implements ContextualKeyDeserializer
    {
        protected final String _prefix;
        
        protected ContextualDeser(String p) {
            _prefix = p;
        }        

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            return _prefix + ":" + key;
        }

        @Override
        public KeyDeserializer createContextual(DeserializationConfig config,
                BeanProperty property) throws JsonMappingException
        {
            return new ContextualDeser((property == null) ? "ROOT" : property.getName());
        }
    }

    static class MapBean {
        public Map<String, Integer> map;
    }
    
    /*
    /**********************************************************
    /* Unit tests, serialization
    /**********************************************************
     */

    public void testSimpleKeySer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addKeySerializer(String.class, new ContextualKeySerializer("prefix"));
        mapper.registerModule(module);
        Map<String,Object> input = new HashMap<String,Object>();
        input.put("a", Integer.valueOf(3));
        String json = mapper.writerWithType(TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, Object.class))
            .writeValueAsString(input);
        assertEquals("{\"prefix:a\":3}", json);
    }
    
    /*
    /**********************************************************
    /* Unit tests, deserialization
    /**********************************************************
     */

    public void testSimpleKeyDeser() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addKeyDeserializer(String.class, new ContextualDeser("???"));
        mapper.registerModule(module);
        MapBean result = mapper.readValue("{\"map\":{\"a\":3}}", MapBean.class);
        Map<String,Integer> map = result.map;
        assertNotNull(map);
        assertEquals(1, map.size());
        Map.Entry<String,Integer> entry = map.entrySet().iterator().next();
        assertEquals(Integer.valueOf(3), entry.getValue());
        assertEquals("map:a", entry.getKey());
    }
}
