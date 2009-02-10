package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.node.*;

/**
 * This unit test suite tries to verify that the "JSON" type
 * mapper can properly parse JSON and bind contents into appropriate
 * JsonNode instances.
 */
public class TestTreeMapperDeserializer
    extends BaseTest
{
    public void testSimple()
        throws Exception
    {
        final String JSON = SAMPLE_DOC_JSON_SPEC;

        JsonFactory jf = new JsonFactory();
        TreeMapper mapper = new TreeMapper();

        for (int type = 0; type < 2; ++type) {
            JsonNode result;

            if (type == 0) {
                result = mapper.readTree(jf.createJsonParser(new StringReader(JSON)));
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
            ob = imageMap.getFieldValue("Width");
            assertTrue(ob.isIntegralNumber());
            assertFalse(ob.isFloatingPointNumber());
            assertEquals(SAMPLE_SPEC_VALUE_WIDTH, ob.getIntValue());
            ob = imageMap.getFieldValue("Height");
            assertTrue(ob.isIntegralNumber());
            assertEquals(SAMPLE_SPEC_VALUE_HEIGHT, ob.getIntValue());
            
            ob = imageMap.getFieldValue("Title");
            assertTrue(ob.isTextual());
            assertEquals(SAMPLE_SPEC_VALUE_TITLE, ob.getTextValue());
            
            ob = imageMap.getFieldValue("Thumbnail");
            assertType(ob, ObjectNode.class);
            ObjectNode tn = (ObjectNode) ob;
            ob = tn.getFieldValue("Url");
            assertTrue(ob.isTextual());
            assertEquals(SAMPLE_SPEC_VALUE_TN_URL, ob.getTextValue());
            ob = tn.getFieldValue("Height");
            assertTrue(ob.isIntegralNumber());
            assertEquals(SAMPLE_SPEC_VALUE_TN_HEIGHT, ob.getIntValue());
            ob = tn.getFieldValue("Width");
            assertTrue(ob.isTextual());
            assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, ob.getTextValue());
            
            ob = imageMap.getFieldValue("IDs");
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
                    assertEquals(values[i], idList.getElementValue(i).getIntValue());
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
        TreeMapper mapper = new TreeMapper();
        JsonNode result = mapper.readTree("true\n");
        assertFalse(result.isNull());
        assertFalse(result.isNumber());
        assertFalse(result.isTextual());
        assertTrue(result.isBoolean());
        assertType(result, BooleanNode.class);
        assertTrue(result.getBooleanValue());
        assertEquals("true", result.getValueAsText());
        assertFalse(result.isMissingNode());

        // also, equality should work ok
        assertEquals(result, BooleanNode.valueOf(true));
        assertEquals(result, BooleanNode.getTrue());
    }

    public void testDouble()
        throws Exception
    {
        TreeMapper mapper = new TreeMapper();
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
        assertEquals(String.valueOf(value), result.getValueAsText());

        // also, equality should work ok
        assertEquals(result, mapper.numberNode(value));
    }

    public void testInt()
        throws Exception
    {
        TreeMapper mapper = new TreeMapper();
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
        assertEquals(String.valueOf(value), result.getValueAsText());
        assertEquals((double) value, result.getDoubleValue());
        assertEquals((long) value, result.getLongValue());

        // also, equality should work ok
        assertEquals(result, mapper.numberNode(value));
    }

    public void testLong()
        throws Exception
    {
        TreeMapper mapper = new TreeMapper();
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
        assertEquals(String.valueOf(value), result.getValueAsText());
        assertEquals((double) value, result.getDoubleValue());

        // also, equality should work ok
        assertEquals(result, mapper.numberNode(value));
    }

    public void testNull()
        throws Exception
    {
        TreeMapper mapper = new TreeMapper();
        JsonNode result = mapper.readTree("   null ");
        assertTrue(result.isNull());
        assertFalse(result.isNumber());
        assertFalse(result.isTextual());
        assertEquals("null", result.getValueAsText());

        // also, equality should work ok
        assertEquals(result, mapper.nullNode());
    }

    public void testDecimalNode()
        throws Exception
    {
        // no "natural" way to get it, must construct
        TreeMapper mapper = new TreeMapper();
        BigDecimal value = new BigDecimal("0.1");
        JsonNode result = mapper.numberNode(value);

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
        assertEquals(value.toString(), result.getValueAsText());

        // also, equality should work ok
        assertEquals(result, mapper.numberNode(value));
    }

    public void testSimpleArray() throws Exception
    {
        TreeMapper mapper = new TreeMapper();
        ArrayNode result = mapper.arrayNode();

        assertTrue(result.isArray());
        assertType(result, ArrayNode.class);

        assertFalse(result.isObject());
        assertFalse(result.isNumber());
        assertFalse(result.isNull());
        assertFalse(result.isTextual());

        // and let's add stuff...
        result.add(mapper.booleanNode(false));
        result.insert(0, mapper.nullNode());

        // should be equal to itself no matter what
        assertEquals(result, result);
        assertFalse(result.equals(null)); // but not to null

        // plus see that we can access stuff
        assertEquals(mapper.nullNode(), result.getPath(0));
        assertEquals(mapper.nullNode(), result.getElementValue(0));
        assertEquals(BooleanNode.getFalse(), result.getPath(1));
        assertEquals(BooleanNode.getFalse(), result.getElementValue(1));
        assertEquals(2, result.size());

        assertNull(result.getElementValue(-1));
        assertNull(result.getElementValue(2));
        JsonNode missing = result.getPath(2);
        assertTrue(missing.isMissingNode());
        assertTrue(result.getPath(-100).isMissingNode());

        // then construct and compare
        ArrayNode array2 = mapper.arrayNode();
        array2.add(mapper.nullNode());
        array2.add(BooleanNode.getFalse());
        assertEquals(result, array2);

        // plus remove entries
        JsonNode rm1 = array2.remove(0);
        assertEquals(mapper.nullNode(), rm1);
        assertEquals(1, array2.size());
        assertEquals(BooleanNode.getFalse(), array2.get(0));
        assertFalse(result.equals(array2));

        JsonNode rm2 = array2.remove(0);
        assertEquals(BooleanNode.getFalse(), rm2);
        assertEquals(0, array2.size());
    }

    /**
     * Type mappers should be able to gracefully deal with end of
     * input.
     */
    public void testEOF()
        throws Exception
    {
        String JSON =
            "{ \"key\": [ { \"a\" : { \"name\": \"foo\",  \"type\": 1\n"
            +"},  \"type\": 3, \"url\": \"http://www.google.com\" } ],\n"
            +"\"name\": \"xyz\", \"type\": 1, \"url\" : null }\n  "
            ;
        JsonFactory jf = new JsonFactory();
        TreeMapper mapper = new TreeMapper();
        JsonParser jp = jf.createJsonParser(new StringReader(JSON));
        JsonNode result = mapper.readTree(jp);

        assertTrue(result.isObject());
        assertEquals(4, result.size());

        assertNull(mapper.readTree(jp));
    }

    public void testMultiple()
        throws Exception
    {
        String JSON = "12  \"string\" [ 1, 2, 3 ]";
        JsonFactory jf = new JsonFactory();
        TreeMapper mapper = new TreeMapper();
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
    public void testMissingNode()
        throws Exception
    {
        String JSON = "[ { }, [ ] ]";
        TreeMapper mapper = new TreeMapper();
        JsonNode result = mapper.readTree(new StringReader(JSON));

        assertTrue(result.isContainerNode());
        assertTrue(result.isArray());
        assertEquals(2, result.size());

        int count = 0;
        for (JsonNode n : result) {
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
        assertNull(onode.getElementValue(0));
        JsonNode dummyNode = onode.getPath(0);
        assertNotNull(dummyNode);
        assertTrue(dummyNode.isMissingNode());
        assertNull(dummyNode.getElementValue(3));
        assertNull(dummyNode.getFieldValue("whatever"));
        JsonNode dummyNode2 = dummyNode.getPath(98);
        assertNotNull(dummyNode2);
        assertTrue(dummyNode2.isMissingNode());
        JsonNode dummyNode3 = dummyNode.getPath("field");
        assertNotNull(dummyNode3);
        assertTrue(dummyNode3.isMissingNode());

        // and same for the array node

        JsonNode anode = it.next();
        assertTrue(anode.isContainerNode());
        assertTrue(anode.isArray());
        assertFalse(anode.isMissingNode()); // real node
        assertEquals(0, anode.size());

        assertNull(anode.getElementValue(0));
        dummyNode = anode.getPath(0);
        assertNotNull(dummyNode);
        assertTrue(dummyNode.isMissingNode());
        assertNull(dummyNode.getElementValue(0));
        assertNull(dummyNode.getFieldValue("myfield"));
        dummyNode2 = dummyNode.getPath(98);
        assertNotNull(dummyNode2);
        assertTrue(dummyNode2.isMissingNode());
        dummyNode3 = dummyNode.getPath("f");
        assertNotNull(dummyNode3);
        assertTrue(dummyNode3.isMissingNode());
    }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
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

