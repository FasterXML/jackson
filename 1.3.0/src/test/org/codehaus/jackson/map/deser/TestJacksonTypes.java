package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for those Jackson types we want to ensure can be deserialized.
 */
public class TestJacksonTypes
    extends org.codehaus.jackson.map.BaseMapTest
{
    public void testJsonLocation() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // note: source reference is untyped, only String guaranteed to work
        JsonLocation loc = new JsonLocation("whatever",  -1, -1, 100, 13);
        // Let's use serializer here; goal is round-tripping
        String ser = serializeAsString(m, loc);
        JsonLocation result = m.readValue(ser, JsonLocation.class);
        assertEquals("Did not correctly deserialize standard serialization '"+ser+"'",
                     loc, result);
    }

    // doesn't really belong here but...
    public void testJsonLocationProps()
    {
        JsonLocation loc = new JsonLocation(null,  -1, -1, 100, 13);
        assertTrue(loc.equals(loc));
        assertFalse(loc.equals(null));
        assertFalse(loc.equals("abx"));

        // should we check it's not 0?
        loc.hashCode();
    }
}
