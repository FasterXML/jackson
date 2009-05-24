package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.OutputProperties;

/**
 * Unit tests for checking handling of SerializationConfig.
 */
public class TestConfig
    extends BaseMapTest
{
    @JsonWriteNullProperties(false)
    @JsonAutoDetect(JsonMethod.NONE)
    final static class ConfigLegacy { }

    @JsonSerialize(include=OutputProperties.NON_DEFAULT)
    @JsonAutoDetect(JsonMethod.NONE)
    final static class Config { }

    public void testDefaults()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();

        // First, defaults:
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS));

        assertTrue(cfg.isEnabled(SerializationConfig.Feature.WRITE_NULL_PROPERTIES));
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS));

        assertFalse(cfg.isEnabled(SerializationConfig.Feature.INDENT_OUTPUT));
    }

    public void testFromAnnotationsLegacy()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();

        /* then configure using annotations from the dummy object; only
         * subset of features affected this way
         */
        cfg.fromAnnotations(ConfigLegacy.class);
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.WRITE_NULL_PROPERTIES));
    }

    public void testFromAnnotations()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();

        /* then configure using annotations from the dummy object; only
         * subset of features affected this way
         */
        cfg.fromAnnotations(Config.class);
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        assertEquals(OutputProperties.NON_DEFAULT, cfg.getSerializationInclusion());
    }

    public void testIndentation() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", Integer.valueOf(2));
        String result = serializeAsString(m, map).trim();
        assertEquals("{\n  \"a\" : 2\n}", result);
    }
}
