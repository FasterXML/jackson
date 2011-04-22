package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.util.*;

import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that simple exceptions can be serialized.
 */
public class TestExceptionSerialization
    extends BaseMapTest
{
    /*
    ///////////////////////////////////////////////////
    // Tests
    ///////////////////////////////////////////////////
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String TEST = "test exception";
        Map<String,Object> result = writeAndMap(mapper, new Exception(TEST));
        assertEquals(4, result.size());
        assertEquals(TEST, result.get("message"));
        assertNull(result.get("cause"));
        assertEquals(TEST, result.get("localizedMessage"));

        // hmmh. what should we get for stack traces?
        Object traces = result.get("stackTrace");
        if (!(traces instanceof List<?>)) {
            fail("Expected a List for exception member 'stackTrace', got: "+traces);
        }
    }
}
