package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.annotate.JsonValue;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ser.ToStringSerializer;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestEnumSerialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper enums
    /**********************************************************
     */

    /**
     * Test enumeration for verifying Enum serialization functionality.
     */
    protected enum TestEnum {
        A, B, C;
        private TestEnum() { }

        @Override public String toString() { return name().toLowerCase(); }
    }

    /**
     * Alternative version that forces use of "toString-serializer".
     */
    @JsonSerialize(using=ToStringSerializer.class)
    protected enum AnnotatedTestEnum {
        A2, B2, C2;
        private AnnotatedTestEnum() { }

        @Override public String toString() { return name().toLowerCase(); }
    }

    protected enum EnumWithJsonValue {
        A("foo"), B("bar");
        private final String name;
        private EnumWithJsonValue(String n) {
            name = n;
        }
        @JsonValue
        @Override
        public String toString() { return name; }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, TestEnum.B);
        assertEquals("\"B\"", sw.toString());
    }

    public void testEnumSet() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        EnumSet<TestEnum> value = EnumSet.of(TestEnum.B);
        mapper.writeValue(sw, value);
        assertEquals("[\"B\"]", sw.toString());
    }

    /**
     * Whereas regular Enum serializer uses enum names, some users
     * prefer calling toString() instead. So let's verify that
     * this can be done using annotation for enum class.
     */
    public void testEnumUsingToString() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, AnnotatedTestEnum.C2);
        assertEquals("\"c2\"", sw.toString());
    }

    /**
     * Unit test that verifies that standard enum serialization
     * can be overridden by using custom serializer factory
     * to specify alternative global enum serializer.
     */
    public void testEnumUsingCSFEnumOverride() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();
        sf.setEnumSerializer(ToStringSerializer.instance);
        mapper.setSerializerFactory(sf);
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, TestEnum.B);
        assertEquals("\"b\"", sw.toString());
    }

    /**
     * Unit test that verifies that standard enum serialization
     * can be overridden by using custom serializer factory
     * to specify generic serializer for enum base class
     */
    @SuppressWarnings("unchecked")
    public void testEnumUsingCSFGenericMapping() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();
        Class<?> enumCls = Enum.class;
        sf.addGenericMapping((Class<Object>) enumCls, ToStringSerializer.instance);
        mapper.setSerializerFactory(sf);
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, TestEnum.A);
        assertEquals("\"a\"", sw.toString());
    }

    // Test [JACKSON-214]
    public void testSubclassedEnums() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("\"B\"", mapper.writeValueAsString(EnumWithSubClass.B));
    }

    // [JACKSON-193]
    public void testEnumsWithJsonValue() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("\"bar\"", mapper.writeValueAsString(EnumWithJsonValue.B));
    }
}
