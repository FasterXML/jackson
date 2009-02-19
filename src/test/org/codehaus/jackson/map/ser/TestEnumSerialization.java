package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;

import org.codehaus.jackson.annotate.JsonUseSerializer;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestEnumSerialization
    extends BaseTest
{
    /*
    ///////////////////////////////////////////////////
    // Helper enums
    ///////////////////////////////////////////////////
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
    @JsonUseSerializer(StdSerializerFactory.StringLikeSerializer.class)
    protected enum TestEnum2 {
        A2, B2, C2;
        private TestEnum2() { }

        @Override public String toString() { return name().toLowerCase(); }
    }

    /*
    ///////////////////////////////////////////////////
    // Tests
    ///////////////////////////////////////////////////
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, TestEnum.B);
        assertEquals("\"B\"", sw.toString());
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
        mapper.writeValue(sw, TestEnum2.C2);
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
        sf.setEnumSerializer(StdSerializerFactory.StringLikeSerializer.instance);
        mapper.setSerializerFactory(sf);
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, TestEnum2.B2);
        assertEquals("\"b2\"", sw.toString());
    }

    /**
     * Unit test that verifies that standard enum serialization
     * can be overridden by using custom serializer factory
     * to specify generic serializer for enum base class
     */
    public void testEnumUsingCSFGenericMapping() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();
        Class<?> enumCls = Enum.class;
        sf.addGenericMapping((Class<Object>) enumCls, StdSerializerFactory.StringLikeSerializer.instance);
        mapper.setSerializerFactory(sf);
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, TestEnum2.A2);
        assertEquals("\"a2\"", sw.toString());
    }
}
