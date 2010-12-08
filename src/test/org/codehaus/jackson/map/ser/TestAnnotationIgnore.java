package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tests use of {@link JsonIgnore} annotations
 * with  bean serialization; as well as (since 1.7)
 * {@link JsonIgnoreType}.
 */
public class TestAnnotationIgnore
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    /// Class for testing enabled {@link JsonIgnore} annotation
    final static class SizeClassEnabledIgnore
    {
        @JsonIgnore public int getY() { return 9; }

        // note: must be public to be seen
        public int getX() { return 1; }

        @JsonIgnore public int getY2() { return 1; }
        @JsonIgnore public int getY3() { return 2; }
    }

    /// Class for testing disabled {@link JsonIgnore} annotation
    final static class SizeClassDisabledIgnore
    {
        // note: must be public to be seen
        public int getX() { return 3; }
        @JsonIgnore(false) public int getY() { return 4; }
    }

    static class BaseClassIgnore
    {
        @JsonProperty("x")
        @JsonIgnore
        public int x() { return 1; }

        public int getY() { return 2; }
    }

    static class SubClassNonIgnore
        extends BaseClassIgnore
    {
        /* Annotations to disable ignorance, in sub-class; note that
         * we must still get "JsonProperty" fro super class
         */
        @Override
        @JsonIgnore(false)
        public int x() { return 3; }
    }

    @JsonIgnoreType
    static class IgnoredType { }

    @JsonIgnoreType(false)
    static class NonIgnoredType
    {
        public int value = 13;
        
        public IgnoredType ignored = new IgnoredType();
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

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
     * Test case to verify that ignore tag can also be disabled
     * via inheritance
     */
    public void testIgnoreOver() throws Exception
    {
        ObjectMapper m = new ObjectMapper();

        // should only see "y"
        Map<String,Object> result = writeAndMap(m, new BaseClassIgnore());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(2), result.get("y"));

        // Should see "x" and "y"
        result = writeAndMap(m, new SubClassNonIgnore());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
        assertEquals(Integer.valueOf(2), result.get("y"));
    }

    /**
     * @since 1.7
     */
    public void testIgnoreType() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals("{\"value\":13}", m.writeValueAsString(new NonIgnoredType()));
    }
}
