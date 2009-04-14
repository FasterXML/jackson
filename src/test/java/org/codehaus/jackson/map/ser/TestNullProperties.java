package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for checking that the "write null properties" configuration
 * works correctly.
 */
public class TestNullProperties
    extends BaseMapTest
{
    static class SimpleBean
    {
        public String getA() { return "a"; }
        public String getB() { return null; }
    }

    @JsonWriteNullProperties(false)
    static class NoNullsBean
    {
        public String getA() { return "a"; }
        public String getB() { return null; }
    }

    static class MethodBean
    {
        @JsonWriteNullProperties(false)
        public String getB() { return null; }
    }

    public void testGlobal() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.getSerializationConfig().isEnabled(SerializationConfig.Feature.WRITE_NULL_PROPERTIES));
        Map<String,Object> result = writeAndMap(m, new SimpleBean());
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertNull(result.get("b"));
        assertTrue(result.containsKey("b"));

        /* Important: MUST create a new ObjectMapper, will otherwise
         * cache old serializer
         */
        m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false);
        result = writeAndMap(m, new SimpleBean());
        assertEquals(1, result.size());
        assertEquals("a", result.get("a"));
        assertNull(result.get("b"));
        assertFalse(result.containsKey("b"));
    }

    public void testByClass() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.getSerializationConfig().isEnabled(SerializationConfig.Feature.WRITE_NULL_PROPERTIES));
        Map<String,Object> result = writeAndMap(m, new NoNullsBean());
        assertEquals(1, result.size());
        assertEquals("a", result.get("a"));
        assertNull(result.get("b"));
        assertFalse(result.containsKey("b"));
    }

    public void testByMethod() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new MethodBean());
        assertEquals(0, result.size());
    }
}
