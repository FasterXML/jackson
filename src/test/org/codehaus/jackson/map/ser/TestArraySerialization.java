package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
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
}


