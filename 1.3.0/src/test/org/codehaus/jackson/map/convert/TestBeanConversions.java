package org.codehaus.jackson.map.convert;

import java.util.*;

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
}
