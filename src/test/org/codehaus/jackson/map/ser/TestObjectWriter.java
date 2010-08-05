package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.map.*;

/**
 * Unit tests for checking features added to {@link ObjectWriter}, such
 * as adding of explicit pretty printer.
 * 
 * @since 1.6
 */
public class TestObjectWriter
    extends BaseMapTest
{
    public void testPrettyPrinter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer();
        HashMap<String, Integer> data = new HashMap<String,Integer>();
        data.put("a", 1);
        
        // default: no indentation
        assertEquals("{\"a\":1}", writer.writeValueAsString(data));

        // and then with standard
        writer = writer.withDefaultPrettyPrinter();
        assertEquals("{\n  \"a\" : 1\n}", writer.writeValueAsString(data));

        // and finally, again without indentation
        writer = writer.withPrettyPrinter(null);
        assertEquals("{\"a\":1}", writer.writeValueAsString(data));
    }
}
