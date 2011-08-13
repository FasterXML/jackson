package org.codehaus.jackson.map.tree;

import main.BaseTest;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;

/**
 * This unit test suite tries to verify that ObjectMapper
 * can properly parse JSON and bind contents into appropriate
 * JsonNode instances.
 */
public class TestTreeMapperDeserializer
    extends BaseTest
{
	public void testSimple()
        throws Exception
    {
        final String JSON = SAMPLE_DOC_JSON_SPEC;

        ObjectMapper mapper = new ObjectMapper();

        for (int type = 0; type < 2; ++type) {
            JsonNode result;

            if (type == 0) {
                result = mapper.readTree(new StringReader(JSON));
            } else {
                result = mapper.readTree(JSON);
            }

            assertType(result, ObjectNode.class);
            assertEquals(1, result.size());
            assertTrue(result.isObject());
            
            ObjectNode main = (ObjectNode) result;
            assertEquals("Image", main.getFieldNames().next());
            JsonNode ob = main.getElements().next();
            assertType(ob, ObjectNode.class);
            ObjectNode imageMap = (ObjectNode) ob;
            
            assertEquals(5, imageMap.size());
            ob = imageMap.get("Width");
            assertTrue(ob.isIntegralNumber());
            assertFalse(ob.isFloatingPointNumber());
            assertEquals(SAMPLE_SPEC_VALUE_WIDTH, ob.getIntValue());
            ob = imageMap.get("Height");
            assertTrue(ob.isIntegralNumber());
            assertEquals(SAMPLE_SPEC_VALUE_HEIGHT, ob.getIntValue());
            
            ob = imageMap.get("Title");
            assertTrue(ob.isTextual());
            assertEquals(SAMPLE_SPEC_VALUE_TITLE, ob.getTextValue());
            
            ob = imageMap.get("Thumbnail");
            assertType(ob, ObjectNode.class);
            ObjectNode tn = (ObjectNode) ob;
            ob = tn.get("Url");
            assertTrue(ob.isTextual());
            assertEquals(SAMPLE_SPEC_VALUE_TN_URL, ob.getTextValue());
            ob = tn.get("Height");
            assertTrue(ob.isIntegralNumber());
            assertEquals(SAMPLE_SPEC_VALUE_TN_HEIGHT, ob.getIntValue());
            ob = tn.get("Width");
            assertTrue(ob.isTextual());
            assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, ob.getTextValue());
            
            ob = imageMap.get("IDs");
            assertTrue(ob.isArray());
            ArrayNode idList = (ArrayNode) ob;
            assertEquals(4, idList.size());
            assertEquals(4, calcLength(idList.getElements()));
            assertEquals(4, calcLength(idList.iterator()));
            {
                int[] values = new int[] {
                    SAMPLE_SPEC_VALUE_TN_ID1,
                    SAMPLE_SPEC_VALUE_TN_ID2,
                    SAMPLE_SPEC_VALUE_TN_ID3,
                    SAMPLE_SPEC_VALUE_TN_ID4
                };
                for (int i = 0; i < values.length; ++i) {
                    assertEquals(values[i], idList.get(i).getIntValue());
                }
                int i = 0;
                for (JsonNode n : idList) {
                    assertEquals(values[i], n.getIntValue());
                    ++i;
                }
            }
        }
    }

    public void testBoolean()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = mapper.readTree("true\n");
        assertFalse(result.isNull());
        assertFalse(result.isNumber());
        assertFalse(result.isTextual());
        assertTrue(result.isBoolean());
        assertType(result, BooleanNode.class);
        assertTrue(result.getBooleanValue());
        assertEquals("true", result.asText());
        assertFalse(result.isMissingNode());

        // also, equality should work ok
        assertEquals(result, BooleanNode.valueOf(true));
        assertEquals(result, BooleanNode.getTrue());
    }

    public void testDouble()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        double value = 3.04;
        JsonNode result = mapper.readTree(String.valueOf(value));
        assertTrue(result.isNumber());
        assertFalse(result.isNull());
        assertType(result, DoubleNode.class);
        assertTrue(result.isFloatingPointNumber());
        assertTrue(result.isDouble());
        assertFalse(result.isInt());
        assertFalse(result.isLong());
        assertFalse(result.isIntegralNumber());
        assertFalse(result.isTextual());
        assertFalse(result.isMissingNode());

        assertEquals(value, result.getDoubleValue());
        assertEquals(value, result.getNumberValue().doubleValue());
        assertEquals((int) value, result.getIntValue());
        assertEquals((long) value, result.getLongValue());
        assertEquals(String.valueOf(value), result.asText());

        // also, equality should work ok
        assertEquals(result, DoubleNode.valueOf(value));
    }

    public void testInt()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        int value = -90184;
        JsonNode result = mapper.readTree(String.valueOf(value));
        assertTrue(result.isNumber());
        assertTrue(result.isIntegralNumber());
        assertTrue(result.isInt());
        assertType(result, IntNode.class);
        assertFalse(result.isLong());
        assertFalse(result.isFloatingPointNumber());
        assertFalse(result.isDouble());
        assertFalse(result.isNull());
        assertFalse(result.isTextual());
        assertFalse(result.isMissingNode());

        assertEquals(value, result.getNumberValue().intValue());
        assertEquals(value, result.getIntValue());
        assertEquals(String.valueOf(value), result.asText());
        assertEquals((double) value, result.getDoubleValue());
        assertEquals((long) value, result.getLongValue());

        // also, equality should work ok
        assertEquals(result, IntNode.valueOf(value));
    }

    public void testLong()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // need to use something being 32-bit value space
        long value = 12345678L << 32;
        JsonNode result = mapper.readTree(String.valueOf(value));
        assertTrue(result.isNumber());
        assertTrue(result.isIntegralNumber());
        assertTrue(result.isLong());
        assertType(result, LongNode.class);
        assertFalse(result.isInt());
        assertFalse(result.isFloatingPointNumber());
        assertFalse(result.isDouble());
        assertFalse(result.isNull());
        assertFalse(result.isTextual());
        assertFalse(result.isMissingNode());

        assertEquals(value, result.getNumberValue().longValue());
        assertEquals(value, result.getLongValue());
        assertEquals(String.valueOf(value), result.asText());
        assertEquals((double) value, result.getDoubleValue());

        // also, equality should work ok
        assertEquals(result, LongNode.valueOf(value));
    }

    public void testNull()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = mapper.readTree("   null ");
        // should not get java null, but NullNode...
        assertNotNull(result);
        assertTrue(result.isNull());
        assertFalse(result.isNumber());
        assertFalse(result.isTextual());
        assertEquals("null", result.asText());

        // also, equality should work ok
        assertEquals(result, NullNode.instance);
    }

    public void testDecimalNode()
        throws Exception
    {
        // no "natural" way to get it, must construct
        BigDecimal value = new BigDecimal("0.1");
        JsonNode result = DecimalNode.valueOf(value);

        assertFalse(result.isArray());
        assertFalse(result.isObject());
        assertTrue(result.isNumber());
        assertFalse(result.isIntegralNumber());
        assertFalse(result.isLong());
        assertType(result, DecimalNode.class);
        assertFalse(result.isInt());
        assertTrue(result.isFloatingPointNumber());
        assertTrue(result.isBigDecimal());
        assertFalse(result.isDouble());
        assertFalse(result.isNull());
        assertFalse(result.isTextual());
        assertFalse(result.isMissingNode());

        assertEquals(value, result.getNumberValue());
        assertEquals(value.toString(), result.asText());

        // also, equality should work ok
        assertEquals(result, DecimalNode.valueOf(value));
    }

    public void testSimpleArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        assertTrue(result.isArray());
        assertType(result, ArrayNode.class);

        assertFalse(result.isObject());
        assertFalse(result.isNumber());
        assertFalse(result.isNull());
        assertFalse(result.isTextual());

        // and let's add stuff...
        result.add(false);
        result.insertNull(0);

        // should be equal to itself no matter what
        assertEquals(result, result);
        assertFalse(result.equals(null)); // but not to null

        // plus see that we can access stuff
        assertEquals(NullNode.instance, result.path(0));
        assertEquals(NullNode.instance, result.get(0));
        assertEquals(BooleanNode.FALSE, result.path(1));
        assertEquals(BooleanNode.FALSE, result.get(1));
        assertEquals(2, result.size());

        assertNull(result.get(-1));
        assertNull(result.get(2));
        JsonNode missing = result.path(2);
        assertTrue(missing.isMissingNode());
        assertTrue(result.path(-100).isMissingNode());

        // then construct and compare
        ArrayNode array2 = mapper.createArrayNode();
        array2.addNull();
        array2.add(false);
        assertEquals(result, array2);

        // plus remove entries
        JsonNode rm1 = array2.remove(0);
        assertEquals(NullNode.instance, rm1);
        assertEquals(1, array2.size());
        assertEquals(BooleanNode.FALSE, array2.get(0));
        assertFalse(result.equals(array2));

        JsonNode rm2 = array2.remove(0);
        assertEquals(BooleanNode.FALSE, rm2);
        assertEquals(0, array2.size());
    }

    /**
     * Type mappers should be able to gracefully deal with end of
     * input.
     */
    public void testEOF() throws Exception
    {
        String JSON =
            "{ \"key\": [ { \"a\" : { \"name\": \"foo\",  \"type\": 1\n"
            +"},  \"type\": 3, \"url\": \"http://www.google.com\" } ],\n"
            +"\"name\": \"xyz\", \"type\": 1, \"url\" : null }\n  "
            ;
        JsonFactory jf = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = jf.createJsonParser(new StringReader(JSON));
        JsonNode result = mapper.readTree(jp);

        assertTrue(result.isObject());
        assertEquals(4, result.size());

        assertNull(mapper.readTree(jp));
    }

    public void testMultiple() throws Exception
    {
        String JSON = "12  \"string\" [ 1, 2, 3 ]";
        JsonFactory jf = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = jf.createJsonParser(new StringReader(JSON));
        JsonNode result = mapper.readTree(jp);

        assertTrue(result.isIntegralNumber());
        assertTrue(result.isInt());
        assertFalse(result.isTextual());
        assertEquals(12, result.getIntValue());

        result = mapper.readTree(jp);
        assertTrue(result.isTextual());
        assertFalse(result.isIntegralNumber());
        assertFalse(result.isInt());
        assertEquals("string", result.getTextValue());

        result = mapper.readTree(jp);
        assertTrue(result.isArray());
        assertEquals(3, result.size());

        assertNull(mapper.readTree(jp));
    }

    /**
     * Let's also verify behavior of "MissingNode" -- one needs to be able
     * to traverse such bogus nodes with appropriate methods.
     */
    @SuppressWarnings("unused")
    public void testMissingNode()
        throws Exception
    {
        String JSON = "[ { }, [ ] ]";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = mapper.readTree(new StringReader(JSON));

        assertTrue(result.isContainerNode());
        assertTrue(result.isArray());
        assertEquals(2, result.size());

        int count = 0;
        for (JsonNode node : result) {
            ++count;
        }
        assertEquals(2, count);

        Iterator<JsonNode> it = result.iterator();

        JsonNode onode = it.next();
        assertTrue(onode.isContainerNode());
        assertTrue(onode.isObject());
        assertEquals(0, onode.size());
        assertFalse(onode.isMissingNode()); // real node
        assertNull(onode.getTextValue());

        // how about dereferencing?
        assertNull(onode.get(0));
        JsonNode dummyNode = onode.path(0);
        assertNotNull(dummyNode);
        assertTrue(dummyNode.isMissingNode());
        assertNull(dummyNode.get(3));
        assertNull(dummyNode.get("whatever"));
        JsonNode dummyNode2 = dummyNode.path(98);
        assertNotNull(dummyNode2);
        assertTrue(dummyNode2.isMissingNode());
        JsonNode dummyNode3 = dummyNode.path("field");
        assertNotNull(dummyNode3);
        assertTrue(dummyNode3.isMissingNode());

        // and same for the array node

        JsonNode anode = it.next();
        assertTrue(anode.isContainerNode());
        assertTrue(anode.isArray());
        assertFalse(anode.isMissingNode()); // real node
        assertEquals(0, anode.size());

        assertNull(anode.get(0));
        dummyNode = anode.path(0);
        assertNotNull(dummyNode);
        assertTrue(dummyNode.isMissingNode());
        assertNull(dummyNode.get(0));
        assertNull(dummyNode.get("myfield"));
        dummyNode2 = dummyNode.path(98);
        assertNotNull(dummyNode2);
        assertTrue(dummyNode2.isMissingNode());
        dummyNode3 = dummyNode.path("f");
        assertNotNull(dummyNode3);
        assertTrue(dummyNode3.isMissingNode());
    }

    public void testArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final String JSON = "[[[-0.027512,51.503221],[-0.008497,51.503221],[-0.008497,51.509744],[-0.027512,51.509744]]]";

        JsonNode n = mapper.readTree(JSON);
        assertNotNull(n);
        assertTrue(n.isArray());
        ArrayNode an = (ArrayNode) n;
        assertEquals(1, an.size());
        ArrayNode an2 = (ArrayNode) n.get(0);
        assertTrue(an2.isArray());
        assertEquals(4, an2.size());
    }
    
    /*
    /**********************************************
    /* Helper methods
    /**********************************************
     */

    private int calcLength(Iterator<JsonNode> it)
    {
        int count = 0;
        while (it.hasNext()) {
            it.next();
            ++count;
        }
        return count;
    }
}

