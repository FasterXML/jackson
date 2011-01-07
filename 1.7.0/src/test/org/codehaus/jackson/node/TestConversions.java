package org.codehaus.jackson.node;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying functionality of {@link JsonNode} methods that
 * convert values to other types
 *
 * @since 1.7
 */
public class TestConversions extends BaseMapTest
{
    public void testAsInt() throws Exception
    {
        assertEquals(9, IntNode.valueOf(9).getValueAsInt());
        assertEquals(7, LongNode.valueOf(7L).getValueAsInt());
        assertEquals(13, new TextNode("13").getValueAsInt());
        assertEquals(0, new TextNode("foobar").getValueAsInt());
        assertEquals(27, new TextNode("foobar").getValueAsInt(27));
        assertEquals(1, BooleanNode.TRUE.getValueAsInt());
    }

    public void testAsBoolean() throws Exception
    {
        assertEquals(false, BooleanNode.FALSE.getValueAsBoolean());
        assertEquals(true, BooleanNode.TRUE.getValueAsBoolean());
        assertEquals(false, IntNode.valueOf(0).getValueAsBoolean());
        assertEquals(true, IntNode.valueOf(1).getValueAsBoolean());
        assertEquals(false, LongNode.valueOf(0).getValueAsBoolean());
        assertEquals(true, LongNode.valueOf(-34L).getValueAsBoolean());
        assertEquals(true, new TextNode("true").getValueAsBoolean());
        assertEquals(false, new TextNode("false").getValueAsBoolean());
        assertEquals(false, new TextNode("barf").getValueAsBoolean());
        assertEquals(true, new TextNode("barf").getValueAsBoolean(true));

        assertEquals(true, new POJONode(Boolean.TRUE).getValueAsBoolean());
    }
}
