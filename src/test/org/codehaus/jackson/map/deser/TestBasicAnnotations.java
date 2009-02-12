package org.codehaus.jackson.map.deser;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tests use of basic Annotations for
 * bean deserialization; ones that indicate (non-constructor)
 * method types, explicit deserializer annotations.
 */
public class TestBasicAnnotations
    extends BaseTest
{
    /*
    //////////////////////////////////////////////
    // Annotated helper classes
    //////////////////////////////////////////////
     */

    /// Class for testing {@link JsonSetter} annotations
    final static class SizeClassSetter
    {
        int _size;
        int _length;
        int _other;

        @JsonSetter public void size(int value) { _size = value; }
        @JsonSetter("length") public void foobar(int value) { _length = value; }

        // note: need not be public if annotated
        @JsonSetter protected void other(int value) { _other = value; }
    }

    /// Class for testing {@link JsonIgnore} annotations with setters
    final static class SizeClassIgnore
    {
        int _x = 0;
        int _y = 0;

        public void setX(int value) { _x = value; }
        @JsonIgnore public void setY(int value) { _y = value; }

        /* Just igoring won't help a lot here; let's define a replacement
         * so that we won't get an exception for "unknown field"
         */
        @JsonSetter("y") void foobar(int value) {
            ; // nop
        }
    }

    /**
     * Class for testing {@link JsonDeserializer} annotation
     * for class itself.
     */
    @JsonUseDeserializer(ClassDeserializer.class)
    final static class TestDeserializerAnnotationClass {
        int _a;
        
        /* we'll test it by not having default no-arg ctor, and leaving
         * out single-int-arg ctor (because deserializer would use that too)
         */
        public TestDeserializerAnnotationClass(int a, int b) {
            _a = a;
        }
    }

    /**
     * Class for testing {@link JsonDeserializer} annotation
     * for a method
     */
    final static class TestDeserializerAnnotationMethod {
        int[] _ints;

        /* Note: could be made to work otherwise, except that
         * to trigger failure (in absence of annotation) Json
         * is of type VALUE_NUMBER_INT, not an Array: array would
         * work by default, but scalar not
         */
        @JsonUseDeserializer(IntsDeserializer.class)
        public void setInts(int[] i) {
            _ints = i;
        }
    }

    /*
    //////////////////////////////////////////////
    // Other helper classes
    //////////////////////////////////////////////
     */

    final static class ClassDeserializer extends JsonDeserializer<TestDeserializerAnnotationClass>
    {
        public TestDeserializerAnnotationClass deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            int i = jp.getIntValue();
            return new TestDeserializerAnnotationClass(i, i);
        }
    }

    final static class IntsDeserializer extends JsonDeserializer<int[]>
    {
        public int[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return new int[] { jp.getIntValue() };
        }
    }

    /*
    //////////////////////////////////////////////
    // Test methods
    //////////////////////////////////////////////
     */

    public void testSimpleSetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        SizeClassSetter result = m.readValue
            ("{ \"other\":3, \"size\" : 2, \"length\" : -999 }",
             SizeClassSetter.class);
                                             
        assertEquals(3, result._other);
        assertEquals(2, result._size);
        assertEquals(-999, result._length);
    }

    public void testSimpleIgnore() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        SizeClassIgnore result = m.readValue
            ("{ \"x\":1, \"y\" : 2 }",
             SizeClassIgnore.class);
        // x should be set, y not
        assertEquals(1, result._x);
        assertEquals(0, result._y);
    }

    /**
     * Unit test to verify that @JsonUseSerializer annotation works
     * when applied to a class
     */
    public void testClassDeserializer() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        TestDeserializerAnnotationClass result = m.readValue
            ("  123  ", TestDeserializerAnnotationClass.class);
        assertEquals(123, result._a);
    }

    /**
     * Unit test to verify that @JsonSerializer annotation works
     * when applied to a Method
     */
    public void testMethodDeserializer() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // note: since it's part of method, must parse from Object struct
        TestDeserializerAnnotationMethod result = m.readValue
            (" { \"ints\" : 3 } ", TestDeserializerAnnotationMethod.class);
        assertNotNull(result);
        int[] ints = result._ints;
        assertNotNull(ints);
        assertEquals(1, ints.length);
        assertEquals(3, ints[0]);
    }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

}
