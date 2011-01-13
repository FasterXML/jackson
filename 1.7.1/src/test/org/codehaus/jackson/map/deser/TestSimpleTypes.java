package org.codehaus.jackson.map.deser;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URI;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.util.TokenBuffer;

/**
 * Unit tests for verifying handling of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestSimpleTypes
    extends BaseMapTest
{
    final static String NAN_STRING = "NaN";

    final static class BooleanBean {
        boolean _v;
        void setV(boolean v) { _v = v; }
    }

    static class IntBean {
        int _v;
        void setV(int v) { _v = v; }
    }

    final static class DoubleBean {
        double _v;
        void setV(double v) { _v = v; }
    }

    final static class FloatBean {
        float _v;
        void setV(float v) { _v = v; }
    }

    /**
     * Also, let's ensure that it's ok to override methods.
     */
    static class IntBean2
        extends IntBean
    {
        @Override
        void setV(int v2) { super.setV(v2+1); }
    }

    /*
    /**********************************************************
    /* Then tests for primitives
    /**********************************************************
     */

    public void testBooleanPrimitive() throws Exception
    {
        // first, simple case:
        ObjectMapper mapper = new ObjectMapper();
        BooleanBean result = mapper.readValue(new StringReader("{\"v\":true}"), BooleanBean.class);
        assertTrue(result._v);
        // then [JACKSON-79]:
        result = mapper.readValue(new StringReader("{\"v\":null}"), BooleanBean.class);
        assertNotNull(result);
        assertFalse(result._v);

        // should work with arrays too..
        boolean[] array = mapper.readValue(new StringReader("[ null ]"), boolean[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertFalse(array[0]);
    }

    public void testIntPrimitive() throws Exception
    {
        // first, simple case:
        ObjectMapper mapper = new ObjectMapper();
        IntBean result = mapper.readValue(new StringReader("{\"v\":3}"), IntBean.class);
        assertEquals(3, result._v);
        // then [JACKSON-79]:
        result = mapper.readValue(new StringReader("{\"v\":null}"), IntBean.class);
        assertNotNull(result);
        assertEquals(0, result._v);

        // should work with arrays too..
        int[] array = mapper.readValue(new StringReader("[ null ]"), int[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(0, array[0]);
    }

    public void testDoublePrimitive() throws Exception
    {
        // first, simple case:
        ObjectMapper mapper = new ObjectMapper();
        // bit tricky with binary fps but...
        double value = 0.016;
        DoubleBean result = mapper.readValue(new StringReader("{\"v\":"+value+"}"), DoubleBean.class);
        assertEquals(value, result._v);
        // then [JACKSON-79]:
        result = mapper.readValue(new StringReader("{\"v\":null}"), DoubleBean.class);
        assertNotNull(result);
        assertEquals(0.0, result._v);

        // should work with arrays too..
        double[] array = mapper.readValue(new StringReader("[ null ]"), double[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(0.0, array[0]);
    }

    public void testDoublePrimitiveNonNumeric() throws Exception
    {
        // first, simple case:
        ObjectMapper mapper = new ObjectMapper();
        // bit tricky with binary fps but...
        double value = Double.POSITIVE_INFINITY;
        DoubleBean result = mapper.readValue(new StringReader("{\"v\":\""+value+"\"}"), DoubleBean.class);
        assertEquals(value, result._v);
        
        // should work with arrays too..
        double[] array = mapper.readValue(new StringReader("[ \"Infinity\" ]"), double[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(Double.POSITIVE_INFINITY, array[0]);
    }
    
    public void testFloatPrimitiveNonNumeric() throws Exception
    {
        // first, simple case:
        ObjectMapper mapper = new ObjectMapper();
        // bit tricky with binary fps but...
        float value = Float.POSITIVE_INFINITY;
        FloatBean result = mapper.readValue(new StringReader("{\"v\":\""+value+"\"}"), FloatBean.class);
        assertEquals(value, result._v);
        
        // should work with arrays too..
        float[] array = mapper.readValue(new StringReader("[ \"Infinity\" ]"), float[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals(Float.POSITIVE_INFINITY, array[0]);
    }
    
    /**
     * Beyond simple case, let's also ensure that method overriding works as
     * expected.
     */
    public void testIntWithOverride() throws Exception
    {
        // first, simple case:
        ObjectMapper mapper = new ObjectMapper();
        IntBean2 result = mapper.readValue(new StringReader("{\"v\":8}"), IntBean2.class);
        assertEquals(9, result._v);

    }

    /*
    /**********************************************************
    /* Then tests for wrappers
    /**********************************************************
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

        /* [JACKSON-78]: should accept ints too, (0 == false, otherwise true)
         */
        result = mapper.readValue(new StringReader("0"), Boolean.class);
        assertEquals(Boolean.FALSE, result);
        result = mapper.readValue(new StringReader("1"), Boolean.class);
        assertEquals(Boolean.TRUE, result);
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
            "1.0", "0.0", "-0.3", "0.7", "42.012", "-999.0", NAN_STRING
        };

        for (String str : STRS) {
            Float exp = Float.valueOf(str);
            Float result;

            if (NAN_STRING != str) {
                // First, as regular floating point value
                result = mapper.readValue(new StringReader(str), Float.class);
                assertEquals(exp, result);
            }

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
            "1.0", "0.0", "-0.3", "0.7", "42.012", "-999.0", NAN_STRING
        };

        for (String str : STRS) {
            Double exp = Double.valueOf(str);
            Double result;

            // First, as regular double value
            if (NAN_STRING != str) {
            	result = mapper.readValue(new StringReader(str), Double.class);
            	assertEquals(exp, result);
            }
            // and then as coerced String:
            result = mapper.readValue(new StringReader(" \""+str+"\""), Double.class);
            assertEquals(exp, result);
        }
    }

    /*
    /**********************************************************
    /* Simple non-primitive types
    /**********************************************************
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
        Object result = mapper.readValue("   null", Object.class);
        assertNull(result);
    }

    public void testClass() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Class<?> result = mapper.readValue("\"java.lang.String\"", Class.class);
        assertEquals(String.class, result);
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
        ObjectMapper mapper = new ObjectMapper();
        UUID value = UUID.fromString("76e6d183-5f68-4afa-b94a-922c1fdb83f8");
        assertEquals(value, mapper.readValue("\""+value.toString()+"\"", UUID.class));

        // [JACKSON-393] fix:

        // first, null should come as null
        TokenBuffer buf = new TokenBuffer(null);
        buf.writeObject(null);
        assertNull(mapper.readValue(buf.asParser(), UUID.class));

        // then, UUID itself come as is:
        buf = new TokenBuffer(null);
        buf.writeObject(value);
        assertSame(value, mapper.readValue(buf.asParser(), UUID.class));

        // and finally from byte[]
        // oh crap; JDK UUID just... sucks. Not even byte[] accessors or constructors? Huh?
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeLong(value.getMostSignificantBits());
        out.writeLong(value.getLeastSignificantBits());
        byte[] data = bytes.toByteArray();
        assertEquals(16, data.length);
        
        buf.writeObject(data);

        UUID value2 = mapper.readValue(buf.asParser(), UUID.class);
        
        assertEquals(value, value2);
    }

    public void testURL() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        URL value = new URL("http://foo.com");
        assertEquals(value, mapper.readValue("\""+value.toString()+"\"", URL.class));

        // trivial case; null to null, embedded URL to URL
        TokenBuffer buf = new TokenBuffer(null);
        buf.writeObject(null);
        assertNull(mapper.readValue(buf.asParser(), URL.class));

        // then, UUID itself come as is:
        buf = new TokenBuffer(null);
        buf.writeObject(value);
        assertSame(value, mapper.readValue(buf.asParser(), URL.class));
    }

    public void testURI() throws Exception
    {
        URI value = new URI("http://foo.com");
        assertEquals(value, new ObjectMapper().readValue("\""+value.toString()+"\"", URI.class));
    }

    /*
    /**********************************************************
    /* Sequence tests
    /**********************************************************
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

