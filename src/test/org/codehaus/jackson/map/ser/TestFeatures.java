package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.BaseMapTest;

import java.io.*;
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
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Class with one explicitly defined getter, one name-based
     * auto-detectable getter.
     */
    static class GetterClass
    {
        @JsonProperty("x") public int getX() { return -2; }
        public int getY() { return 1; }
    }

    /**
     * Another test-class that explicitly disables auto-detection
     */
    @JsonAutoDetect(JsonMethod.NONE)
    static class DisabledGetterClass
    {
        @JsonProperty("x") public int getX() { return -2; }
        public int getY() { return 1; }
    }

    /**
     * Another test-class that explicitly enables auto-detection
     */
    @JsonAutoDetect(JsonMethod.GETTER)
    static class EnabledGetterClass
    {
        @JsonProperty("x") public int getX() { return -2; }
        public int getY() { return 1; }

        // not auto-detected, since "is getter" auto-detect disabled
        public boolean isOk() { return true; }
    }

    /**
     * One more: only detect "isXxx", not "getXXX"
     */
    @JsonAutoDetect(JsonMethod.IS_GETTER)
    static class EnabledIsGetterClass
    {
        // Won't be auto-detected any more
        public int getY() { return 1; }

        // but this will be
        public boolean isOk() { return true; }
    }

    static class CloseableBean implements Closeable
    {
        public int a = 3;

        protected boolean wasClosed = false;
        
        @Override
        public void close() throws IOException {
            wasClosed = true;
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testGlobalAutoDetection() throws IOException
    {
        // First: auto-detection enabled (default):
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new GetterClass());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(-2), result.get("x"));
        assertEquals(Integer.valueOf(1), result.get("y"));

        // Then auto-detection disabled. But note: we MUST create a new
        // mapper, since old version of serializer may be cached by now
        m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, false);
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
        m.configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, false);
        result = writeAndMap(m, new EnabledGetterClass());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("x"));
        assertTrue(result.containsKey("y"));
    }

    public void testPerClassAutoDetectionForIsGetter() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        // class level should override
        m.configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, true);
        m.configure(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS, false);
         Map<String,Object> result = writeAndMap(m, new EnabledIsGetterClass());
        assertEquals(1, result.size());
        assertTrue(result.containsKey("ok"));
        assertEquals(Boolean.TRUE, result.get("ok"));
    }

    // Simple test verifying that chainable methods work ok...
    public void testConfigChainability()
    {
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.getDeserializationConfig().isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS));
        assertTrue(m.getSerializationConfig().isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
        m.configure(DeserializationConfig.Feature.AUTO_DETECT_SETTERS, false).configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, false);
        assertFalse(m.getDeserializationConfig().isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS));
        assertFalse(m.getSerializationConfig().isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
    }

    // Test for [JACKSON-282]
    public void testCloseCloseable() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        // default should be disabled:
        CloseableBean bean = new CloseableBean();
        m.writeValueAsString(bean);
        assertFalse(bean.wasClosed);

        // but can enable it:
        m.configure(SerializationConfig.Feature.CLOSE_CLOSEABLE, true);
        bean = new CloseableBean();
        m.writeValueAsString(bean);
        assertTrue(bean.wasClosed);

        // also: let's ensure that ObjectWriter won't interfere with it
        bean = new CloseableBean();
        m.writerWithType(CloseableBean.class).writeValueAsString(bean);
        assertTrue(bean.wasClosed);
    }

    // Test for [JACKSON-289]
    public void testCharArrays() throws IOException
    {
        char[] chars = new char[] { 'a','b','c' };
        ObjectMapper m = new ObjectMapper();
        // default: serialize as Strings
        assertEquals(quote("abc"), m.writeValueAsString(chars));
        
        // new feature: serialize as JSON array:
        m.configure(SerializationConfig.Feature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true);
        assertEquals("[\"a\",\"b\",\"c\"]", m.writeValueAsString(chars));
    }

    // Test for [JACKSON-401]
    public void testFlushingAutomatic() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        assertTrue(mapper.getSerializationConfig().isEnabled(SerializationConfig.Feature.FLUSH_AFTER_WRITE_VALUE));
        // default is to flush after writeValue()
        StringWriter sw = new StringWriter();
        JsonGenerator jgen = mapper.getJsonFactory().createJsonGenerator(sw);
        mapper.writeValue(jgen, Integer.valueOf(13));
        assertEquals("13", sw.toString());
        jgen.close();

        // ditto with ObjectWriter
        sw = new StringWriter();
        jgen = mapper.getJsonFactory().createJsonGenerator(sw);
        ObjectWriter ow = mapper.writer();
        ow.writeValue(jgen, Integer.valueOf(99));
        assertEquals("99", sw.toString());
        jgen.close();
    }

    // Test for [JACKSON-401]
    public void testFlushingNotAutomatic() throws IOException
    {
        // but should not occur if configured otherwise
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.FLUSH_AFTER_WRITE_VALUE, false);
        StringWriter sw = new StringWriter();
        JsonGenerator jgen = mapper.getJsonFactory().createJsonGenerator(sw);

        mapper.writeValue(jgen, Integer.valueOf(13));
        // no flushing now:
        assertEquals("", sw.toString());
        // except when actually flushing
        jgen.flush();
        assertEquals("13", sw.toString());
        jgen.close();
        // Also, same should happen with ObjectWriter
        sw = new StringWriter();
        jgen = mapper.getJsonFactory().createJsonGenerator(sw);
        ObjectWriter ow = mapper.writer();
        ow.writeValue(jgen, Integer.valueOf(99));
        assertEquals("", sw.toString());
        // except when actually flushing
        jgen.flush();
        assertEquals("99", sw.toString());
        jgen.close();
    }
}
