package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Tests for checking that static methods are not recognized as accessors
 * for properties
 */
public class TestStatics
    extends BaseMapTest
{
    static class Bean
    {
        int _x;

        public static void setX(int value) { throw new Error("Should NOT call static method"); }
        
        @JsonProperty("x") public void assignX(int x) { _x = x; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testSimpleIgnore() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // should not care about static setter...
        Bean result = m.readValue("{ \"x\":3}", Bean.class);
        assertEquals(3, result._x);
    }
}
