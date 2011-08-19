package org.codehaus.jackson.map.introspect;

import java.util.Map;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * @since 1.9
 */
public class TestPOJOPropertiesCollector
    extends BaseMapTest
{
    static class Simple {
        public int value;
        
        @JsonProperty("value")
        public void valueSetter(int v) { value = v; }

        @JsonProperty("value")
        public int getFoobar() { return value; }
    }

    static class SimpleFieldDeser
    {
        @JsonDeserialize String[] values;
    }
    
    static class SimpleGetterVisibility {
        public int getA() { return 0; }
        protected int getB() { return 1; }
        @SuppressWarnings("unused")
        private int getC() { return 2; }
    }
    
    // Class for testing 'shared ignore'
    static class Empty {
        public int value;
        
        public void setValue(int v) { value = v; }

        @JsonIgnore
        public int getValue() { return value; }
    }

    static class IgnoredSetter {
        @JsonProperty
        public int value;
        
        @JsonIgnore
        public void setValue(int v) { value = v; }

        public int getValue() { return value; }
    }

    // Should find just one setter for "y", due to partial ignore
    static class IgnoredRenamedSetter {
        @JsonIgnore public void setY(int value) { }
        @JsonProperty("y") void foobar(int value) { }
    }
    
    // should produce a single property, "x"
    static class RenamedProperties {
        @JsonProperty("x")
        public int value;
        
        public void setValue(int v) { value = v; }

        public int getX() { return value; }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimple()
    {
        POJOPropertiesCollector coll = collector(Simple.class, true);
        Map<String, POJOPropertyCollector> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyCollector prop = props.get("value");
        assertNotNull(prop);
        assertTrue(prop.hasSetter());
        assertTrue(prop.hasGetter());
        assertTrue(prop.hasField());
    }

    public void testSimpleFieldVisibility()
    {
        // first, for serialization:
        POJOPropertiesCollector coll = collector(SimpleFieldDeser.class, false);
        Map<String, POJOPropertyCollector> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyCollector prop = props.get("values");
        assertNotNull(prop);
        assertFalse(prop.hasSetter());
        assertFalse(prop.hasGetter());
        assertTrue(prop.hasField());
    }

    public void testSimpleGetterVisibility()
    {
        POJOPropertiesCollector coll = collector(SimpleGetterVisibility.class, true);
        Map<String, POJOPropertyCollector> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyCollector prop = props.get("a");
        assertNotNull(prop);
        assertFalse(prop.hasSetter());
        assertTrue(prop.hasGetter());
        assertFalse(prop.hasField());
    }
    
    /**
     * Unit test for verifying that a single @JsonIgnore can remove the
     * whole property, unless explicit property marker exists
     */
    public void testEmpty()
    {
        POJOPropertiesCollector coll = collector(Empty.class, true);
        Map<String, POJOPropertyCollector> props = coll.getPropertyMap();
        assertEquals(0, props.size());
    }

    /**
     * Unit test for verifying handling of 'partial' @JsonIgnore; that is,
     * if there is at least one explicit annotation to indicate property,
     * only parts that are ignored are, well, ignored
     */
    public void testPartialIgnore()
    {
        POJOPropertiesCollector coll = collector(IgnoredSetter.class, true);
        Map<String, POJOPropertyCollector> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyCollector prop = props.get("value");
        assertNotNull(prop);
        assertFalse(prop.hasSetter());
        assertTrue(prop.hasGetter());
        assertTrue(prop.hasField());
    }

    public void testSimpleRenamed()
    {
        POJOPropertiesCollector coll = collector(RenamedProperties.class, true);
        Map<String, POJOPropertyCollector> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyCollector prop = props.get("x");
        assertNotNull(prop);
        assertTrue(prop.hasSetter());
        assertTrue(prop.hasGetter());
        assertTrue(prop.hasField());
    }

    public void testSimpleIgnoreAndRename()
    {
        POJOPropertiesCollector coll = collector(IgnoredRenamedSetter.class, true);
        Map<String, POJOPropertyCollector> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyCollector prop = props.get("y");
        assertNotNull(prop);
        assertTrue(prop.hasSetter());
        assertFalse(prop.hasGetter());
        assertFalse(prop.hasField());
    }

    public void testGlobalVisibilityForGetters()
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, false);
        POJOPropertiesCollector coll = collector(m, SimpleGetterVisibility.class, true);
        // should be 1, expect that we disabled getter auto-detection, so
        Map<String, POJOPropertyCollector> props = coll.getPropertyMap();
        assertEquals(0, props.size());
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected POJOPropertiesCollector collector(Class<?> cls, boolean forSerialization)
    {
        return collector(new ObjectMapper(), cls, forSerialization);
    }

    protected POJOPropertiesCollector collector(ObjectMapper mapper,
            Class<?> cls, boolean forSerialization)
    {
        BasicClassIntrospector bci = new BasicClassIntrospector();
        // no real difference between serialization, deserialization, at least here
        return bci.collectProperties(mapper.getSerializationConfig(),
                mapper.constructType(cls), null, true);
    }
}
