package org.codehaus.jackson.map.tree;

import main.BaseTest;
import static org.junit.Assert.*;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;

/**
 * This unit test suite tries to verify that the trees ObjectMapper
 * constructs can be serialized properly.
 */
public class TestTreeMapperSerializer
    extends BaseTest
{
    final static String FIELD1 = "first";
    final static String FIELD2 = "Second?";
    final static String FIELD3 = "foo'n \"bar\"";
    final static String FIELD4 = "4";

    final static String TEXT1 = "Some text & \"stuff\"";
    final static String TEXT2 = "Some more text:\twith\nlinefeeds and all!";

    final static double DOUBLE_VALUE = 9.25;

    public void testFromArray()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode root = mapper.createArrayNode();
        root.add(TEXT1);
        root.add(3);
        ObjectNode obj = root.addObject();
        obj.put(FIELD1, true);
        obj.putArray(FIELD2);
        root.add(false);

        /* Ok, ready... let's serialize using one of two alternate
         * methods: first preferred (using generator)
         * (there are 2 variants here too)
         */
        for (int i = 0; i < 2; ++i) {
            StringWriter sw = new StringWriter();
            if (i == 0) {
                JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
                root.serialize(gen, null);
                gen.close();
            } else {
                mapper.writeValue(sw, root);
            }
            verifyFromArray(sw.toString());
        }
            
        // And then convenient but less efficient alternative:
        verifyFromArray(root.toString());
    }

    public void testFromMap()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put(FIELD4, TEXT2);
        root.put(FIELD3, -1);
        root.putArray(FIELD2);
        root.put(FIELD1, DOUBLE_VALUE);

        /* Let's serialize using one of two alternate methods:
         * first preferred (using generator)
         * (there are 2 variants here too)
         */
        for (int i = 0; i < 2; ++i) {
            StringWriter sw = new StringWriter();
            if (i == 0) {
                JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
                root.serialize(gen, null);
                gen.close();
            } else {
                mapper.writeValue(sw, root);
            }
            verifyFromMap(sw.toString());
        }

        // And then convenient but less efficient alternative:
        verifyFromMap(root.toString());
    }

    /**
     * Unit test to check for regression of [JACKSON-18].
     */
    public void testSmallNumbers()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode root = mapper.createArrayNode();
        for (int i = -20; i <= 20; ++i) {
            JsonNode n = root.numberNode(i);
            root.add(n);
            // Hmmh. Not sure why toString() won't be triggered otherwise...
            assertEquals(String.valueOf(i), n.toString());
        }

        // Loop over 2 different serialization methods
        for (int type = 0; type < 2; ++type) {
            StringWriter sw = new StringWriter();
            if (type == 0) {
                JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
                root.serialize(gen, null);
                gen.close();
            } else {
                mapper.writeValue(sw, root);
            }
            
            String doc = sw.toString();
            JsonParser jp = new JsonFactory().createJsonParser(new StringReader(doc));
            
            assertEquals(JsonToken.START_ARRAY, jp.nextToken());
            for (int i = -20; i <= 20; ++i) {
                assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertEquals(i, jp.getIntValue());
                assertEquals(""+i, jp.getText());
            }
            assertEquals(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }
    }

    public void testNull() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, NullNode.instance);
        assertEquals("null", sw.toString());
    }

    public void testBinary()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final int LENGTH = 13045;
        byte[] data = new byte[LENGTH];
        for (int i = 0; i < LENGTH; ++i) {
            data[i] = (byte) i;
        }
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, BinaryNode.valueOf(data));

        JsonParser jp = new JsonFactory().createJsonParser(sw.toString());
        // note: can't determine it's binary from json alone:
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertArrayEquals(data, jp.getBinaryValue());
        jp.close();
    }

    /*
    ///////////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////////
     */

    private void verifyFromArray(String input)
        throws Exception
    {
        JsonParser jp = new JsonFactory().createJsonParser(new StringReader(input));
        
        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(TEXT1, getAndVerifyText(jp));
        
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(3, jp.getIntValue());
        
        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(FIELD1, getAndVerifyText(jp));
        
        assertEquals(JsonToken.VALUE_TRUE, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(FIELD2, getAndVerifyText(jp));
        
        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());
        assertEquals(JsonToken.END_OBJECT, jp.nextToken());
        
        assertEquals(JsonToken.VALUE_FALSE, jp.nextToken());
        
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
    }

    private void verifyFromMap(String input)
        throws Exception
    {
        JsonParser jp = new JsonFactory().createJsonParser(new StringReader(input));
        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(FIELD4, getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(TEXT2, getAndVerifyText(jp));
        
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(FIELD3, getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(-1, jp.getIntValue());
        
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(FIELD2, getAndVerifyText(jp));
        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());
        
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(FIELD1, getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(DOUBLE_VALUE, jp.getDoubleValue());
        
        assertEquals(JsonToken.END_OBJECT, jp.nextToken());
        
        assertNull(jp.nextToken());
    }
}
