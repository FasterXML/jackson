package org.codehaus.jackson.map;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

import org.codehaus.jackson.annotate.JsonCreator;

import main.BaseTest;

public abstract class BaseMapTest
    extends BaseTest
{
    /*
    //////////////////////////////////////////////
    // Shared helper classes
    //////////////////////////////////////////////
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

    protected BaseMapTest() { super(); }

    /*
    //////////////////////////////////////////////
    // Additional assert methods
    //////////////////////////////////////////////
     */

    protected void assertEquals(int[] exp, int[] act)
    {
        assertArrayEquals(exp, act);
    }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
    protected Map<String,Object> writeAndMap(ObjectMapper m, Object value)
        throws IOException
    {
        String str = serializeAsString(m, value);
        return (Map<String,Object>) m.readValue(str, Object.class);
    }

    protected Map<String,Object> writeAndMap(Object value)
        throws IOException
    {
        return writeAndMap(new ObjectMapper(), value);
    }

    @SuppressWarnings("unchecked")
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

    public String quote(String str) {
        return '"'+str+'"';
    }
}



