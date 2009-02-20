package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URI;
import java.util.*;

import org.codehaus.jackson.*;

/**
 * Unit tests for verifying handling of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestObjectMapperSimpleDeserializer
    extends BaseTest
{
    /*
    //////////////////////////////////////////////////////////
    // Then tests for primitives, wrappers
    //////////////////////////////////////////////////////////
     */

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

    /* Note: dealing with floating-point values is tricky; not sure if
     * we can really use equality tests here... JDK does have decent
     * conversions though, to retain accuracy and round-trippability.
     * But still...
     */
    public void testFloatWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        // Also: should be able to coerce floats, strings:
        String[] STRS = new String[] {
            "1.0", "0.0", "-0.3", "0.7", "42.012", "-999.0"
        };

        for (String str : STRS) {
            // First, as regular floating point value
            Float exp = Float.valueOf(str);
            Float result = mapper.readValue(new StringReader(str), Float.class);
            assertEquals(exp, result);

            // and then as coerced String:
            result = mapper.readValue(new StringReader(" \""+str+"\""), Float.class);
            assertEquals(exp, result);
        }
    }

    public void testDoubleWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        // Also: should be able to coerce doubles, strings:
        String[] STRS = new String[] {
            "1.0", "0.0", "-0.3", "0.7", "42.012", "-999.0"
        };

        for (String str : STRS) {
            // First, as regular double value
            Double exp = Double.valueOf(str);
            Double result = mapper.readValue(new StringReader(str), Double.class);
            assertEquals(exp, result);

            // and then as coerced String:
            result = mapper.readValue(new StringReader(" \""+str+"\""), Double.class);
            assertEquals(exp, result);
        }
    }

    /*
    //////////////////////////////////////////////////////////
    // Simple non-primitive types
    //////////////////////////////////////////////////////////
     */

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

    public void testBigDecimal() throws Exception
    {
        BigDecimal value = new BigDecimal("0.001");
        BigDecimal result = new ObjectMapper().readValue(new StringReader(value.toString()), BigDecimal.class);
        assertEquals(value, result);
    }

    public void testBigInteger() throws Exception
    {
        BigInteger value = new BigInteger("-1234567890123456789012345567809");
        BigInteger result = new ObjectMapper().readValue(new StringReader(value.toString()), BigInteger.class);
        assertEquals(value, result);
    }

    public void testUUID() throws Exception
    {
        UUID value = UUID.fromString("76e6d183-5f68-4afa-b94a-922c1fdb83f8");
        assertEquals(value, new ObjectMapper().readValue("\""+value.toString()+"\"", UUID.class));
    }

    public void testURL() throws Exception
    {
        URL value = new URL("http://foo.com");
        assertEquals(value, new ObjectMapper().readValue("\""+value.toString()+"\"", URL.class));
    }

    public void testURI() throws Exception
    {
        URI value = new URI("http://foo.com");
        assertEquals(value, new ObjectMapper().readValue("\""+value.toString()+"\"", URI.class));
    }

    /**
     * Simple test to check behavior when end-of-stream is encountered
     * without content. Should throw an exception.
     */
    public void testEOF() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Object result = mapper.readValue("    ", Object.class);
            fail("Expected an exception, but got result value: "+result);
        } catch (JsonMappingException jex) {
            verifyException(jex, "No content available");
        }
    }

    /*
    //////////////////////////////////////////////////////////
    // Sequence tests
    //////////////////////////////////////////////////////////
     */

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

