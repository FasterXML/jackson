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

    public void testByteWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Byte result = mapper.readValue(new StringReader("   -42\t"), Byte.class);
        assertEquals(Byte.valueOf((byte)-42), result);

        // Also: should be able to coerce floats, strings:
        result = mapper.readValue(new StringReader(" \"-12\""), Byte.class);
        assertEquals(Byte.valueOf((byte)-12), result);

        result = mapper.readValue(new StringReader(" 39.07"), Byte.class);
        assertEquals(Byte.valueOf((byte)39), result);
    }

    public void testShortWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Short result = mapper.readValue(new StringReader("37"), Short.class);
        assertEquals(Short.valueOf((short)37), result);

        // Also: should be able to coerce floats, strings:
        result = mapper.readValue(new StringReader(" \"-1009\""), Short.class);
        assertEquals(Short.valueOf((short)-1009), result);

        result = mapper.readValue(new StringReader("-12.9"), Short.class);
        assertEquals(Short.valueOf((short)-12), result);
    }

    public void testCharacterWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // First: canonical value is 1-char string
        Character result = mapper.readValue(new StringReader("\"a\""), Character.class);
        assertEquals(Character.valueOf('a'), result);

        // But can also pass in ascii code
        result = mapper.readValue(new StringReader(" "+((int) 'X')), Character.class);
        assertEquals(Character.valueOf('X'), result);
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


    public void testLongWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Long result = mapper.readValue(new StringReader("12345678901"), Long.class);
        assertEquals(Long.valueOf(12345678901L), result);

        // Also: should be able to coerce floats, strings:
        result = mapper.readValue(new StringReader(" \"-9876\""), Long.class);
        assertEquals(Long.valueOf(-9876), result);

        result = mapper.readValue(new StringReader("1918.3"), Long.class);
        assertEquals(Long.valueOf(1918), result);
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
        final int NR_OF_INTS = 100;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NR_OF_INTS; ++i) {
            sb.append(" ");
            sb.append(i);
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = mapper.getJsonFactory().createJsonParser(sb.toString());
        for (int i = 0; i < NR_OF_INTS; ++i) {
            Integer result = mapper.readValue(jp, Integer.class);
            assertEquals(Integer.valueOf(i), result);
        }
    }
}

