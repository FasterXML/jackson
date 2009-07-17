package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

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

    public void testClass() throws Exception
    {
        String result = serializeAsString(java.util.List.class);
        assertEquals("\"java.util.List\"", result);
    }
}
