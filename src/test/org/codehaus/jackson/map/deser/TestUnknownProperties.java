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
    final static String JSON_UNKNOWN_FIELD = "{ \"a\" : 1, \"foo\" : 3 }";
    final static class TestBean
    {
        String _unknown;

        int _a;

        public TestBean() { }

        public void setA(int a) { _a = a; }

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
            ((TestBean) bean).markUnknown(propertyName+":"+jp.getText());
            // Yup, we are good to go; must skip whatever value we'd have:
            jp.skipChildren();
            return true;
        }
    }

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
     * And then using a handler...
     */
    public void testUnknownHandlingIgnore()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().addHandler(new MyHandler());
        TestBean result = mapper.readValue(new StringReader(JSON_UNKNOWN_FIELD), TestBean.class);
        assertNotNull(result);
        assertEquals(1, result._a);
        assertEquals("foo:3", result._unknown);
    }
}
