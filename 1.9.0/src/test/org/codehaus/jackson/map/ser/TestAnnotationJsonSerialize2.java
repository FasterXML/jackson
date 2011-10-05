package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.*;
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
    
    static class ListWrapperSimple
    {
        @JsonSerialize(contentAs=SimpleValue.class)
        public final ArrayList<ActualValue> values = new ArrayList<ActualValue>();
        
        public ListWrapperSimple(String value) {
            values.add(new ActualValue(value));
        }
    }

    static class ListWrapperWithSerializer
    {
        @JsonSerialize(contentUsing=SimpleValueSerializer.class)
        public final ArrayList<ActualValue> values = new ArrayList<ActualValue>();
        
        public ListWrapperWithSerializer(String value) {
            values.add(new ActualValue(value));
        }
    }
    
    static class MapWrapperSimple
    {
        @JsonSerialize(contentAs=SimpleValue.class)
        public final HashMap<SimpleKey, ActualValue> values = new HashMap<SimpleKey, ActualValue>();
        
        public MapWrapperSimple(String key, String value) {
            values.put(new SimpleKey(key), new ActualValue(value));
        }
    }

    static class MapWrapperWithSerializer
    {
        @JsonSerialize(keyUsing=SimpleKeySerializer.class, contentUsing=SimpleValueSerializer.class)
        public final HashMap<SimpleKey, ActualValue> values = new HashMap<SimpleKey, ActualValue>();
        
        public MapWrapperWithSerializer(String key, String value) {
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
        ListWrapperSimple input = new ListWrapperSimple("bar");
        assertEquals("{\"values\":[{\"value\":\"bar\"}]}", m.writeValueAsString(input));
    }

    public void testSerializedAsListWithPropertyAnnotations2() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        ListWrapperWithSerializer input = new ListWrapperWithSerializer("abc");
        assertEquals("{\"values\":[\"value abc\"]}", m.writeValueAsString(input));
    }
    
    // [JACKSON-480], test annotations when applied to Map property (getter, setter)
    public void testSerializedAsMapWithPropertyAnnotations() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        MapWrapperSimple input = new MapWrapperSimple("a", "b");
        assertEquals("{\"values\":{\"toString:a\":{\"value\":\"b\"}}}", m.writeValueAsString(input));
    }

    public void testSerializedAsMapWithPropertyAnnotations2() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        MapWrapperWithSerializer input = new MapWrapperWithSerializer("foo", "b");
        assertEquals("{\"values\":{\"key foo\":\"value b\"}}", m.writeValueAsString(input));
    }

    // [JACKSON-602]: Include.NON_EMPTY
    public void testEmptyInclusion() throws IOException
    {
        ObjectMapper defMapper = new ObjectMapper();
        ObjectMapper inclMapper = new ObjectMapper().setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);

        StringWrapper str = new StringWrapper("");
        assertEquals("{\"str\":\"\"}", defMapper.writeValueAsString(str));
        assertEquals("{}", inclMapper.writeValueAsString(str));
        assertEquals("{}", inclMapper.writeValueAsString(new StringWrapper()));

        ListWrapper<String> list = new ListWrapper<String>();
        assertEquals("{\"list\":[]}", defMapper.writeValueAsString(list));
        assertEquals("{}", inclMapper.writeValueAsString(list));
        assertEquals("{}", inclMapper.writeValueAsString(new ListWrapper<String>()));

        MapWrapper<String,Integer> map = new MapWrapper<String,Integer>(new HashMap<String,Integer>());
        assertEquals("{\"map\":{}}", defMapper.writeValueAsString(map));
        assertEquals("{}", inclMapper.writeValueAsString(map));
        assertEquals("{}", inclMapper.writeValueAsString(new MapWrapper<String,Integer>(null)));

        ArrayWrapper<Integer> array = new ArrayWrapper<Integer>(new Integer[0]);
        assertEquals("{\"array\":[]}", defMapper.writeValueAsString(array));
        assertEquals("{}", inclMapper.writeValueAsString(array));
        assertEquals("{}", inclMapper.writeValueAsString(new ArrayWrapper<Integer>(null)));
    }
}
