package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
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

    // [JACKSON-480]

    static class SimpleKey {
        protected final String key;
        
        public SimpleKey(String str) { key = str; }
        
        @Override public String toString() { return "key "+key; }
    }

    static class SimpleValue {
        public final String value;
        
        public SimpleValue(String str) { value = str; }
    }

    @JsonPropertyOrder({"value", "value2"})
    static class ActualValue extends SimpleValue
    {
        public final String other = "123";
        
        public ActualValue(String str) { super(str); }
    }

    static class SimpleKeySerializer extends JsonSerializer<SimpleKey> {
        @Override
        public void serialize(SimpleKey key, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
            jgen.writeFieldName("key "+key.key);
        }
    }

    static class SimpleValueSerializer extends JsonSerializer<SimpleValue> {
        @Override
        public void serialize(SimpleValue value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
            jgen.writeString("value "+value.value);
        }
    }

    @JsonSerialize(contentAs=SimpleValue.class)
    static class SimpleValueList extends ArrayList<ActualValue> { }

    @JsonSerialize(contentAs=SimpleValue.class)
    static class SimpleValueMap extends HashMap<SimpleKey, ActualValue> { }

    @JsonSerialize(contentUsing=SimpleValueSerializer.class)
    static class SimpleValueListWithSerializer extends ArrayList<ActualValue> { }

    @JsonSerialize(keyUsing=SimpleKeySerializer.class, contentUsing=SimpleValueSerializer.class)
    static class SimpleValueMapWithSerializer extends ArrayList<ActualValue> { }
    
    static class ListWrapper
    {
        @JsonSerialize(contentAs=SimpleValue.class)
        public final ArrayList<ActualValue> values = new ArrayList<ActualValue>();
        
        public ListWrapper(String value) {
            values.add(new ActualValue(value));
        }
    }
    
    static class MapWrapper
    {
        @JsonSerialize(contentAs=SimpleValue.class)
        public final HashMap<SimpleKey, ActualValue> values = new HashMap<SimpleKey, ActualValue>();
        
        public MapWrapper(String key, String value) {
            values.put(new SimpleKey(key), new ActualValue(value));
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
    }

    // [JACKSON-480], test annotations when applied to List value class
    public void testSerializedAsListWithClassAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        SimpleValueList list = new SimpleValueList();
        list.add(new ActualValue("foo"));
        assertEquals("[{\"value\":\"foo\"}]", m.writeValueAsString(list));
    }

    // [JACKSON-480], test annotations when applied to Map value class
    public void testSerializedAsMapWithClassAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        SimpleValueMap map = new SimpleValueMap();
        map.put(new SimpleKey("x"), new ActualValue("y"));
        assertEquals("{\"key x\":{\"value\":\"y\"}}", m.writeValueAsString(map));
    }

    // [JACKSON-480], test annotations when applied to List value class
    public void testSerializedAsListWithClassSerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        SimpleValueListWithSerializer list = new SimpleValueListWithSerializer();
        list.add(new ActualValue("foo"));
        assertEquals("[\"value foo\"]", m.writeValueAsString(list));
    }
    
    
    /*
    // [JACKSON-480], test annotations when applied to List property (getter, setter)
    public void testSerializedAsListWithPropertyAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        ListWrapper input = new ListWrapper("bar");
        assertEquals("{\"values\":[{\"value\":\"bar\"}}]", m.writeValueAsString(input));
    }

    // [JACKSON-480], test annotations when applied to Map property (getter, setter)
    public void testSerializedAsMapWithPropertyAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        MapWrapper input = new MapWrapper("a", "b");
        assertEquals("{\"values\":{\"key a\":{\"value b\"}}}", m.writeValueAsString(input));
    }
*/    
    // [JACKSON-480], custom handlers for key, content values
    
}
