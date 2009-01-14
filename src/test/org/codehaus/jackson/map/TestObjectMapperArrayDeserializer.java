package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.net.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeReference;

/**
 * This unit test suite tries to verify that the "Native" java type
 * mapper can properly re-construct Java array objects from Json arrays.
 */
public class TestObjectMapperArrayDeserializer
    extends BaseTest
{
    /*
    //////////////////////////////////////////
    // Tests for "untyped" arrays, Object[]
    //////////////////////////////////////////
     */

    public void testUntypedArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        // to get "untyped" default map-to-map, pass Object[].class
        String JSON = "[ 1, null, \"x\", true, 2.0 ]";

        Object[] result = mapper.readValue(JSON, Object[].class);
        assertNotNull(result);

        assertEquals(5, result.length);

        assertEquals(Integer.valueOf(1), result[0]);
        assertNull(result[1]);
        assertEquals("x", result[2]);
        assertEquals(Boolean.TRUE, result[3]);
        assertEquals(Double.valueOf(2.0), result[4]);
    }

    public void testIntegerArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final int LEN = 90000;

        // Let's construct array to get it big enough

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        sb.append(']');

        Integer[] result = mapper.readValue(sb.toString(), Integer[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            assertEquals(i, result[i].intValue());
        }
    }

    /*
    //////////////////////////////////////////
    // Tests for String arrays, char[]
    //////////////////////////////////////////
     */

    public void testStringArray() throws Exception
    {
        final String[] STRS = new String[] {
            "a", "b", "abcd", "", "???", "\"quoted\"", "lf: \n",
        };
        StringWriter sw = new StringWriter();
        JsonGenerator jg = new JsonFactory().createJsonGenerator(sw);
        jg.writeStartArray();
        for (String str : STRS) {
            jg.writeString(str);
        }
        jg.writeEndArray();
        jg.close();

        ObjectMapper mapper = new ObjectMapper();
        String[] result = mapper.readValue(sw.toString(), String[].class);
        assertNotNull(result);

        assertEquals(STRS.length, result.length);
        for (int i = 0; i < STRS.length; ++i) {
            assertEquals(STRS[i], result[i]);
        }
    }

    public void testCharArray() throws Exception
    {
        final String TEST_STR = "Let's just test it? Ok!";
        ObjectMapper mapper = new ObjectMapper();
        char[] result = mapper.readValue("\""+TEST_STR+"\"", char[].class);
        assertEquals(TEST_STR, new String(result));
    }

    /*
    //////////////////////////////////////////
    // Tests for primitive arrays
    //////////////////////////////////////////
     */

    public void testBooleanArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        boolean[] result = mapper.readValue("[ true, false, false ]", boolean[].class);
        assertNotNull(result);
        assertEquals(3, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertFalse(result[2]);
    }

    public void testByteArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final int LEN = 37000;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i & 0x7F);
        }
        sb.append(']');
        byte[] result = mapper.readValue(sb.toString(), byte[].class);
        assertNotNull(result);
        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            byte exp = (byte) (i & 0x7F);
            assertEquals(exp, result[i]);
        }
    }

    public void testShortArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final int LEN = 31001; // fits in signed 16-bit
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        sb.append(']');

        short[] result = mapper.readValue(sb.toString(), short[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            short exp = (short) i;
            assertEquals(exp, result[i]);
        }
    }

    public void testIntArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final int LEN = 70000;

        // Let's construct array to get it big enough

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(-i);
        }
        sb.append(']');

        int[] result = mapper.readValue(sb.toString(), int[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            assertEquals(-i, result[i]);
        }
    }

    public void testLongArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final int LEN = 12300;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        sb.append(']');

        long[] result = mapper.readValue(sb.toString(), long[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            long exp = (long) i;
            assertEquals(exp, result[i]);
        }
    }

    public void testDoubleArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final int LEN = 7000;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            // not ideal, but has to do...
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i).append('.').append(i % 10);
        }
        sb.append(']');

        double[] result = mapper.readValue(sb.toString(), double[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            String expStr = String.valueOf(i) + "." + String.valueOf(i % 10);
            String actStr = String.valueOf(result[i]);
            if (!expStr.equals(actStr)) {
                fail("Entry #"+i+"/"+LEN+"; exp '"+expStr+"', got '"+actStr+"'");
            }
        }
    }

    public void testFloatArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final int LEN = 7000;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            // not ideal, but has to do...
            sb.append(i).append('.').append(i % 10);
        }
        sb.append(']');

        float[] result = mapper.readValue(sb.toString(), float[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            String expStr = String.valueOf(i) + "." + String.valueOf(i % 10);
            assertEquals(expStr, String.valueOf(result[i]));
        }
    }

    /*
    //////////////////////////////////////////
    // Tests for Bean arrays
    //////////////////////////////////////////
     */

    public void testBeanArray()
        throws Exception
    {
        List<Bean1> src = new ArrayList<Bean1>();

        List<Bean2> b2 = new ArrayList<Bean2>();
        b2.add(new Bean2("a"));
        b2.add(new Bean2("foobar"));
        src.add(new Bean1(1, 2, b2));

        b2 = new ArrayList<Bean2>();
        b2.add(null);
        src.add(new Bean1(4, 5, b2));

        // Ok: let's assume bean serializer works ok....
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();

        mapper.writeValue(sw, src);

        // And then test de-serializer
        List<Bean1> result = mapper.readValue(sw.toString(), new TypeReference<List<Bean1>>() { });
        assertNotNull(result);
        assertEquals(src, result);
    }

    /*
    //////////////////////////////////////////
    // Helper classes
    //////////////////////////////////////////
     */

    public final static class Bean1
    {
        int _x, _y;
        List<Bean2> _beans;

        // Just for deserialization:
        private Bean1() { }

        public Bean1(int x, int y, List<Bean2> beans)
        {
            _x = x;
            _y = y;
            _beans = beans;
        }

        public int getX() { return _x; }
        public int getY() { return _y; }
        public List<Bean2> getBeans() { return _beans; }

        public void setX(int x) { _x = x; }
        public void setY(int y) { _y = y; }
        public void setBeans(List<Bean2> b) { _beans = b; }

        @Override public boolean equals(Object o) {
            if (!(o instanceof Bean1)) return false;
            Bean1 other = (Bean1) o;
            return (_x == other._x)
                && (_y == other._y)
                && _beans.equals(other._beans)
                ;
        }
    }

    /**
     * Simple bean that just gets serialized as a String value.
     * Deserialization from String value will be done via single-arg
     * constructor.
     */
    public final static class Bean2
        implements JsonSerializable // so we can output as simple String
    {
        final String _desc;

        public Bean2(String d)
        {
            _desc = d;
        }

        public void serialize(JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(_desc);
        }

        @Override public String toString() { return _desc; }

        @Override public boolean equals(Object o) {
            if (!(o instanceof Bean2)) return false;
            Bean2 other = (Bean2) o;
            return _desc.equals(other._desc);
        }
    }
}
