package org.codehaus.jackson.map;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

import main.BaseTest;

public abstract class BaseMapTest
    extends BaseTest
{
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

    protected String serializeAsString(ObjectMapper m, Object value)
        throws IOException
    {
        /* 28-Aug-2009, tatu: Let's use new functionality, to also
         *   exercise specialized custom "stringable" writer...
         */
        /*
        StringWriter sw = new StringWriter();
        m.writeValue(sw, value);
        return sw.toString();
        */
        return m.writeValueAsString(value);
    }

    protected String serializeAsString(Object value)
        throws IOException
    {
        return serializeAsString(new ObjectMapper(), value);
    }
}



