package org.codehaus.jackson.node;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * Unit tests for verifying functionality of {@link JsonNode} methods that
 * convert values to other types
 *
 * @since 1.7
 */
public class TestConversions extends BaseMapTest
{
    static class Root {
        public Leaf leaf;
    }

    static class Leaf {
        public int value;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
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
    
    // Deserializer to trigger the problem described in [JACKSON-554]
    public static class LeafDeserializer extends JsonDeserializer<Leaf>
    {
        @Override
        public Leaf deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            JsonNode tree = jp.readValueAsTree();
            Leaf leaf = new Leaf();
            leaf.value = tree.get("value").getIntValue();
            return leaf;
        }
    }

    // MixIn for [JACKSON-554]
    @JsonDeserialize(using = LeafDeserializer.class)
    public static class LeafMixIn
    {
    }

    // Test for [JACKSON-554]
    public void testTreeToValue() throws Exception
    {
        String JSON = "{\"leaf\":{\"value\":13}}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().addMixInAnnotations(Leaf.class, LeafMixIn.class);
        JsonNode root = mapper.readTree(JSON);
        // Ok, try converting to bean using two mechanisms
        Root r1 = mapper.treeToValue(root, Root.class);
        assertNotNull(r1);
        assertEquals(13, r1.leaf.value);
        Root r2 = mapper.readValue(root, Root.class);
        assertEquals(13, r2.leaf.value);
    }
}
