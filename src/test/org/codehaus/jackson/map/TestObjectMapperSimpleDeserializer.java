package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestObjectMapperSimpleDeserializer
    extends BaseTest
{
    /**
     * Simple unit test to verify that we can map boolean values to
     * java.lang.Boolean.
     */
    public void testBooleanWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Boolean result = mapper.readValue(new StringReader("true"), Boolean.class);
        assertEquals(Boolean.TRUE, result);
        result = mapper.readValue(new StringReader("false"), Boolean.class);
        assertEquals(Boolean.FALSE, result);
    }

    public void testIntWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Integer result = mapper.readValue(new StringReader("   -42\t"), Integer.class);
        assertEquals(Integer.valueOf(-42), result);

        // Also: should be able to coerce floats, strings:
        result = mapper.readValue(new StringReader(" \"-1200\""), Integer.class);
        assertEquals(Integer.valueOf(-1200), result);

        result = mapper.readValue(new StringReader(" 39.07"), Integer.class);
        assertEquals(Integer.valueOf(39), result);
    }

    public void testSingleString() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String value = "FOO!";
        String result = mapper.readValue(new StringReader("\""+value+"\""), String.class);
        assertEquals(value, result);
    }

    public void testNull() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // null doesn't really have a type, fake by assuming Object
        Object result = mapper.readValue(new StringReader("   null"), Object.class);
        assertNull(result);
    }

    /**
     * Then a unit test to verify that we can conveniently bind sequence of
     * space-separate simple values
     */
    public void testSequenceOfInts() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; ++i) {
            sb.append(" ");
            sb.append(i);
        }

        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < 100; ++i) {
            Integer result = mapper.readValue(new StringReader(sb.toString()), Integer.class);
            assertEquals(Integer.valueOf(i), result);
        }
    }
}

