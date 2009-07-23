package org.codehaus.jackson.map.deser;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Tests related to [WSTX-139]
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
}
