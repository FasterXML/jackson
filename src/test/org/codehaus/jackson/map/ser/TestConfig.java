package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for checking handling of SerializationConfig.
 */
public class TestConfig
    extends BaseMapTest
{
    @JsonWriteNullProperties(false)
    @JsonAutoDetect(JsonMethod.NONE)
    final static class Dummy { }

    public void testDefaults()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();

        // First, defaults:
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.WRITE_NULL_PROPERTIES));
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS));
    }

    public void testFromAnnotations()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();

        /* then configure using annotations from the dummy object; only
         * subset of features affected this way
         */
        cfg.fromAnnotations(Dummy.class);
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.WRITE_NULL_PROPERTIES));
    }
}
