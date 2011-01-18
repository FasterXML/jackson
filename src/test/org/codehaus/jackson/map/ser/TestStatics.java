package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite verifies that static fields and methods are
 * ignored wrt serialization
 */
public class TestStatics
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    final static class FieldBean
    {
        public int x = 1;

        public static int y = 2;

        // not even @JsonProperty should make statics usable...
        @JsonProperty public static int z = 3;
    }

    final static class GetterBean
    {
        public int getX() { return 3; }

        public static int getA() { return -3; }

        // not even @JsonProperty should make statics usable...
        @JsonProperty public static int getFoo() { return 123; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testStaticFields() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new FieldBean());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
    }

    public void testStaticMethods() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new GetterBean());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
    }
}
