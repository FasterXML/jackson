package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;

import org.codehaus.jackson.map.*;

public class TestArraySerialization
    extends BaseTest
{
    public void testIntArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, new int[] { 1, 2, 3, -7 });
        assertEquals("[1,2,3,-7]", sw.toString().trim());
    }

    public void testStringArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, new String[] { "a", "\"foo\"", null });
        assertEquals("[\"a\",\"\\\"foo\\\"\",null]", sw.toString().trim());
    }

    public void testDoubleArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, new double[] { 1.01, 2.0, -7, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY });
        assertEquals("[1.01,2.0,-7.0,\"NaN\",\"-Infinity\",\"Infinity\"]", sw.toString().trim());
    }

    public void testFloatArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, new float[] { 1.01f, 2.0f, -7f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY });
        assertEquals("[1.01,2.0,-7.0,\"NaN\",\"-Infinity\",\"Infinity\"]", sw.toString().trim());
    }
}


