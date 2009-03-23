package org.codehaus.jackson.map.deser;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying handling of non-specific numeric types.
 */
public class TestGenericNumber
    extends BaseMapTest
{
    public void testIntAsNumber() throws Exception
    {
        /* Even if declared as 'generic' type, should return using most
         * efficient type... here, Integer
         */
        Number result = new ObjectMapper().readValue(new StringReader(" 123 "), Number.class);
        assertEquals(Integer.valueOf(123), result);
    }

    /**
     * Related to [JACKSON-72]: by default should wrap floating-point
     * Number as Double
     */
    public void testDoubleAsNumber() throws Exception
    {
        Number result = new ObjectMapper().readValue(new StringReader(" 1.0 "), Number.class);
        assertEquals(Double.valueOf(1.0), result);
    }

    /**
     * Test for verifying [JACKSON-72].
     */
    public void testFpTypeOverrideSimple() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().enable(DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS);
        BigDecimal dec = new BigDecimal("0.1");

        // First test generic stand-alone Number
        Number result = m.readValue(dec.toString(), Number.class);
        assertEquals(BigDecimal.class, result.getClass());
        assertEquals(dec, result);

        // Then plain old Object
        Object value = m.readValue(dec.toString(), Object.class);
        assertEquals(BigDecimal.class, result.getClass());
        assertEquals(dec, value);
    }

	public void testFpTypeOverrideStructured() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        BigDecimal dec = new BigDecimal("-19.37");

        m.getDeserializationConfig().enable(DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS);

        // List element types
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>)m.readValue("[ "+dec.toString()+" ]", List.class);
        assertEquals(1, list.size());
        Object val = list.get(0);
        assertEquals(BigDecimal.class, val.getClass());
        assertEquals(dec, val);

        // and a map
        Map<?,?> map = m.readValue("{ \"a\" : "+dec.toString()+" }", Map.class);
        assertEquals(1, map.size());
        val = map.get("a");
        assertEquals(BigDecimal.class, val.getClass());
        assertEquals(dec, val);
    }
}
