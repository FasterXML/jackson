package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;

/**
 * This unit test suite tries to verify that JsonNode-based trees
 * can be serialized as expected
 */
public class TestTreeSerialization
    extends BaseMapTest
{
    final static class Bean {
        public String getX() { return "y"; }
        public int getY() { return 13; }
    }

    @SuppressWarnings("unchecked")
	public void testSimpleViaObjectMapper()
        throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        // also need tree mapper to construct tree to serialize
        ObjectNode n = mapper.getNodeFactory().objectNode();
        n.put("number", 15);
        n.put("string", "abc");
        ObjectNode n2 = n.putObject("ob");
        n2.putArray("arr");
        StringWriter sw = new StringWriter();
        JsonGenerator jg = mapper.getJsonFactory().createJsonGenerator(sw);
        mapper.writeTree(jg, n);

        Map<String,Object> result = (Map<String,Object>) mapper.readValue(sw.toString(), Map.class);

        assertEquals(3, result.size());
        assertEquals("abc", result.get("string"));
        assertEquals(Integer.valueOf(15), result.get("number"));
        Map<String,Object> ob = (Map<String,Object>) result.get("ob");
        assertEquals(1, ob.size());
        List<Object> list = (List<Object>) ob.get("arr");
        assertEquals(0, list.size());
    }

    /**
     * Simple test to verify that POJONodes (JsonNode wrapper around
     * any old Java object) work with serialization
     */
    @SuppressWarnings("unchecked")
	public void testPOJOString()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // also need tree mapper to construct tree to serialize
        ObjectNode n = mapper.getNodeFactory().objectNode();
        n.put("pojo", mapper.getNodeFactory().POJONode("abc"));
        StringWriter sw = new StringWriter();
        JsonGenerator jg = mapper.getJsonFactory().createJsonGenerator(sw);
        mapper.writeTree(jg, n);
        Map<String,Object> result = (Map<String,Object>) mapper.readValue(sw.toString(), Map.class);
        assertEquals(1, result.size());
        assertEquals("abc", result.get("pojo"));
    }

    @SuppressWarnings("unchecked")
    public void testPOJOIntArray()
        throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode n = mapper.getNodeFactory().objectNode();
        n.put("pojo", mapper.getNodeFactory().POJONode(new int[] { 1, 2, 3 }));
        StringWriter sw = new StringWriter();
        JsonGenerator jg = mapper.getJsonFactory().createJsonGenerator(sw);
        mapper.writeTree(jg, n);

        Map<String,Object> result = (Map<String,Object>) mapper.readValue(sw.toString(), Map.class);

        assertEquals(1, result.size());
        // int array becomes a list when mapped to general Object:
        List<Object> list = (List<Object>) result.get("pojo");
        assertEquals(3, list.size());
        for (int i = 0; i < 3; ++i) {
            assertEquals(Integer.valueOf(i+1), list.get(i));
        }
    }

    @SuppressWarnings("unchecked")
    public void testPOJOBean()
        throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        // also need tree mapper to construct tree to serialize
        ObjectNode n = mapper.getNodeFactory().objectNode();
        n.put("pojo", mapper.getNodeFactory().POJONode(new Bean()));
        StringWriter sw = new StringWriter();
        JsonGenerator jg = mapper.getJsonFactory().createJsonGenerator(sw);
        mapper.writeTree(jg, n);

        Map<String,Object> result = (Map<String,Object>) mapper.readValue(sw.toString(), Map.class);

        assertEquals(1, result.size());
        Map<String,Object> bean = (Map<String,Object>) result.get("pojo");
        assertEquals(2, bean.size());
        assertEquals("y", bean.get("x"));
        assertEquals(Integer.valueOf(13), bean.get("y"));
    }
}
