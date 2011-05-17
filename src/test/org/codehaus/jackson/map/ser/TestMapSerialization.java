package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@SuppressWarnings("serial")
public class TestMapSerialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Class needed for testing [JACKSON-220]
     */
    @JsonSerialize(using=MapSerializer.class)    
    static class PseudoMap extends LinkedHashMap<String,String>
    {
        public PseudoMap(String... values) {
            for (int i = 0, len = values.length; i < len; i += 2) {
                put(values[i], values[i+1]);
            }
        }
    }

    static class MapSerializer extends JsonSerializer<Map<String,String>>
    {
        @Override
        public void serialize(Map<String,String> value,
                              JsonGenerator jgen,
                              SerializerProvider provider)
            throws IOException
        {
            // just use standard Map.toString(), output as JSON String
            jgen.writeString(value.toString());
        }
    }

    // For [JACKSON-574]
    static class DefaultKeySerializer extends JsonSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            jgen.writeFieldName("DEFAULT:"+value);
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // Test [JACKSON-220]
    public void testMapSerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals("\"{a=b, c=d}\"", m.writeValueAsString(new PseudoMap("a", "b", "c", "d")));
    }

    // Test [JACKSON-314]
    public void testMapNullSerialization() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,String> map = new HashMap<String,String>();
        map.put("a", null);
        // by default, should output null-valued entries:
        assertEquals("{\"a\":null}", m.writeValueAsString(map));
        // but not if explicitly asked not to (note: config value is dynamic here)
        m.configure(SerializationConfig.Feature.WRITE_NULL_MAP_VALUES, false);
        assertEquals("{}", m.writeValueAsString(map));
    }

    // [JACKSON-499], problems with map entries, values
    public void testMapKeyValueSerialization() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,String> map = new HashMap<String,String>();
        map.put("a", "b");
        assertEquals("[\"a\"]", m.writeValueAsString(map.keySet()));
        assertEquals("[\"b\"]", m.writeValueAsString(map.values()));

        // TreeMap has similar inner class(es):
        map = new TreeMap<String,String>();
        map.put("c", "d");
        assertEquals("[\"c\"]", m.writeValueAsString(map.keySet()));
        assertEquals("[\"d\"]", m.writeValueAsString(map.values()));

        // and for [JACKSON-533], same for concurrent maps
        map = new ConcurrentHashMap<String,String>();
        map.put("e", "f");
        assertEquals("[\"e\"]", m.writeValueAsString(map.keySet()));
        assertEquals("[\"f\"]", m.writeValueAsString(map.values()));
    }

    // For [JACKSON-574]
    public void testDefaultKeySerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        m.getSerializerProvider().setDefaultKeySerializer(new DefaultKeySerializer());
        Map<String,String> map = new HashMap<String,String>();
        map.put("a", "b");
        assertEquals("{\"DEFAULT:a\":\"b\"}", m.writeValueAsString(map));
    }
}
