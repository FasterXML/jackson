package org.codehaus.jackson.node;

import java.io.IOException;

import static org.junit.Assert.*;

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

    // Test for [JACKSON-631]
    public void testBase64Text() throws Exception
    {
        // let's actually iterate over sets of encoding modes, lengths
        
        final int[] LENS = { 1, 2, 3, 4, 7, 9, 32, 33, 34, 35 };
        final Base64Variant[] VARIANTS = {
                Base64Variants.MIME,
                Base64Variants.MIME_NO_LINEFEEDS,
                Base64Variants.MODIFIED_FOR_URL,
                Base64Variants.PEM
        };

        for (int len : LENS) {
            byte[] input = new byte[len];
            for (int i = 0; i < input.length; ++i) {
                input[i] = (byte) i;
            }
            for (Base64Variant variant : VARIANTS) {
                TextNode n = new TextNode(variant.encode(input));
                byte[] data = null;
                try {
                    data = n.getBinaryValue(variant);
                } catch (Exception e) {
                    throw new IOException("Failed (variant "+variant+", data length "+len+"): "+e.getMessage(), e);
                }
                assertNotNull(data);
                assertArrayEquals(data, input);
            }
        }
    }
}
