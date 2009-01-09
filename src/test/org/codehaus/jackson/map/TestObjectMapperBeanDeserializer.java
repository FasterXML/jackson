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

    final static class FactoryValueBean
    {
        final String _desc;

        protected FactoryValueBean(String desc, int dummy) { _desc = desc; }

        public static FactoryValueBean valueOf(String v) { return new FactoryValueBean(v, 0); }
        public static FactoryValueBean valueOf(int v) { return new FactoryValueBean(String.valueOf(v), 0); }
        public static FactoryValueBean valueOf(long v) { return new FactoryValueBean(String.valueOf(v), 0); }

        @Override public String toString() { return _desc; }
    }

    /*
    /////////////////////////////////////////////////
    // Deserialization from simple types (String, int)
    /////////////////////////////////////////////////
     */

    public void testFromStringCtor() throws Exception
    {
        CtorValueBean result = new ObjectMapper().readValue("\"abc\"", CtorValueBean.class);
        assertEquals("abc", result.toString());
    }

    public void testFromIntCtor() throws Exception
    {
        CtorValueBean result = new ObjectMapper().readValue("13", CtorValueBean.class);
        assertEquals("13", result.toString());
    }

    public void testFromLongCtor() throws Exception
    {
        // Must use something that is forced as Long...
        long value = 12345678901244L;
        CtorValueBean result = new ObjectMapper().readValue(""+value, CtorValueBean.class);
        assertEquals(""+value, result.toString());
    }

    public void testFromStringFactory() throws Exception
    {
        FactoryValueBean result = new ObjectMapper().readValue("\"abc\"", FactoryValueBean.class);
        assertEquals("abc", result.toString());
    }

    public void testFromIntFactory() throws Exception
    {
        FactoryValueBean result = new ObjectMapper().readValue("13", FactoryValueBean.class);
        assertEquals("13", result.toString());
    }

    public void testFromLongFactory() throws Exception
    {
        // Must use something that is forced as Long...
        long value = 12345678901244L;
        FactoryValueBean result = new ObjectMapper().readValue(""+value, FactoryValueBean.class);
        assertEquals(""+value, result.toString());
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
