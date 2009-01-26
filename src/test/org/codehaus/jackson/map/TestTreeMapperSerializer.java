package org.codehaus.jackson.map;

import main.BaseTest;
import static org.junit.Assert.*;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tries to verify that the "JSON type"
 * mapper constructed JsonNodes can be serialized properly.
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
        TreeMapper mapper = new TreeMapper();

        JsonNode root = mapper.arrayNode();
        root.appendElement(mapper.textNode(TEXT1));
        root.appendElement(mapper.numberNode(3));
        JsonNode obj = mapper.objectNode();
        root.appendElement(obj);
        obj.setElement(FIELD1, mapper.booleanNode(true));
        obj.setElement(FIELD2, mapper.arrayNode());
        root.appendElement(mapper.booleanNode(false));

        /* Ok, ready... let's serialize using one of two alternate
         * methods: first preferred (using generator)
         * (there are 2 variants here too)
         */
        for (int i = 0; i < 2; ++i) {
            StringWriter sw = new StringWriter();
            if (i == 0) {
                JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
                root.writeTo(gen);
                gen.close();
            } else {
                mapper.writeTree(root, sw);
            }
            verifyFromArray(sw.toString());
        }
            
        // And then convenient but less efficient alternative:
        verifyFromArray(root.toString());
    }

    public void testFromMap()
        throws Exception
    {
        TreeMapper mapper = new TreeMapper();

        JsonNode root = mapper.objectNode();
        root.setElement(FIELD4, mapper.textNode(TEXT2));
        root.setElement(FIELD3, mapper.numberNode(-1));
        root.setElement(FIELD2, mapper.arrayNode());
        root.setElement(FIELD1, mapper.numberNode(DOUBLE_VALUE));

        /* Let's serialize using one of two alternate methods:
         * first preferred (using generator)
         * (there are 2 variants here too)
         */
        for (int i = 0; i < 2; ++i) {
            StringWriter sw = new StringWriter();
            if (i == 0) {
                JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
                root.writeTo(gen);
                gen.close();
            } else {
                mapper.writeTree(root, sw);
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
        TreeMapper mapper = new TreeMapper();
        JsonNode root = mapper.arrayNode();
        for (int i = -20; i <= 20; ++i) {
            JsonNode n = mapper.numberNode(i);
            root.appendElement(n);
            // Hmmh. Not sure why toString() won't be triggered otherwise...
            assertEquals(String.valueOf(i), n.toString());
        }

        // Loop over 2 different serialization methods
        for (int type = 0; type < 2; ++type) {
            StringWriter sw = new StringWriter();
            if (type == 0) {
                JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
                root.writeTo(gen);
                gen.close();
            } else {
                mapper.writeTree(root, sw);
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
        TreeMapper mapper = new TreeMapper();
        StringWriter sw = new StringWriter();
        mapper.writeTree(mapper.nullNode(), sw);
        assertEquals("null", sw.toString());
    }

    public void testBinary()
        throws Exception
    {
        TreeMapper mapper = new TreeMapper();
        final int LENGTH = 13045;
        byte[] data = new byte[LENGTH];
        for (int i = 0; i < LENGTH; ++i) {
            data[i] = (byte) i;
        }
        StringWriter sw = new StringWriter();
        mapper.writeTree(mapper.binaryNode(data), sw);

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
