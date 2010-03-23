package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for those Jackson types we want to ensure can be serialized.
 */
public class TestJacksonTypes
    extends BaseMapTest
{
    public void testLocation() throws IOException
    {
        JsonLocation loc = new JsonLocation(new File("/tmp/test.json"),
                                            -1, 100, 13);
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> result = writeAndMap(mapper, loc);
        assertEquals(5, result.size());
        assertEquals("/tmp/test.json", result.get("sourceRef"));
        assertEquals(Integer.valueOf(-1), result.get("charOffset"));
        assertEquals(Integer.valueOf(-1), result.get("byteOffset"));
        assertEquals(Integer.valueOf(100), result.get("lineNr"));
        assertEquals(Integer.valueOf(13), result.get("columnNr"));

    }
}
