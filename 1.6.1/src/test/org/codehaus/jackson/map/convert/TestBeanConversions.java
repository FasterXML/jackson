package org.codehaus.jackson.map.convert;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.*;

public class TestBeanConversions
    extends org.codehaus.jackson.map.BaseMapTest
{
    final ObjectMapper mapper = new ObjectMapper();

    static class Point {
        public int x, y;

        public int z = -13;
    }

    static class PointStrings {
        public final String x, y;

        public PointStrings(String x, String y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class BooleanBean {
        public boolean boolProp;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    public void testBeanConvert()
    {
        // should have no problems convert between compatible beans...
        PointStrings input = new PointStrings("37", "-9");
        Point point = mapper.convertValue(input, Point.class);
        assertEquals(37, point.x);
        assertEquals(-9, point.y);
        // z not included in input, will be whatever default constructor provides
        assertEquals(-13, point.z);
    }

    static class WrapperBean {
        public BooleanBean x;
    }
    
    // For [JACKSON-371]; verify that we know property that caused issue...
    // (note: not optimal place for test, but will have to do for now)
    public void testErrorReporting() throws Exception
    {
        //String json = "{\"boolProp\":\"oops\"}";
        ObjectMapper mapper = new ObjectMapper();
        
        // First: unknown property
        try {
            mapper.readValue("{\"unknownProp\":true}", BooleanBean.class);
        } catch (JsonProcessingException e) {
            verifyException(e, "unknownProp");
        }

        // then bad conversion
        try {
            mapper.readValue("{\"boolProp\":\"foobar\"}", BooleanBean.class);
        } catch (JsonProcessingException e) {
            verifyException(e, "boolProp");
        }
    }

}
