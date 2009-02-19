package org.codehaus.jackson.map;

import main.BaseTest;

import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Unit tests to verify that Json Objects map property to Map-like
 * ObjectNodes.
 */
public class TestTreeMapperMaps
    extends BaseTest
{
    public void testSimpleObject() throws Exception
    {
        String JSON = "{ \"key\" : 1, \"b\" : \"x\" }";
        TreeMapper mapper = new TreeMapper();
        JsonNode root = mapper.readTree(JSON);

        // basic properties first:
        assertFalse(root.isValueNode());
        assertTrue(root.isContainerNode());
        assertFalse(root.isArray());
        assertTrue(root.isObject());
        assertEquals(2, root.size());

        // Related to [JACKSON-50]:
        Iterator<JsonNode> it = root.iterator();
        assertNotNull(it);
        assertTrue(it.hasNext());
        JsonNode n = it.next();
        assertNotNull(n);
        assertEquals(mapper.numberNode(1), n);

        assertTrue(it.hasNext());
        n = it.next();
        assertNotNull(n);
        assertEquals(mapper.textNode("x"), n);

        assertFalse(it.hasNext());

        // Ok, then, let's traverse via extended interface
        ObjectNode obNode = (ObjectNode) root;
        Iterator<Map.Entry<String,JsonNode>> fit = obNode.getFields();
        // we also know that LinkedHashMap is used, i.e. order preserved
        assertTrue(fit.hasNext());
        Map.Entry<String,JsonNode> en = fit.next();
        assertEquals("key", en.getKey());
        assertEquals(mapper.numberNode(1), en.getValue());

        assertTrue(fit.hasNext());
        en = fit.next();
        assertEquals("b", en.getKey());
        assertEquals(mapper.textNode("x"), en.getValue());

        // Plus: we should be able to modify the node via iterator too:
        fit.remove();
        assertEquals(1, obNode.size());
        assertEquals(mapper.numberNode(1), root.get("key"));
        assertNull(root.get("b"));
    }
}
