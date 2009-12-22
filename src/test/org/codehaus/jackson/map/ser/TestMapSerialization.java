package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class TestMapSerialization
    extends BaseMapTest
{
    /*
    ////////////////////////////////////////////////////////////////
    // Helper classes
    ////////////////////////////////////////////////////////////////
     */


    /**
     * Class needed for testing [JACKSON-220]
     */
    @JsonSerialize(using=MapSerializer.class)    
    static class PseudoMap extends LinkedHashMap<String,String>
    {
        public PseudoMap(String... values) {
            for (int i = 0, len = values.length; i < len; i += 2) {
                put(values[i], values[i+1]);
            }
        }
    }

    static class MapSerializer extends JsonSerializer<Map<String,String>>
    {
        public void serialize(Map<String,String> value,
                              JsonGenerator jgen,
                              SerializerProvider provider)
            throws IOException
        {
            // just use standard Map.toString(), output as JSON String
            jgen.writeString(value.toString());
        }
    }
    /*
    ////////////////////////////////////////////////////////////////
    // Test methods
    ////////////////////////////////////////////////////////////////
     */

    /**
     * Test [JACKSON-220]
     */
    public void testMapSerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals("\"{a:b, c:d}\"", m.writeValueAsString(new PseudoMap("a", "b", "c", "d")));
    }
}
