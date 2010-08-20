package org.codehaus.jackson.map;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonCreator;

import main.BaseTest;

public abstract class BaseMapTest
    extends BaseTest
{
    private final static Object SINGLETON_OBJECT = new Object();

    /*
    /**********************************************************
    /* Shared helper classes
    /**********************************************************
     */

    /**
     * Simple wrapper around boolean types, usually to test value
     * conversions or wrapping
     */
    protected static class BooleanWrapper {
        public final Boolean b;
        @JsonCreator BooleanWrapper(Boolean value) { b = value; }
    }

    /**
     * Simple wrapper around String type, usually to test value
     * conversions or wrapping
     */
    protected static class StringWrapper {
        public final String str;
        @JsonCreator StringWrapper(String value) {
            str = value;
        }
    }


    protected static class ObjectWrapper {
        private final Object object;
        ObjectWrapper(final Object object) {
            this.object = object;
        }
        public Object getObject() { return object; }
        @JsonCreator
            static ObjectWrapper jsonValue(final Object object) {
            return new ObjectWrapper(object);
        }
    }

    /**
     * Enumeration type with sub-classes per value.
     */
    protected enum EnumWithSubClass {
        A { public void foobar() { } }
        ,B { public void foobar() { } }
        ;

        public abstract void foobar();
    }

    protected BaseMapTest() { super(); }

    /*
    /**********************************************************
    /* Additional assert methods
    /**********************************************************
     */

    protected void assertEquals(int[] exp, int[] act)
    {
        assertArrayEquals(exp, act);
    }

    /**
     * Helper method for verifying 3 basic cookie cutter cases;
     * identity comparison (true), and against null (false),
     * or object of different type (false)
     */
    protected void assertStandardEquals(Object o)
    {
        assertTrue(o.equals(o));
        assertFalse(o.equals(null));
        assertFalse(o.equals(SINGLETON_OBJECT));
        // just for fun, let's also call hash code...
        o.hashCode();
    }

    protected void assertNodeNumbersForNonNumeric(JsonNode n)
    { 
        assertEquals(0, n.getValueAsInt());
        assertEquals(-42, n.getValueAsInt(-42));
        assertEquals(0, n.getValueAsLong());
        assertEquals(12345678901L, n.getValueAsLong(12345678901L));
        assertEquals(0.0, n.getValueAsDouble());
        assertEquals(-19.25, n.getValueAsDouble(-19.25));
    }
    
    protected void assertNodeNumbers(JsonNode n, int expInt, double expDouble)
    {
        assertEquals(expInt, n.getValueAsInt());
        assertEquals(expInt, n.getValueAsInt(-42));
        assertEquals((long) expInt, n.getValueAsLong());
        assertEquals((long) expInt, n.getValueAsLong(19L));
        assertEquals(expDouble, n.getValueAsDouble());
        assertEquals(expDouble, n.getValueAsDouble(-19.25));
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    protected Map<String,Object> writeAndMap(ObjectMapper m, Object value)
        throws IOException
    {
        String str = m.writeValueAsString(value);
        return (Map<String,Object>) m.readValue(str, Map.class);
    }

    protected Map<String,Object> writeAndMap(Object value)
        throws IOException
    {
        return writeAndMap(new ObjectMapper(), value);
    }

    protected <T> T readAndMapFromString(ObjectMapper m, String input, Class<T> cls)
        throws IOException
    {
        return (T) m.readValue("\""+input+"\"", cls);
    }

    protected String serializeAsString(ObjectMapper m, Object value)
        throws IOException
    {
        return m.writeValueAsString(value);
    }

    protected String serializeAsString(Object value)
        throws IOException
    {
        return serializeAsString(new ObjectMapper(), value);
    }

    protected String asJSONObjectValueString(Object... args)
        throws IOException
    {
        return asJSONObjectValueString(new ObjectMapper(), args);
    }

    protected String asJSONObjectValueString(ObjectMapper m, Object... args)
        throws IOException
    {
        LinkedHashMap<Object,Object> map = new LinkedHashMap<Object,Object>();
        for (int i = 0, len = args.length; i < len; i += 2) {
            map.put(args[i], args[i+1]);
        }
        return m.writeValueAsString(map);
    }
}
