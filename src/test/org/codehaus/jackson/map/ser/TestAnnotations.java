package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * This unit test suite tests use of Annotations for
 * bean serialization.
 */
public class TestAnnotations
    extends BaseMapTest
{
    /*
    //////////////////////////////////////////////
    // Annotated helper classes
    //////////////////////////////////////////////
     */

    /// Class for testing {@link JsonProperty} annotations with getters
    final static class SizeClassGetter
    {
        @JsonProperty public int size() { return 3; }
        @JsonProperty("length") public int foobar() { return -17; }
        // note: need not be public since there's annotation
        @JsonProperty protected int value() { return 0; }

        // dummy method; not a getter signature
        protected int getNotReally(int arg) { return 0; }
    }

    // And additional testing to cover [JACKSON-64]
    final static class SizeClassGetter2
    {
        // Should still be considered property "x"
        @JsonProperty protected int getX() { return 3; }
    }

    // and some support for testing [JACKSON-120]
    final static class SizeClassGetter3
    {
        // Should be considered property "y" even tho non-public
        @JsonSerialize protected int getY() { return 8; }
    }


    /**
     * Class for testing {@link JsonSerializer} annotation
     * for class itself.
     */
    @JsonSerialize(using=BogusSerializer.class)
    final static class ClassSerializer {
    }

    /**
     * Class for testing an active {@link JsonSerialize#using} annotation
     * for a method
     */
    final static class ClassMethodSerializer {
        private int _x;

        public ClassMethodSerializer(int x) { _x = x; }

        @JsonSerialize(using=StringSerializer.class)
            public int getX() { return _x; }
    }

    /**
     * Class for testing an inactive (one that will not have any effect)
     * {@link JsonSerialize} annotation for a method
     */
    final static class InactiveClassMethodSerializer {
        private int _x;

        public InactiveClassMethodSerializer(int x) { _x = x; }

        // Basically, has no effect, hence gets serialized as number
        @JsonSerialize(using=JsonSerializer.None.class)
            public int getX() { return _x; }
    }

    /**
     * Class for verifying that getter information is inherited
     * as expected via normal class inheritance
     */
    static class BaseBean {
        public int getX() { return 1; }
        @SuppressWarnings("unused")
            @JsonProperty("y")
        private int getY() { return 2; }
    }

    static class SubClassBean extends BaseBean {
        public int getZ() { return 3; }
    }

    /*
    //////////////////////////////////////////////
    // Other helper classes
    //////////////////////////////////////////////
     */

    public final static class BogusSerializer extends JsonSerializer<Object>
    {
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBoolean(true);
        }
    }

    private final static class StringSerializer extends JsonSerializer<Object>
    {
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString("X"+value+"X");
        }

    }

    /*
    //////////////////////////////////////////////
    // Main tests
    //////////////////////////////////////////////
     */

    public void testSimpleGetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new SizeClassGetter());
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(3), result.get("size"));
        assertEquals(Integer.valueOf(-17), result.get("length"));
        assertEquals(Integer.valueOf(0), result.get("value"));
    }

    public void testSimpleGetter2() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new SizeClassGetter2());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
    }

    // testing [JACKSON-120], implied getter
    public void testSimpleGetter3() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new SizeClassGetter3());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(8), result.get("y"));
    }

    /**
     * Let's also verify that inherited super-class getters are used
     * as expected
     */
    public void testGetterInheritance() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new SubClassBean());
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
        assertEquals(Integer.valueOf(2), result.get("y"));
        assertEquals(Integer.valueOf(3), result.get("z"));
    }

    /**
     * Unit test to verify that {@JsonSerialize#using} annotation works
     * when applied to a class
     */
    public void testClassSerializer() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        StringWriter sw = new StringWriter();
        m.writeValue(sw, new ClassSerializer());
        assertEquals("true", sw.toString());
    }

    /**
     * Unit test to verify that @JsonSerializer annotation works
     * when applied to a Method
     */
    public void testActiveMethodSerializer() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        StringWriter sw = new StringWriter();
        m.writeValue(sw, new ClassMethodSerializer(13));
        /* Here we will get wrapped as an object, since we have
         * full object, just override a single property
         */
        assertEquals("{\"x\":\"X13X\"}", sw.toString());
    }

    public void testInactiveMethodSerializer() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        StringWriter sw = new StringWriter();
        m.writeValue(sw, new InactiveClassMethodSerializer(8));
        /* Here we will get wrapped as an object, since we have
         * full object, just override a single property
         */
        assertEquals("{\"x\":8}", sw.toString());
    }
}
