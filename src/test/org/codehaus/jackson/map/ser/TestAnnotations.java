package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tests use of Annotations for
 * bean serialization.
 */
public class TestAnnotations
    extends BaseTest
{
    /*
    //////////////////////////////////////////////
    // Annotated helper classes
    //////////////////////////////////////////////
     */

    /// Class for testing {@link JsonGetter} annotations
    final static class SizeClassGetter
    {
        @JsonGetter public int size() { return 3; }
        @JsonGetter("length") public int foobar() { return -17; }
        // note: need not be public since there's annotation
        @JsonGetter protected int value() { return 0; }
    }

    /// Class for testing enabled {@link JsonIgnore} annotation
    final static class SizeClassEnabledIgnore
    {
        // note: must be public to be seen
        public int getX() { return 1; }
        @JsonIgnore public int getY() { return 9; }
    }

    /// Class for testing enabled {@link JsonIgnore} annotation
    final static class SizeClassDisabledIgnore
    {
        // note: must be public to be seen
        public int getX() { return 3; }
        @JsonIgnore(false) public int getY() { return 4; }
    }

    /**
     * Class for testing {@link JsonSerializer} annotation
     * for class itself.
     */
    @JsonUseSerializer(BogusSerializer.class)
    final static class ClassSerializer {
    }

    /**
     * Class for testing an active {@link JsonUseSerializer} annotation
     * for a method
     */
    final static class ClassMethodSerializer {
        private int _x;

        public ClassMethodSerializer(int x) { _x = x; }

        @JsonUseSerializer(StringSerializer.class)
            public int getX() { return _x; }
    }

    /**
     * Class for testing an inactive (one that will not have any effect)
     * {@link JsonUseSerializer} annotation for a method
     */
    final static class InactiveClassMethodSerializer {
        private int _x;

        public InactiveClassMethodSerializer(int x) { _x = x; }

        // Basically, has no effect, hence gets serialized as number
        @JsonUseSerializer(NoClass.class)
            public int getX() { return _x; }
    }

    /**
     * Class for verifying that broken class-attached
     * {@link JsonUseSerializer} annotation is handled properly
     * (by throwing {@link JsonMappingException}).
     */
    @JsonUseSerializer(String.class)
    // wrong: String is not a JsonSerializer
    final static class BrokenUseSerClassAnnotation {
        public int getX() { return 1; }
    }

    /**
     * Class for verifying that broken class-attached
     * {@link JsonUseSerializer} annotation is handled properly
     * (by throwing {@link JsonMappingException}).
     */
    final static class BrokenUseSerMethodAnnotation {
        // wrong: Integer is not a JsonSerializer
        @JsonUseSerializer(Integer.class)
        public int getX() { return 2; }
    }

    /**
     * Class for verifying that getter information is inherited
     * as expected via normal class inheritance
     */
    class BaseBean {
        public int getX() { return 1; }
        @SuppressWarnings("unused")
		@JsonGetter("y")
        private int getY() { return 2; }
    }

    class SubClassBean extends BaseBean {
        public int getZ() { return 3; }
    }

    /*
    //////////////////////////////////////////////
    // Other helper classes
    //////////////////////////////////////////////
     */

    final static class BogusSerializer extends JsonSerializer<Object>
    {
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBoolean(true);
        }

    }

    final static class StringSerializer extends JsonSerializer<Object>
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

    public void testSimpleIgnore() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // Should see "x", not "y"
        Map<String,Object> result = writeAndMap(m, new SizeClassEnabledIgnore());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
        assertNull(result.get("y"));
    }

    public void testDisabledIgnore() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // Should see "x" and "y"
        Map<String,Object> result = writeAndMap(m, new SizeClassDisabledIgnore());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
        assertEquals(Integer.valueOf(4), result.get("y"));
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
     * Unit test to verify that @JsonUseSerializer annotation works
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

    /**
     * Unit test that verifies that a broken {@JsonUseSerializer}
     * annotation is properly handled
     */
    public void testBrokenSerializerByClass() throws Exception
    {
        try {
            new ObjectMapper().writeValue(new StringWriter(), new BrokenUseSerClassAnnotation());
            fail("Expected an exception for invalid @JsonUseSerializer annotation");
        } catch (JsonMappingException jex) {
            ; // good
        } catch (Exception jex) {
            fail("Expected an exception of type JsonMappingException, got ("+jex.getClass()+": "+jex);
        }
    }

    public void testBrokenSerializerByMethod() throws Exception
    {
        try {
            new ObjectMapper().writeValue(new StringWriter(), new BrokenUseSerMethodAnnotation());
            fail("Expected an exception for invalid @JsonUseSerializer annotation");
        } catch (JsonMappingException jex) {
            ; // good
        } catch (Exception jex) {
            fail("Expected an exception of type JsonMappingException, got ("+jex.getClass()+": "+jex);
        }
    }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
	private Map<String,Object> writeAndMap(ObjectMapper m, Object value)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        m.writeValue(sw, value);
        return (Map<String,Object>) m.readValue(sw.toString(), Object.class);
    }
}
