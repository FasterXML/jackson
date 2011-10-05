package org.codehaus.jackson.map.ser;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ser.std.ToStringSerializer;

/**
 * This unit test suite tests functioning of {@link JsonValue}
 * annotation with bean serialization.
 */
public class TestAnnotationJsonValue
    extends BaseMapTest
{
    /*
    /*********************************************************
    /* Helper bean classes
    /*********************************************************
     */

    static class ValueClass<T>
    {
        final T _value;

        public ValueClass(T v) { _value = v; }

        @JsonValue T value() { return _value; }

        // shouldn't need this, but may be useful for troubleshooting:
        @Override
        public String toString() { return "???"; }
    }

    /**
     * Another test class to check that it is also possible to
     * force specific serializer to use with @JsonValue annotated
     * method. Difference is between Integer serialization, and
     * conversion to a Json String.
     */
    final static class ToStringValueClass<T>
        extends ValueClass<T>
    {
        public ToStringValueClass(T value) { super(value); }

        // Also, need to use this annotation to help
        @JsonSerialize(using=ToStringSerializer.class)
        @Override
        @JsonValue T value() { return super.value(); }
    }

    final static class ToStringValueClass2
        extends ValueClass<String>
    {
        public ToStringValueClass2(String value) { super(value); }

        /* Simple as well, but let's ensure that other getters won't matter...
         */

        @JsonProperty int getFoobar() { return 4; }

        public String[] getSomethingElse() { return new String[] { "1", "a" }; }
    }

    static class ValueBase {
        public String a = "a";
    }

    static class ValueType extends ValueBase {
        public String b = "b";
    }
    
    // Finally, let's also test static vs dynamic type
    static class ValueWrapper {
        @JsonValue
        public ValueBase getX() { return new ValueType(); }
    }

    static class MapBean
    {
        @JsonValue
        public Map<String,String> toMap()
        {
            HashMap<String,String> map = new HashMap<String,String>();
            map.put("a", "1");
            return map;
        }
    }
    
    /*
    /*********************************************************
    /* Test cases
    /*********************************************************
     */

    public void testSimpleJsonValue() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String result = serializeAsString(m, new ValueClass<String>("abc"));
        assertEquals("\"abc\"", result);
    }

    public void testJsonValueWithUseSerializer() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String result = serializeAsString(m, new ToStringValueClass<Integer>(Integer.valueOf(123)));
        assertEquals("\"123\"", result);
    }

    /**
     * Test for verifying that additional getters won't confuse serializer.
     */
    public void testMixedJsonValue() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String result = serializeAsString(m, new ToStringValueClass2("xyz"));
        assertEquals("\"xyz\"", result);
    }

    public void testValueWithStaticType() throws Exception
    {
        // Ok; first, with dynamic type:
        ObjectMapper m = new ObjectMapper();
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", serializeAsString(m, new ValueWrapper()));

        // then static
        m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
        assertEquals("{\"a\":\"a\"}", serializeAsString(m, new ValueWrapper()));
    }

    public void testMapWithJsonValue() throws Exception
    {
        assertEquals("{\"a\":\"1\"}", new ObjectMapper().writeValueAsString(new MapBean()));
    }
}
