package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestObjectMapperSimpleSerializer
    extends BaseTest
{
    protected enum TestEnum { VALUE1, VALUE2, NONE; }

    public void testBoolean() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, Boolean.TRUE);
        assertEquals("true", sw.toString());
        sw = new StringWriter();
        mapper.writeValue(sw, Boolean.FALSE);
        assertEquals("false", sw.toString());
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
            StringWriter sw = new StringWriter();
            float f = (float) d;
            mapper.writeValue(sw, Float.valueOf(f));
            assertEquals(String.valueOf(f), sw.toString());
        }
    }

    public void testDouble() throws Exception
    {
        double[] values = new double[] {
            0.0, 1.0, 0.1, -37.01, 999.99, 0.3, 33.3
        };
        ObjectMapper mapper = new ObjectMapper();

        for (double d : values) {
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, Double.valueOf(d));
            assertEquals(String.valueOf(d), sw.toString());
        }
    }

    public void testEnum() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, TestEnum.VALUE2);
        assertEquals("\"VALUE2\"", sw.toString());

    }
}
