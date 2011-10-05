package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that feature requested
 * via [JACKSON-88] ("setterless collections") work as
 * expected, similar to how Collections and Maps work
 * with JAXB.
 */
public class TestSetterlessProperties
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    static class CollectionBean
    {
        List<String> _values = new ArrayList<String>();

        public List<String> getValues() { return _values; }
    }

    static class MapBean
    {
        Map<String,Integer> _values = new HashMap<String,Integer>();

        public Map<String,Integer> getValues() { return _values; }
    }

    // testing to verify that field has precedence over getter, for lists
    static class Dual
    {
        @JsonProperty("list") protected List<Integer> values = new ArrayList<Integer>();

        public Dual() { }
        
        public List<Integer> getList() {
            throw new IllegalStateException("Should not get called");
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimpleSetterlessCollectionOk()
        throws Exception
    {
        CollectionBean result = new ObjectMapper().readValue
            ("{\"values\":[ \"abc\", \"def\" ]}", CollectionBean.class);
        List<String> l = result._values;
        assertEquals(2, l.size());
        assertEquals("abc", l.get(0));
        assertEquals("def", l.get(1));
    }

    /**
     * Let's also verify that disabling the feature makes
     * deserialization fail for setterless bean
     */
    public void testSimpleSetterlessCollectionFailure()
        throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // by default, it should be enabled
        assertTrue(m.getDeserializationConfig().isEnabled(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS));
        m.configure(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS, false);
        assertFalse(m.getDeserializationConfig().isEnabled(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS));

        // and now this should fail
        try {
            m.readValue
                ("{\"values\":[ \"abc\", \"def\" ]}", CollectionBean.class);
            fail("Expected an exception");
        } catch (JsonMappingException e) {
            /* Not a good exception, ideally could suggest a need for
             * a setter...?
             */
            verifyException(e, "Unrecognized field");
        }
    }

    public void testSimpleSetterlessMapOk()
        throws Exception
    {
        MapBean result = new ObjectMapper().readValue
            ("{\"values\":{ \"a\": 15, \"b\" : -3 }}", MapBean.class);
        Map<String,Integer> m = result._values;
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(15), m.get("a"));
        assertEquals(Integer.valueOf(-3), m.get("b"));
    }

    public void testSimpleSetterlessMapFailure()
        throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS, false);
        // so this should fail now without a setter
        try {
            m.readValue
                ("{\"values\":{ \"a\":3 }}", MapBean.class);
            fail("Expected an exception");
        } catch (JsonMappingException e) {
            verifyException(e, "Unrecognized field");
        }
    }

    /* Test for [JACKSON-328], precedence of "getter-as-setter" (for Lists) versus
     * field for same property.
     */
    public void testSetterlessPrecedence() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS, true);
        Dual value = m.readValue("{\"list\":[1,2,3]}, valueType)", Dual.class);
        assertNotNull(value);
        assertEquals(3, value.values.size());
    }
}
