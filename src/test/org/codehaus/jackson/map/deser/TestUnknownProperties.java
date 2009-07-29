package org.codehaus.jackson.map.deser;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for checking handling of unknown properties
 */
public class TestUnknownProperties
    extends BaseMapTest
{
    final static String JSON_UNKNOWN_FIELD = "{ \"a\" : 1, \"foo\" : [ 1, 2, 3], \"b\" : -1 }";

    /*
    ///////////////////////////////////////////////////////////
    // Helper classes
    ///////////////////////////////////////////////////////////
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

    /*
    ///////////////////////////////////////////////////////////
    // Test methods
    ///////////////////////////////////////////////////////////
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
     * {@link UnknownPropertyHandler}.
     */
    public void testUnknownHandlingIgnoreWithHandler()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
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
}
