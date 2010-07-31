package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

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
        public Map<String,Boolean> extra() { return extra; }
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

}
