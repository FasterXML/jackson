package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

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
    @SuppressWarnings("serial")
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

    // [JACKSON-480]
    static class SimpleKey {
        protected final String key;
        
        public SimpleKey(String str) { key = str; }
        
        @Override public String toString() { return "simple "+key; }
    }

    static class ActualKey extends SimpleKey
    {
        public ActualKey(String str) { super(str); }
        
        @Override public String toString() { return "actual "+key; }
    }

    static class MapWrapper
    {
        @JsonSerialize(keyAs=SimpleKey.class)
        public final HashMap<ActualKey, String> values = new HashMap<ActualKey, String>();
        
        public MapWrapper(String key, String value) {
            values.put(new ActualKey(key), value);
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    /**
     * Test [JACKSON-220]
     */
    public void testMapSerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals("\"{a=b, c=d}\"", m.writeValueAsString(new PseudoMap("a", "b", "c", "d")));
    }

    /**
     * Test [JACKSON-314]
     */
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

    // [JACKSON-480], custom annotations for key, content values
    public void testSerializedAsAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        MapWrapper input = new MapWrapper("a", "b");
        assertEquals("{\"values\":{\"simple a\":\"b\"}}", m.writeValueAsString(input));
    }

    // [JACKSON-480], custom handlers for key, content values
    
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
    }
}
