package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tests functioning of {@link JsonValue}
 * annotation with bean serialization.
 */
public class TestAnnotationJsonValue
    extends BaseMapTest
{
    /*
    //////////////////////////////////////////////
    // Helper bean classes
    //////////////////////////////////////////////
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

        @JsonUseSerializer(ToStringSerializer.class)
            @JsonValue T value() { return super.value(); }
    }

    /*
    //////////////////////////////////////////////
    // Test cases
    //////////////////////////////////////////////
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
}
