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

    final static class EmptyDummy { }

    static class AnnoBean {
        int value = 3;
        
        @JsonProperty("y")
            public void setX(int v) { value = v; }
    }
    
    /*
    //////////////////////////////////////////////
    // Main tests
    //////////////////////////////////////////////
     */

    public void testDefaults()
    {
        ObjectMapper m = new ObjectMapper();
        DeserializationConfig cfg = m.getDeserializationConfig();

        // Expected defaults:
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.USE_ANNOTATIONS));
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS));
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS));
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS));
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS));


        assertFalse(cfg.isEnabled(DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS));
        assertFalse(cfg.isEnabled(DeserializationConfig.Feature.USE_BIG_INTEGER_FOR_INTS));

        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    public void testOverrideIntrospectors()
    {
        ObjectMapper m = new ObjectMapper();
        DeserializationConfig cfg = m.getDeserializationConfig();
        // and finally, ensure we could override introspectors
        cfg.setIntrospector(null); // no way to verify tho
        cfg.setAnnotationIntrospector(null);
        assertNull(cfg.getAnnotationIntrospector());
    }

    public void testFromAnnotations()
    {
        ObjectMapper m = new ObjectMapper();
        DeserializationConfig cfg = m.getDeserializationConfig();

        // First: without any annotations
        cfg.fromAnnotations(EmptyDummy.class);
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS));
        assertTrue(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS));
        
        /* Then configure using annotations from dummy object that
         * does have annotations; only subset of features affected this way
         */
        cfg.fromAnnotations(Dummy.class);
        assertFalse(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS));
        assertFalse(cfg.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS));
    }
        
    public void testAnnotationsDisabled() throws Exception
    {
        // first: verify that annotation introspection is enabled by default
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.getDeserializationConfig().isEnabled(DeserializationConfig.Feature.USE_ANNOTATIONS));
        // with annotations, property is renamed
        AnnoBean bean = m.readValue("{ \"y\" : 0 }", AnnoBean.class);
        assertEquals(0, bean.value);

        m = new ObjectMapper();
        m.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, false);
        // without annotations, should default to default bean-based name...
        bean = m.readValue("{ \"x\" : 0 }", AnnoBean.class);
        assertEquals(0, bean.value);
    }
}
