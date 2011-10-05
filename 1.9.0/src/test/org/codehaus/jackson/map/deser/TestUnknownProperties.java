package org.codehaus.jackson.map.deser;

import java.io.*;
import java.util.HashMap;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for checking handling of unknown properties
 */
public class TestUnknownProperties
    extends BaseMapTest
{
    final static String JSON_UNKNOWN_FIELD = "{ \"a\" : 1, \"foo\" : [ 1, 2, 3], \"b\" : -1 }";

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    final static class TestBean
    {
        String _unknown;

        int _a, _b;

        public TestBean() { }

        public void setA(int a) { _a = a; }
        public void setB(int b) { _b = b; }

        public void markUnknown(String unk) { _unknown = unk; }
    }

    /**
     * Simple {@link DeserializationProblemHandler} sub-class that
     * just marks unknown property/ies when encountered, along with
     * Json value of the property.
     */
    final static class MyHandler
        extends DeserializationProblemHandler
    {
        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonDeserializer<?> deserializer,
                                             Object bean, String propertyName)
            throws IOException, JsonProcessingException
        {
            JsonParser jp = ctxt.getParser();
            // very simple, just to verify that we do see correct token type
            ((TestBean) bean).markUnknown(propertyName+":"+jp.getCurrentToken().toString());
            // Yup, we are good to go; must skip whatever value we'd have:
            jp.skipChildren();
            return true;
        }
    }

    @JsonIgnoreProperties({"b", "c"})
    static class IgnoreSome
    {
        public int a, b;
        private String c, d;

        public IgnoreSome() { }

        public String c() { return c; }
        public void setC(String value) { c = value; }
        public String d() { return d; }
        public void setD(String value) { d = value; }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    static class IgnoreUnknown {
        public int a;
    }

    @SuppressWarnings("serial")
    @JsonIgnoreProperties({"a", "d"})
    static class IgnoreMap extends HashMap<String,Object> { }

    static class ImplicitIgnores {
        @JsonIgnore public int a;
        @JsonIgnore public void setB(int b) { }
        public int c;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    /**
     * By default we should just get an exception if an unknown property
     * is encountered
     */
    public void testUnknownHandlingDefault()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue(new StringReader(JSON_UNKNOWN_FIELD), TestBean.class);
        } catch (JsonMappingException jex) {
            verifyException(jex, "Unrecognized field \"foo\"");
        }
    }

    /**
     * Test that verifies that it is possible to ignore unknown properties using
     * {@link DeserializationProblemHandler}.
     */
    public void testUnknownHandlingIgnoreWithHandler()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().clearHandlers();
        mapper.getDeserializationConfig().addHandler(new MyHandler());
        TestBean result = mapper.readValue(new StringReader(JSON_UNKNOWN_FIELD), TestBean.class);
        assertNotNull(result);
        assertEquals(1, result._a);
        assertEquals(-1, result._b);
        assertEquals("foo:START_ARRAY", result._unknown);
    }

    /**
     * Test for checking that it is also possible to simply suppress
     * error reporting for unknown properties.
     *
     * @since 1.2
     */
    public void testUnknownHandlingIgnoreWithFeature()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TestBean result = null;
        try {
            result = mapper.readValue(new StringReader(JSON_UNKNOWN_FIELD), TestBean.class);
        } catch (JsonMappingException jex) {
            fail("Did not expect a problem, got: "+jex.getMessage());
        }
        assertNotNull(result);
        assertEquals(1, result._a);
        assertNull(result._unknown);
        assertEquals(-1, result._b);
    }

    /// @since 1.4
    public void testWithClassIgnore()
        throws Exception
    {
        IgnoreSome result = new ObjectMapper().readValue("{ \"a\":1,\"b\":2,\"c\":\"x\",\"d\":\"y\"}",
                IgnoreSome.class);
        // first: should deserialize 2 of properties normally
        assertEquals(1, result.a);
        assertEquals("y", result.d());
        // and not take other 2
        assertEquals(0, result.b);
        assertNull(result.c());
    }

    /// @since 1.4
    public void testClassIgnoreWithMap() throws Exception
    {
        // Let's actually use incompatible types for "a" and "d"; should not matter when ignored
        IgnoreMap result = new ObjectMapper().readValue
            ("{ \"a\":[ 1],\n"
                +"\"b\":2,\n"
                +"\"c\": \"x\",\n"
                +"\"d\":false }", IgnoreMap.class);
        assertEquals(2, result.size());
        Object ob = result.get("b");
        assertEquals(Integer.class, ob.getClass());
        assertEquals(Integer.valueOf(2), ob);
        assertEquals("x", result.get("c"));
        assertFalse(result.containsKey("a"));
        assertFalse(result.containsKey("d"));
    }

    /// @since 1.4
    public void testClassWithIgnoreUnknown() throws Exception
    {
        IgnoreUnknown result = new ObjectMapper().readValue
            ("{\"b\":3,\"c\":[1,2],\"x\":{ },\"a\":-3}", IgnoreUnknown.class);
        assertEquals(-3, result.a);
    }

    /**
     * Test that verifies that use of {@link JsonIgnore} will add implicit
     * skipping of matching properties.
     */
    public void testClassWithUnknownAndIgnore() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // should be ok: "a" and "b" ignored, "c" mapped:
        ImplicitIgnores result = m.readValue
            ("{\"a\":1,\"b\":2,\"c\":3 }", ImplicitIgnores.class);
        assertEquals(3, result.c);

        // but "d" is not defined, so should still error
        try {
            m.readValue("{\"a\":1,\"b\":2,\"c\":3,\"d\":4 }", ImplicitIgnores.class);            
        } catch (JsonMappingException e) {
            verifyException(e, "Unrecognized field \"d\"");
        }
    }
}
