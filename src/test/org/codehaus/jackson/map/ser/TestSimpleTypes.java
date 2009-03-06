package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.*;

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
            0.0, 1.0, 0.1, -37.01, 999.99, 0.3, 33.3
        };
        ObjectMapper mapper = new ObjectMapper();

        for (double d : values) {
            float f = (float) d;
            assertEquals(String.valueOf(f),serializeAsString(mapper, Float.valueOf(f)));
        }
    }

    public void testDouble() throws Exception
    {
        double[] values = new double[] {
            0.0, 1.0, 0.1, -37.01, 999.99, 0.3, 33.3
        };
        ObjectMapper mapper = new ObjectMapper();

        for (double d : values) {
            assertEquals(String.valueOf(d),serializeAsString(mapper, Double.valueOf(d)));
        }
    }

    public void testClass() throws Exception
    {
        Map<String,Object> result = writeAndMap(Object.class);
        assertEquals(1, result.size());
    }
}
