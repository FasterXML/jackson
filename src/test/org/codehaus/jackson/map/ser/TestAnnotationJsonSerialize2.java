package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@SuppressWarnings("serial")
public class TestAnnotationJsonSerialize2
    extends BaseMapTest
{
    // [JACKSON-480]

    static class SimpleKey {
        protected final String key;
        
        public SimpleKey(String str) { key = str; }
        
        @Override public String toString() { return "toString:"+key; }
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
    static class SimpleValueMapWithSerializer extends HashMap<SimpleKey, ActualValue> { }
    
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
    
    // [JACKSON-480], test value annotation applied to List value class
    public void testSerializedAsListWithClassAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        SimpleValueList list = new SimpleValueList();
        list.add(new ActualValue("foo"));
        assertEquals("[{\"value\":\"foo\"}]", m.writeValueAsString(list));
    }

    // [JACKSON-480], test value annotation applied to Map value class
    public void testSerializedAsMapWithClassAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        SimpleValueMap map = new SimpleValueMap();
        map.put(new SimpleKey("x"), new ActualValue("y"));
        assertEquals("{\"toString:x\":{\"value\":\"y\"}}", m.writeValueAsString(map));
    }

    // [JACKSON-480], test Serialization annotation with List
    public void testSerializedAsListWithClassSerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        SimpleValueListWithSerializer list = new SimpleValueListWithSerializer();
        list.add(new ActualValue("foo"));
        assertEquals("[\"value foo\"]", m.writeValueAsString(list));
    }

    // [JACKSON-480], test Serialization annotation with Map
    public void testSerializedAsMapWithClassSerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        SimpleValueMapWithSerializer map = new SimpleValueMapWithSerializer();
        map.put(new SimpleKey("abc"), new ActualValue("123"));
        assertEquals("{\"key abc\":\"value 123\"}", m.writeValueAsString(map));
    }
    
    // [JACKSON-480], test annotations when applied to List property (getter, setter)
    public void testSerializedAsListWithPropertyAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        ListWrapper input = new ListWrapper("bar");
        assertEquals("{\"values\":[{\"value\":\"bar\"}]}", m.writeValueAsString(input));
    }

    // [JACKSON-480], test annotations when applied to Map property (getter, setter)
    public void testSerializedAsMapWithPropertyAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        MapWrapper input = new MapWrapper("a", "b");
        assertEquals("{\"values\":{\"toString:a\":{\"value\":\"b\"}}}", m.writeValueAsString(input));
    }
}
