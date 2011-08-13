package org.codehaus.jackson.node;

import java.math.BigInteger;
import java.math.BigDecimal;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Basic tests for {@link JsonNode} base class and some features
 * of implementation classes
 */
public class TestJsonNode
    extends BaseMapTest
{
    public void testBasicsWithNullNode() throws Exception
    {
        // Let's use something that doesn't add much beyond JsonNode base
        NullNode n = NullNode.instance;

        // basic properties
        assertFalse(n.isContainerNode());
        assertFalse(n.isBigDecimal());
        assertFalse(n.isBigInteger());
        assertFalse(n.isBinary());
        assertFalse(n.isBoolean());
        assertFalse(n.isPojo());
        assertFalse(n.isMissingNode());

        // fallback accessors
        assertFalse(n.getBooleanValue());
        assertNull(n.getNumberValue());
        assertEquals(0, n.getIntValue());
        assertEquals(0L, n.getLongValue());
        assertEquals(BigDecimal.ZERO, n.getDecimalValue());
        assertEquals(BigInteger.ZERO, n.getBigIntegerValue());

        assertEquals(0, n.size());
        assertFalse(n.getElements().hasNext());
        assertFalse(n.getFieldNames().hasNext());
        // path is never null; but does point to missing node
        assertNotNull(n.path("xyz"));
        assertTrue(n.path("xyz").isMissingNode());

        assertFalse(n.has("field"));
        assertFalse(n.has(3));

        // 1.6:
        assertNodeNumbers(n, 0, 0.0);
    }

    public void testText()
    {
        assertNull(TextNode.valueOf(null));
        TextNode empty = TextNode.valueOf("");
        assertStandardEquals(empty);
        assertSame(TextNode.EMPTY_STRING_NODE, empty);

        // 1.6:
        assertNodeNumbers(TextNode.valueOf("-3"), -3, -3.0);
        assertNodeNumbers(TextNode.valueOf("17.75"), 17, 17.75);
    
        // [JACKSON-587]
        long value = 127353264013893L;
        TextNode n = TextNode.valueOf(String.valueOf(value));
        assertEquals(value, n.asLong());
        
        // and then with non-numeric input
        assertNodeNumbersForNonNumeric(TextNode.valueOf("foobar"));

    }

    public void testBoolean()
    {
        BooleanNode f = BooleanNode.getFalse();
        assertNotNull(f);
        assertTrue(f.isBoolean());
        assertSame(f, BooleanNode.valueOf(false));
        assertStandardEquals(f);
        assertFalse(f.getBooleanValue());
        assertEquals("false", f.asText());
        assertEquals(JsonToken.VALUE_FALSE, f.asToken());

        // and ditto for true
        BooleanNode t = BooleanNode.getTrue();
        assertNotNull(t);
        assertTrue(t.isBoolean());
        assertSame(t, BooleanNode.valueOf(true));
        assertStandardEquals(t);
        assertTrue(t.getBooleanValue());
        assertEquals("true", t.asText());
        assertEquals(JsonToken.VALUE_TRUE, t.asToken());

        // 1.6:
        assertNodeNumbers(f, 0, 0.0);
        assertNodeNumbers(t, 1, 1.0);
    }

    public void testInt()
    {
        IntNode n = IntNode.valueOf(1);
        assertStandardEquals(n);
        assertTrue(0 != n.hashCode());
        assertEquals(JsonToken.VALUE_NUMBER_INT, n.asToken());
        assertEquals(JsonParser.NumberType.INT, n.getNumberType());
        assertEquals(1, n.getIntValue());
        assertEquals(1L, n.getLongValue());
        assertEquals(BigDecimal.ONE, n.getDecimalValue());
        assertEquals(BigInteger.ONE, n.getBigIntegerValue());
        assertEquals("1", n.asText());

        // 1.6:
        assertNodeNumbers(n, 1, 1.0);
    }

    public void testLong()
    {
        LongNode n = LongNode.valueOf(1L);
        assertStandardEquals(n);
        assertTrue(0 != n.hashCode());
        assertEquals(JsonToken.VALUE_NUMBER_INT, n.asToken());
        assertEquals(JsonParser.NumberType.LONG, n.getNumberType());
        assertEquals(1, n.getIntValue());
        assertEquals(1L, n.getLongValue());
        assertEquals(BigDecimal.ONE, n.getDecimalValue());
        assertEquals(BigInteger.ONE, n.getBigIntegerValue());
        assertEquals("1", n.asText());

        // 1.6:
        assertNodeNumbers(n, 1, 1.0);
    }

    public void testDouble()
    {
        DoubleNode n = DoubleNode.valueOf(0.25);
        assertStandardEquals(n);
        assertTrue(0 != n.hashCode());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, n.asToken());
        assertEquals(JsonParser.NumberType.DOUBLE, n.getNumberType());
        assertEquals(0, n.getIntValue());
        assertEquals(0.25, n.getDoubleValue());
        assertNotNull(n.getDecimalValue());
        assertEquals(BigInteger.ZERO, n.getBigIntegerValue());
        assertEquals("0.25", n.asText());

        // 1.6:
        assertNodeNumbers(DoubleNode.valueOf(4.5), 4, 4.5);
    }

    public void testDecimalNode() throws Exception
    {
        DecimalNode n = DecimalNode.valueOf(BigDecimal.ONE);
        assertStandardEquals(n);
        assertTrue(n.equals(new DecimalNode(BigDecimal.ONE)));
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, n.asToken());
        assertEquals(JsonParser.NumberType.BIG_DECIMAL, n.getNumberType());
        assertTrue(n.isNumber());
        assertFalse(n.isIntegralNumber());
        assertTrue(n.isBigDecimal());
        assertEquals(BigDecimal.ONE, n.getNumberValue());
        assertEquals(1, n.getIntValue());
        assertEquals(1L, n.getLongValue());
        assertEquals(BigDecimal.ONE, n.getDecimalValue());
        assertEquals("1", n.asText());

        // 1.6:
        assertNodeNumbers(n, 1, 1.0);
    }

    public void testBigIntegerNode() throws Exception
    {
        BigIntegerNode n = BigIntegerNode.valueOf(BigInteger.ONE);
        assertStandardEquals(n);
        assertTrue(n.equals(new BigIntegerNode(BigInteger.ONE)));
        assertEquals(JsonToken.VALUE_NUMBER_INT, n.asToken());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, n.getNumberType());
        assertTrue(n.isNumber());
        assertTrue(n.isIntegralNumber());
        assertTrue(n.isBigInteger());
        assertEquals(BigInteger.ONE, n.getNumberValue());
        assertEquals(1, n.getIntValue());
        assertEquals(1L, n.getLongValue());
        assertEquals(BigInteger.ONE, n.getBigIntegerValue());
        assertEquals("1", n.asText());
        
        // 1.6:
        assertNodeNumbers(n, 1, 1.0);

        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
        
        n = BigIntegerNode.valueOf(maxLong);
        assertEquals(Long.MAX_VALUE, n.getLongValue());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode n2 = mapper.readTree(maxLong.toString());
        assertEquals(Long.MAX_VALUE, n2.getLongValue());

        // then over long limit:
        BigInteger beyondLong = maxLong.shiftLeft(2); // 4x max long
        n2 = mapper.readTree(beyondLong.toString());
        assertEquals(beyondLong, n2.getBigIntegerValue());
    }

    public void testBinary() throws Exception
    {
        assertNull(BinaryNode.valueOf(null));
        assertNull(BinaryNode.valueOf(null, 0, 0));

        BinaryNode empty = BinaryNode.valueOf(new byte[1], 0, 0);
        assertSame(BinaryNode.EMPTY_BINARY_NODE, empty);
        assertStandardEquals(empty);

        byte[] data = new byte[3];
        data[1] = (byte) 3;
        BinaryNode n = BinaryNode.valueOf(data, 1, 1);
        data[2] = (byte) 3;
        BinaryNode n2 = BinaryNode.valueOf(data, 2, 1);
        assertTrue(n.equals(n2));
        assertEquals("\"Aw==\"", n.toString());

        assertEquals("AAMD", new BinaryNode(data).asText());

        // 1.6:
        assertNodeNumbersForNonNumeric(n);
    }

    public void testPOJO()
    {
        POJONode n = new POJONode("x"); // not really a pojo but that's ok
        assertStandardEquals(n);
        assertEquals(n, new POJONode("x"));
        assertEquals("x", n.asText());
        // not sure if this is what it'll remain as but:
        assertEquals("x", n.toString());

        assertEquals(new POJONode(null), new POJONode(null));

        // 1.6:
        // default; non-numeric
        assertNodeNumbersForNonNumeric(n);
        // but if wrapping actual number, use it
        assertNodeNumbers(new POJONode(Integer.valueOf(123)), 123, 123.0);
    }

    public void testMissing()
    {
        MissingNode n = MissingNode.getInstance();
        assertEquals(JsonToken.NOT_AVAILABLE, n.asToken());
        assertNull(n.asText());
        assertStandardEquals(n);
        assertEquals("", n.toString());

        // missing acts same as null, so:
        assertNodeNumbers(n, 0, 0.0);
    }
}
