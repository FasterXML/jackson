package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.map.*;

/**
 * 
 * @author tatu
 * @since 1.5
 */
public class TestRootType
    extends BaseMapTest
{
    /*
    /***********************************************
    /* Annotated helper classes
    /***********************************************
     */

    static class BaseType {
        public String a = "a";

        public int getB() { return 3; }
    }

    static class SubType extends BaseType {
        public String a2 = "x";
        
        public boolean getB2() { return true; }
    }
    
    /*
    /***********************************************
    /* Main tests
    /***********************************************
     */
    
    @SuppressWarnings("unchecked")
    public void testSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SubType bean = new SubType();

        // first, test with dynamically detected type
        Map<String,Object> result = writeAndMap(mapper, bean);
        assertEquals(4, result.size());
        assertEquals("a", result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
        assertEquals("x", result.get("a2"));
        assertEquals(Boolean.TRUE, result.get("b2"));

        // and then using specified typed writer
        ObjectWriter w = mapper.typedWriter(BaseType.class);
        String json = w.writeValueAsString(bean);
        result = (Map)mapper.readValue(json, Map.class);
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
    }
}
