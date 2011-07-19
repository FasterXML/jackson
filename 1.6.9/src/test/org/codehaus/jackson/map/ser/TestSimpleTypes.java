package org.codehaus.jackson.map.ser;

import java.math.BigInteger;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

import static org.junit.Assert.*;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestSimpleTypes
    extends BaseMapTest
{
    public void testBoolean() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("true", serializeAsString(mapper, Boolean.TRUE));
        assertEquals("false", serializeAsString(mapper, Boolean.FALSE));
    }

    public void testBooleanArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("[true,false]", serializeAsString(mapper, new boolean[] { true, false} ));
        assertEquals("[true,false]", serializeAsString(mapper, new Boolean[] { Boolean.TRUE, Boolean.FALSE} ));
    }

    public void testByteArray() throws Exception
    {
        byte[] data = { 1, 17, -3, 127, -128 };
        Byte[] data2 = new Byte[data.length];
        for (int i = 0; i < data.length; ++i) {
            data2[i] = data[i]; // auto-boxing
        }
        ObjectMapper mapper = new ObjectMapper();
        // For this we need to deserialize, to get base64 codec
        String str1 = serializeAsString(mapper, data);
        String str2 = serializeAsString(mapper, data2);
        assertArrayEquals(data, mapper.readValue(str1, byte[].class));
        assertArrayEquals(data2, mapper.readValue(str2, Byte[].class));
    }

    public void testShortArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("[0,1]", serializeAsString(mapper, new short[] { 0, 1 }));
        assertEquals("[2,3]", serializeAsString(mapper, new Short[] { 2, 3 }));
    }

    public void testIntArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("[0,-3]", serializeAsString(mapper, new int[] { 0, -3 }));
        assertEquals("[13,9]", serializeAsString(mapper, new Integer[] { 13, 9 }));
    }

    /* Note: dealing with floating-point values is tricky; not sure if
     * we can really use equality tests here... JDK does have decent
     * conversions though, to retain accuracy and round-trippability.
     * But still...
     */
    public void testFloat() throws Exception
    {
        double[] values = new double[] {
            0.0, 1.0, 0.1, -37.01, 999.99, 0.3, 33.3, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        ObjectMapper mapper = new ObjectMapper();

        for (double d : values) {
           float f = (float) d;
    	   String expected = String.valueOf(f);
           if (Float.isNaN(f) || Float.isInfinite(f)) {
               expected = "\""+expected+"\"";
       	   }
           assertEquals(expected,serializeAsString(mapper, Float.valueOf(f)));
        }
    }

    public void testDouble() throws Exception
    {
        double[] values = new double[] {
            0.0, 1.0, 0.1, -37.01, 999.99, 0.3, 33.3, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        ObjectMapper mapper = new ObjectMapper();

        for (double d : values) {
            String expected = String.valueOf(d);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                expected = "\""+d+"\"";
            }
            assertEquals(expected,serializeAsString(mapper, Double.valueOf(d)));
        }
    }

    public void testBigInteger() throws Exception
    {
        BigInteger[] values = new BigInteger[] {
                BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO,
                BigInteger.valueOf(1234567890L),
                new BigInteger("123456789012345678901234568"),
                new BigInteger("-1250000124326904597090347547457")
                };
        ObjectMapper mapper = new ObjectMapper();

        for (BigInteger value : values) {
            String expected = value.toString();
            assertEquals(expected,serializeAsString(mapper, value));
        }
    }
    
    public void testClass() throws Exception
    {
        String result = serializeAsString(java.util.List.class);
        assertEquals("\"java.util.List\"", result);
    }
}
