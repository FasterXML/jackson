package org.codehaus.jackson.map.tree;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;

/**
 * This unit test suite tries to verify that JsonNode-based trees
 * can be deserialized as expected.
 */
public class TestTreeDeserialization
    extends BaseMapTest
{
    final static class Bean {
        int _x;
        JsonNode _node;

        public void setX(int x) { _x = x; }
        public void setNode(JsonNode n) { _node = n; }
    }

    /**
     * This test checks that is possible to mix "regular" Java objects
     * and JsonNode.
     */
    public void testMixed() throws IOException
    {
        ObjectMapper om = new ObjectMapper();
        String JSON = "{\"node\" : { \"a\" : 3 }, \"x\" : 9 }";
        Bean bean = om.readValue(JSON, Bean.class);

        assertEquals(9, bean._x);
        JsonNode n = bean._node;
        assertNotNull(n);
        assertEquals(1, n.size());
        ObjectNode on = (ObjectNode) n;
        assertEquals(3, on.get("a").getIntValue());
    }

    /// Verifying [JACKSON-143]
    public void testArrayNodeEquality()
    {
        ArrayNode n1 = new ArrayNode(null);
        ArrayNode n2 = new ArrayNode(null);

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));

        n1.add(TextNode.valueOf("Test"));

        assertFalse(n1.equals(n2));
        assertFalse(n2.equals(n1));

        n2.add(TextNode.valueOf("Test"));

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));
    }

    public void testObjectNodeEquality()
    {
        ObjectNode n1 = new ObjectNode(null);
        ObjectNode n2 = new ObjectNode(null);

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));

        n1.put("x", TextNode.valueOf("Test"));

        assertFalse(n1.equals(n2));
        assertFalse(n2.equals(n1));

        n2.put("x", TextNode.valueOf("Test"));

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));
    }
}
