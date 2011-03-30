package org.codehaus.jackson.node;

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
        assertFalse(n.getFieldNames().hasNext());
        assertNull(n.get("a"));
        assertTrue(n.path("a").isMissingNode());

        TextNode text = TextNode.valueOf("x");
        n.put("a", text);
        assertEquals(1, n.size());
        assertTrue(n.getElements().hasNext());
        assertTrue(n.getFields().hasNext());
        assertTrue(n.getFieldNames().hasNext());
        assertSame(text, n.get("a"));
        assertSame(text, n.path("a"));
        assertNull(n.get("b"));
        assertNull(n.get(0)); // not used with objects

        assertFalse(n.has(0));
        assertTrue(n.has("a"));
        assertFalse(n.has("b"));

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
        assertEquals(2, n.size());

        Map<String,JsonNode> nodes = new HashMap<String,JsonNode>();
        nodes.put("d", text);
        n.putAll(nodes);
        assertEquals(3, n.size());

        n.removeAll();
        assertEquals(0, n.size());
    }

    /**
     * Verify null handling
     */
    public void testNullChecking()
    {
        ObjectNode o1 = JsonNodeFactory.instance.objectNode();
        ObjectNode o2 = JsonNodeFactory.instance.objectNode();
        // used to throw NPE before fix:
        o1.putAll(o2);
        assertEquals(0, o1.size());
        assertEquals(0, o2.size());

        // also: nulls should be converted to NullNodes...
        o1.put("x", (ObjectNode) null);
        JsonNode n = o1.get("x");
        assertNotNull(n);
        assertSame(n, NullNode.instance);

        o1.put("str", (String) null);
        n = o1.get("str");
        assertNotNull(n);
        assertSame(n, NullNode.instance);

        o1.put("d", (BigDecimal) null);
        n = o1.get("d");
        assertNotNull(n);
        assertSame(n, NullNode.instance);
    }

    /**
     * Another test to verify [JACKSON-227]...
     */
    public void testNullChecking2()
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode src = mapper.createObjectNode();
        ObjectNode dest = mapper.createObjectNode();
        src.put("a", "b");
        dest.putAll(src);
    }

    public void testRemove()
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode ob = mapper.createObjectNode();
        ob.put("a", "a");
        ob.put("b", "b");
        ob.put("c", "c");
        assertEquals(3, ob.size());
        assertSame(ob, ob.remove(Arrays.asList("a", "c")));
        assertEquals(1, ob.size());
        assertEquals("b", ob.get("b").getTextValue());
    }

    public void testRetain()
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode ob = mapper.createObjectNode();
        ob.put("a", "a");
        ob.put("b", "b");
        ob.put("c", "c");
        assertEquals(3, ob.size());
        assertSame(ob, ob.retain("a", "c"));
        assertEquals(2, ob.size());
        assertEquals("a", ob.get("a").getTextValue());
        assertNull(ob.get("b"));
        assertEquals("c", ob.get("c").getTextValue());
    }

    // @since 1.8
    public void testValidWith() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        assertEquals("{}", mapper.writeValueAsString(root));
        JsonNode child = root.with("prop");
        assertTrue(child instanceof ObjectNode);
        assertEquals("{\"prop\":{}}", mapper.writeValueAsString(root));
    }

    public void testInvalidWith() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.createArrayNode();
        try { // should not work for non-ObjectNode nodes:
            root.with("prop");
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "not of type ObjectNode");
        }
        // also: should fail of we already have non-object property
        ObjectNode root2 = mapper.createObjectNode();
        root2.put("prop", 13);
        try { // should not work for non-ObjectNode nodes:
            root2.with("prop");
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "has value that is not");
        }
    }
}
