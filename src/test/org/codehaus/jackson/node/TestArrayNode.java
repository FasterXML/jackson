package org.codehaus.jackson.node;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Additional tests for {@link ArrayNode} container class.
 */
public class TestArrayNode
    extends BaseMapTest
{
    public void testBasics()
    {
        ArrayNode n = new ArrayNode(JsonNodeFactory.instance);
        assertStandardEquals(n);
        assertFalse(n.getElements().hasNext());
        TextNode text = TextNode.valueOf("x");
        n.add(text);
        assertEquals(1, n.size());
        assertFalse(0 == n.hashCode());
        assertTrue(n.getElements().hasNext());
        assertNull(n.get("x")); // not used with arrays
        assertTrue(n.path("x").isMissingNode());
        assertSame(text, n.get(0));

        // add null node too
        n.add((JsonNode) null);
        assertEquals(2, n.size());
        assertTrue(n.get(1).isNull());
        // change to text
        n.set(1, text);
        assertSame(text, n.get(1));
        n.set(0, null);
        assertTrue(n.get(0).isNull());

        // and finally, clear it all
        ArrayNode n2 = new ArrayNode(JsonNodeFactory.instance);
        n2.add("foobar");
        assertFalse(n.equals(n2));
        n.addAll(n2);
        assertEquals(3, n.size());

        assertFalse(n.get(0).isTextual());
        assertNotNull(n.remove(0));
        assertTrue(n.get(0).isTextual());

        ArrayList<JsonNode> nodes = new ArrayList<JsonNode>();
        nodes.add(text);
        assertEquals(3, n.size());
        assertNull(n.get(10000));

        assertNotNull(n.addArray());
        assertEquals(4, n.size());
        n.addPOJO("foo");
        assertEquals(5, n.size());

        n.removeAll();
        assertEquals(0, n.size());
    }
}
