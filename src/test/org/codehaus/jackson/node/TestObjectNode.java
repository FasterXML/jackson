package org.codehaus.jackson.node;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Additional tests for {@link ObjectNode} container class.
 */
public class TestObjectNode
    extends BaseMapTest
{
    public void testBasics()
    {
        ObjectNode n = new ObjectNode(JsonNodeFactory.instance);
        assertStandardEquals(n);

        assertFalse(n.getElements().hasNext());
        assertFalse(n.getFields().hasNext());
        assertNull(n.get("a"));
        assertTrue(n.get("a").isMissingNode());

        TextNode text = TextNode.valueOf("x");
        n.put("a", text);
        assertEquals(1, n.size());
        assertTrue(n.getElements().hasNext());
        assertTrue(n.getFields().hasNext());
        assertSame(text, n.get("a"));
        assertSame(text, n.path("a"));
        assertNull(n.get("b"));
        assertNull(n.get(0)); // not used with objects

        ObjectNode n2 = new ObjectNode(JsonNodeFactory.instance);
        n2.put("b", 13);
        assertFalse(n.equals(n2));
        n.putAll(n2);
        assertEquals(2, n.size());
        n.put("null", (JsonNode)null);
        assertEquals(3, n.size());
        // should replace, not add
        n.put("null", "notReallNull");
        assertEquals(3, n.size());
        assertNotNull(n.remove("null"));

        Map<String,JsonNode> nodes = new HashMap<String,JsonNode>();
        nodes.put("d", text);
        n.putAll(nodes);
        assertEquals(4, n.size());

        n.removeAll();
        assertEquals(0, n.size());
    }
}
