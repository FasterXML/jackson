package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying deserialization of Beans.
 */
public class TestObjectMapperBeanDeserializer
    extends BaseTest
{
    /*
    /////////////////////////////////////////////////
    // Helper classes
    /////////////////////////////////////////////////
     */

    final static class CtorValueBean
    {
        final String _desc;

        public CtorValueBean(String d) { _desc = d; }
        public CtorValueBean(int value) { _desc = String.valueOf(value); }
        public CtorValueBean(long value) { _desc = String.valueOf(value); }

        @Override public String toString() { return _desc; }
    }

    /*
    /////////////////////////////////////////////////
    // Deserialization from simple types (String, int)
    /////////////////////////////////////////////////
     */

    public void testFromStringCtor() throws Exception
    {
        assertEquals("abc", new ObjectMapper().readValue("\"abc\"", CtorValueBean.class));
    }

    public void testFromIntCtor() throws Exception
    {
        assertEquals("13", new ObjectMapper().readValue("13", CtorValueBean.class));
    }

    public void testFromLongCtor() throws Exception
    {
        // Must use something that is forced as Long...
        long value = 12345678901244L;
        assertEquals(""+value, new ObjectMapper().readValue(""+value, CtorValueBean.class));
    }

    public void testFromStringFactory() throws Exception
    {
    }

    /*
    /////////////////////////////////////////////////
    // Deserialization from Json Object
    /////////////////////////////////////////////////
     */

    /*
    /////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////
     */
}
