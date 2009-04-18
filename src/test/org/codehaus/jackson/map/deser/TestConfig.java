package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.BaseMapTest;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for checking handling of DeserializationConfig.
 */
public class TestConfig
    extends BaseMapTest
{
    @JsonAutoDetect(JsonMethod.NONE)
    final static class Dummy { }

    public void testDefaults()
    {
        ObjectMapper m = new ObjectMapper();
        DeserializationConfig cfg = m.getDeserializationConfig();

        // Expected defaults:
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS));
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS));
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS));
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS));


        assertFalse(cfg.isEnabled(DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS));
        assertFalse(cfg.isEnabled(DeserializationConfig.Feature.USE_BIG_INTEGER_FOR_INTS));
    }

    public void testFromAnnotations()
    {
        ObjectMapper m = new ObjectMapper();
        DeserializationConfig cfg = m.getDeserializationConfig();

        /* Configure using annotations from the dummy object; only
         * subset of features affected this way
         */
        cfg.fromAnnotations(Dummy.class);
        assertFalse(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS));
        assertFalse(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS));
    }
}
