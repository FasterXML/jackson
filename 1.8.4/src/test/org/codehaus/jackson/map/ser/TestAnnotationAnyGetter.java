package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class TestAnnotationAnyGetter
    extends BaseMapTest
{
    /*
    /*********************************************************
    /* Helper bean classes
    /*********************************************************
     */

    static class Bean
    {
        final static Map<String,Boolean> extra = new HashMap<String,Boolean>();
        static {
            extra.put("a", Boolean.TRUE);
        }
        
        public int getX() { return 3; }

        @JsonAnyGetter
        public Map<String,Boolean> getExtra() { return extra; }
    }

    static class AnyOnlyBean
    {
        @JsonAnyGetter
        public Map<String,Integer> any() {
            HashMap<String,Integer> map = new HashMap<String,Integer>();
            map.put("a", 3);
            return map;
        }
    }
    
    /*
    /*********************************************************
    /* Test cases
    /*********************************************************
     */

    public void testSimpleJsonValue() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String json = serializeAsString(m, new Bean());
        Map<?,?> map = m.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("x"));
        assertEquals(Boolean.TRUE, map.get("a"));
    }

    // [JACKSON-392]
    public void testAnyOnly() throws Exception
    {
        ObjectMapper m;

        // First, with normal fail settings:
        m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, true);
        String json = serializeAsString(m, new AnyOnlyBean());
        assertEquals("{\"a\":3}", json);

        // then without fail
        m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        json = serializeAsString(m, new AnyOnlyBean());
        assertEquals("{\"a\":3}", json);
    }
    
}
