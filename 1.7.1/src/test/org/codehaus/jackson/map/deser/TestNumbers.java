package org.codehaus.jackson.map.deser;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.map.*;

/**
 * Tests related to [JACKSON-139]
 */
public class TestNumbers
    extends BaseMapTest
{
    public void testFloatNaN() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Float result = m.readValue(" \"NaN\"", Float.class);
        assertEquals(Float.valueOf(Float.NaN), result);
    }

    public void testDoubleInf() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Double result = m.readValue(" \""+Double.POSITIVE_INFINITY+"\"", Double.class);
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), result);

        result = m.readValue(" \""+Double.NEGATIVE_INFINITY+"\"", Double.class);
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), result);
    }

    // [JACKSON-349]
    public void testEmptyAsNumber() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        assertNull(m.readValue(quote(""), Integer.class));
        assertNull(m.readValue(quote(""), Long.class));
        assertNull(m.readValue(quote(""), Float.class));
        assertNull(m.readValue(quote(""), Double.class));
        assertNull(m.readValue(quote(""), BigInteger.class));
        assertNull(m.readValue(quote(""), BigDecimal.class));
    }
}
