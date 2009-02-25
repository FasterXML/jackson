package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for checking whether JsonSerializerFactory.Feature
 * configuration works
 */
public class TestFeatures
    extends BaseMapTest
{
    /**
     * Class with one explicitly defined getter, one name-based
     * auto-detectable getter.
     */
    static class GetterClass
    {
        @JsonGetter("x") public int getX() { return -2; }
        public int getY() { return 1; }
    }

    /**
     * Another test-class that explicitly disables auto-detection
     */
    @JsonAutoDetect(JsonMethod.NONE)
    static class DisabledGetterClass
    {
        @JsonGetter("x") public int getX() { return -2; }
        public int getY() { return 1; }
    }

    /**
     * Another test-class that explicitly enables auto-detection
     */
    @JsonAutoDetect(JsonMethod.GETTER)
    static class EnabledGetterClass
    {
        @JsonGetter("x") public int getX() { return -2; }
        public int getY() { return 1; }
    }

    public void testGlobalAutoDetection() throws IOException
    {
        // First: auto-detection enabled (default):
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new GetterClass());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(-2), result.get("x"));
        assertEquals(Integer.valueOf(1), result.get("y"));

        // Then auto-detection disabled:
        CustomSerializerFactory sf = new CustomSerializerFactory();
        sf.setFeature(SerializerFactory.Feature.AUTO_DETECT_GETTERS, false);
        m = new ObjectMapper(sf);
        result = writeAndMap(m, new GetterClass());
        assertEquals(1, result.size());
        assertTrue(result.containsKey("x"));
    }

    public void testPerClassAutoDetection() throws IOException
    {
        // First: class-level auto-detection disabling
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new DisabledGetterClass());
        assertEquals(1, result.size());
        assertTrue(result.containsKey("x"));

        // And then class-level auto-detection enabling, should override defaults
        CustomSerializerFactory sf = new CustomSerializerFactory();
        sf.setFeature(SerializerFactory.Feature.AUTO_DETECT_GETTERS, false);
        m = new ObjectMapper(sf);
        result = writeAndMap(m, new EnabledGetterClass());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("x"));
        assertTrue(result.containsKey("y"));
    }
}

