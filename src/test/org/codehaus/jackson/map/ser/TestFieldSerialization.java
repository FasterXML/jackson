package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that field-backed properties can also be serialized
 * (since version 1.1) as well as getter-accessible properties.
 */
public class TestFieldSerialization
    extends BaseMapTest
{
    /*
    //////////////////////////////////////////////
    // Annotated helper classes
    //////////////////////////////////////////////
     */

    public class SimpleFieldBean
    {
        public int x, y;

        // not auto-detectable, not public
        int z;

        // ignored, not detectable either
        @JsonIgnore public int a;
    }

    @JsonAutoDetect(JsonMethod.SETTER)
    public class NoAutoDetectBean
    {
        // not auto-detectable any more
        public int x;

        @JsonProperty("z")
        public int _z;
    }

    /*
    //////////////////////////////////////////////
    // Main tests
    //////////////////////////////////////////////
     */

    public void testSimpleAutoDetect() throws Exception
    {
        SimpleFieldBean bean = new SimpleFieldBean();
        // let's set x, leave y as is
        bean.x = 13;
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, bean);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(13), result.get("x"));
        assertEquals(Integer.valueOf(0), result.get("y"));
    }

    public void testNoAutoDetect() throws Exception
    {
        NoAutoDetectBean bean = new NoAutoDetectBean();
        bean._z = -4;
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, bean);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(-4), result.get("z"));
    }
}
