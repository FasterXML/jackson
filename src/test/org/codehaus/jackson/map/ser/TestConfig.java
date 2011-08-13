package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Unit tests for checking handling of SerializationConfig.
 */
public class TestConfig
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    @SuppressWarnings("deprecation")
    @JsonWriteNullProperties(false)
    @JsonAutoDetect(JsonMethod.NONE)
    final static class ConfigLegacy { }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT,
                   typing=JsonSerialize.Typing.STATIC)
    @JsonAutoDetect(JsonMethod.NONE)
    final static class Config { }

    final static class ConfigNone { }

    static class AnnoBean {
        public int getX() { return 1; }
        @SuppressWarnings("unused") @JsonProperty("y")
        private int getY() { return 2; }
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    @SuppressWarnings("deprecation")
    public void testDefaults()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();

        // First, defaults:
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.USE_ANNOTATIONS));
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS));

        assertTrue(cfg.isEnabled(SerializationConfig.Feature.WRITE_NULL_PROPERTIES));
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS));

        assertFalse(cfg.isEnabled(SerializationConfig.Feature.INDENT_OUTPUT));
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.USE_STATIC_TYPING));

        // since 1.3:
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS));
        // since 1.4
        
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS));
        // since 1.5
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION));

    }

    public void testOverrideIntrospectors()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();
        // and finally, ensure we could override introspectors
        cfg = cfg.withClassIntrospector(null); // no way to verify tho
        cfg = cfg.withAnnotationIntrospector(null);
        assertNull(cfg.getAnnotationIntrospector());
    }

    public void testMisc()
    {
        ObjectMapper m = new ObjectMapper();
        m.setDateFormat(null); // just to execute the code path
        assertNotNull(m.getSerializationConfig().toString()); // ditto
    }

    @SuppressWarnings("deprecation")
    public void testFromAnnotationsLegacy()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();

        /* then configure using annotations from the dummy object; only
         * subset of features affected this way
         */
        cfg.fromAnnotations(ConfigLegacy.class);
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.WRITE_NULL_PROPERTIES));

        /* 08-Mar-2010, tatu: the way auto-detection is handled was changed; these
         *    tests are not correct any more:
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS));
         */
    }

    @SuppressWarnings("deprecation")
    public void testFromAnnotations()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();
        
        // first without any annotations
        cfg.fromAnnotations(ConfigNone.class);
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS));
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.USE_STATIC_TYPING));
 
        /* then configure using annotations from the dummy object; only
         * subset of features affected this way
         */
        cfg.fromAnnotations(Config.class);
        assertEquals(JsonSerialize.Inclusion.NON_DEFAULT, cfg.getSerializationInclusion());
        assertTrue(cfg.isEnabled(SerializationConfig.Feature.USE_STATIC_TYPING));

        /* 08-Mar-2010, tatu: the way auto-detection is handled was changed; these
         *    tests are not correct any more:
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        assertFalse(cfg.isEnabled(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS));
        */
    }

    public void testIndentation() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", Integer.valueOf(2));
        String result = serializeAsString(m, map).trim();
        // 02-Jun-2009, tatu: not really a clean way but...
        String lf = System.getProperty("line.separator");
        assertEquals("{"+lf+"  \"a\" : 2"+lf+"}", result);
    }

    public void testAnnotationsDisabled() throws Exception
    {
        // first: verify that annotation introspection is enabled by default
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.getSerializationConfig().isEnabled(SerializationConfig.Feature.USE_ANNOTATIONS));
        Map<String,Object> result = writeAndMap(m, new AnnoBean());
        assertEquals(2, result.size());

        m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.USE_ANNOTATIONS, false);
        result = writeAndMap(m, new AnnoBean());
        assertEquals(1, result.size());
    }

    /**
     * Test for verifying working of [JACKSON-191]
     * 
     * @since 1.4
     */
    public void testProviderConfig() throws Exception   
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(0, mapper.getSerializerProvider().cachedSerializersCount());
        // and then should get one constructed for:
        Map<String,Object> result = this.writeAndMap(mapper, new AnnoBean());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
        assertEquals(Integer.valueOf(2), result.get("y"));

        /* Note: it is 2 because we'll also get serializer for basic 'int', not
         * just AnnoBean
         */
        /* 12-Jan-2010, tatus: Actually, probably more, if and when we typing
         *   aspects are considered (depending on what is cached)
         */
        int count = mapper.getSerializerProvider().cachedSerializersCount();
        if (count < 2) {
            fail("Should have at least 2 cached serializers, got "+count);
        }
        mapper.getSerializerProvider().flushCachedSerializers();
        assertEquals(0, mapper.getSerializerProvider().cachedSerializersCount());
    }

}
