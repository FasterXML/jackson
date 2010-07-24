package org.codehaus.jackson.node;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Additional tests for {@link ArrayNode} container class.
 */
public class TestArrayNode
    extends BaseMapTest
{
    public void testBasics() throws IOException
    {
        ArrayNode n = new ArrayNode(JsonNodeFactory.instance);
        assertStandardEquals(n);
        assertFalse(n.getElements().hasNext());
        assertFalse(n.getFieldNames().hasNext());
        TextNode text = TextNode.valueOf("x");
        n.add(text);
        assertEquals(1, n.size());
        assertFalse(0 == n.hashCode());
        assertTrue(n.getElements().hasNext());
        // no field names for arrays
        assertFalse(n.getFieldNames().hasNext());
        assertNull(n.get("x")); // not used with arrays
        assertTrue(n.path("x").isMissingNode());
        assertSame(text, n.get(0));

        // single element, so:
        assertFalse(n.has("field"));
        assertTrue(n.has(0));
        assertFalse(n.has(1));
        
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
        assertEquals(2, n.size());
        assertTrue(n.get(0).isTextual());

        ArrayList<JsonNode> nodes = new ArrayList<JsonNode>();
        nodes.add(text);
        n.addAll(nodes);
        assertEquals(3, n.size());
        assertNull(n.get(10000));
        assertNull(n.remove(-4));

        TextNode text2 = TextNode.valueOf("b");
        n.insert(0, text2);
        assertEquals(4, n.size());
        assertSame(text2, n.get(0));

        assertNotNull(n.addArray());
        assertEquals(5, n.size());
        n.addPOJO("foo");
        assertEquals(6, n.size());

        // Try serializing it for fun, too...
        JsonGenerator jg = new MappingJsonFactory().createJsonGenerator(new StringWriter());
        n.serialize(jg, null);

        n.removeAll();
        assertEquals(0, n.size());
    }

    public void testAdds()
    {
        ArrayNode n = new ArrayNode(JsonNodeFactory.instance);
        assertNotNull(n.addArray());
        assertNotNull(n.addObject());
        n.addPOJO("foobar");
        n.add(1);
        n.add(1L);
        n.add(0.5);
        n.add(0.5f);
        assertEquals(7, n.size());

        assertNotNull(n.insertArray(0));
        assertNotNull(n.insertObject(0));
        n.insertPOJO(2, "xxx");
        assertEquals(10, n.size());
    }

    /**
     * Test to verify [JACKSON-227]
     */
    public void testNullChecking()
    {
        ArrayNode a1 = JsonNodeFactory.instance.arrayNode();
        ArrayNode a2 = JsonNodeFactory.instance.arrayNode();
        // used to throw NPE before fix:
        a1.addAll(a2);
        assertEquals(0, a1.size());
        assertEquals(0, a2.size());

        a2.addAll(a1);
        assertEquals(0, a1.size());
        assertEquals(0, a2.size());
    }

    /**
     * Another test to verify [JACKSON-227]...
     */
    public void testNullChecking2()
    {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode src = mapper.createArrayNode();
        ArrayNode dest = mapper.createArrayNode();
        src.add("element");
        dest.addAll(src);
    }
    
    public void testParser() throws Exception
    {
        ArrayNode n = new ArrayNode(JsonNodeFactory.instance);
        n.add(123);
        TreeTraversingParser p = new TreeTraversingParser(n, null);
        p.setCodec(null);
        assertNull(p.getCodec());
        assertNotNull(p.getParsingContext());
        assertNotNull(p.getTokenLocation());
        assertNotNull(p.getCurrentLocation());
        assertNull(p.getEmbeddedObject());
        assertNull(p.currentNode());

        //assertNull(p.getNumberType());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        p.skipChildren();
        assertToken(JsonToken.END_ARRAY, p.getCurrentToken());
        p.close();

        p = new TreeTraversingParser(n, null);
        p.nextToken();
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    }
}
