package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.*;

/**
 * Unit tests for verifying that field-backed properties can also be serialized
 * (since version 1.1) as well as getter-accessible properties.
 */
public class TestFieldSerialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    static class SimpleFieldBean
    {
        public int x, y;

        // not auto-detectable, not public
        int z;

        // ignored, not detectable either
        @JsonIgnore public int a;
    }
    
    static class SimpleFieldBean2
    {
        @JsonSerialize String[] values;

        // note: this annotation should not matter for serialization:
        @JsonDeserialize int dummy;
    }

    static class TransientBean
    {
        public int a;
        // transients should not be included
        public transient int b;
        // or statics
        public static int c;
    }

    @JsonAutoDetect(JsonMethod.SETTER)
    public class NoAutoDetectBean
    {
        // not auto-detectable any more
        public int x;

        @JsonProperty("z")
        public int _z;
    }

    /**
     * Let's test invalid bean too: can't have 2 logical properties
     * with same name.
     *<p>
     * 21-Feb-2010, tatus: That is, not within same class.
     *    As per [JACKSON-226] it is acceptable to "override"
     *    field definitions in sub-classes.
     */
    public static class DupFieldBean
    {
        @JsonProperty("foo")
        public int _z;

        @SuppressWarnings("unused")
        @JsonSerialize
            private int foo;
    }

    public static class DupFieldBean2
    {
        public int z;

        @JsonProperty("z")
        public int _z;
    }

    public static class OkDupFieldBean
        extends SimpleFieldBean
    {
        @JsonProperty("x")
        protected int myX;

        @SuppressWarnings("hiding")
        public int y;

        public OkDupFieldBean(int x, int y) {
            this.myX = x;
            this.y = y;
        }
    }

    /**
     * It is ok to have a method-based and field-based property
     * introspectable: only one should be serialized, and since
     * methods have precedence, it should be the method one.
     */
    public static class FieldAndMethodBean
    {
        @JsonProperty public int z;

        @JsonProperty("z") public int getZ() { return z+1; }
    }

    /*
    /**********************************************************
    /* Main tests, success
    /**********************************************************
     */

    public void testSimpleAutoDetect() throws Exception
    {
        SimpleFieldBean bean = new SimpleFieldBean();
        // let's set x, leave y as is
        bean.x = 13;
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, bean);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(13), result.get("x"));
        assertEquals(Integer.valueOf(0), result.get("y"));
    }

    @SuppressWarnings("unchecked")
	public void testSimpleAnnotation() throws Exception
    {
        SimpleFieldBean2 bean = new SimpleFieldBean2();
        bean.values = new String[] { "a", "b" };
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, bean);
        assertEquals(1, result.size());
        List<String> values = (List<String>) result.get("values");
        assertEquals(2, values.size());
        assertEquals("a", values.get(0));
        assertEquals("b", values.get(1));
    }

    public void testTransientAndStatic() throws Exception
    {
        TransientBean bean = new TransientBean();
        Map<String,Object> result = writeAndMap(bean);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(0), result.get("a"));
    }

    public void testNoAutoDetect() throws Exception
    {
        NoAutoDetectBean bean = new NoAutoDetectBean();
        bean._z = -4;
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, bean);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(-4), result.get("z"));
    }

    /**
     * Unit test that verifies that if both a field and a getter
     * method exist for a logical property (which is allowed),
     * getter has precendence over field.
     */
    public void testMethodPrecedence() throws Exception
    {
        FieldAndMethodBean bean = new FieldAndMethodBean();
        bean.z = 9;
        assertEquals(10, bean.getZ());
        assertEquals("{\"z\":10}", serializeAsString(bean));
    }

    /**
     * Testing [JACKSON-226]: it is ok to have "field override",
     * as long as there are no intra-class conflicts.
     */
    public void testOkDupFields() throws Exception
    {
        OkDupFieldBean bean = new OkDupFieldBean(1, 2);
        Map<String,Object> json = writeAndMap(new ObjectMapper(), bean);
        assertEquals(2, json.size());
        assertEquals(Integer.valueOf(1), json.get("x"));
        assertEquals(Integer.valueOf(2), json.get("y"));
    }

    /*
    /**********************************************************
    /* Main tests, failure cases
    /**********************************************************
     */

    public void testFailureDueToDups() throws Exception
    {
        try {
            writeAndMap(new ObjectMapper(), new DupFieldBean());
        } catch (JsonMappingException e) {
            verifyException(e, "Multiple fields representing");
        }
    }

    public void testFailureDueToDupField() throws Exception
    {
        try {
            writeAndMap(new ObjectMapper(), new DupFieldBean2());
        } catch (JsonMappingException e) {
            verifyException(e, "Multiple fields representing");
        }
    }
}

