package map;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.node.*;

/**
 * This unit test suite tries to verify that the "JSON" type
 * mapper can properly parse JSON and bind contents into appropriate
 * JsonNode instances.
 */
public class TestToJsonType
    extends BaseTest
{
    public void testSimple()
        throws Exception
    {
        final String JSON = SAMPLE_DOC_JSON_SPEC;

        JsonFactory jf = new JsonFactory();
        JsonNode result = new JsonTypeMapper().read(jf.createJsonParser(new StringReader(JSON)));
        assertType(result, ObjectNode.class);
        assertEquals(1, result.size());
        assertTrue(result.isObject());

        ObjectNode main = (ObjectNode) result;
        assertEquals("Image", main.getFieldNames().next());
        JsonNode ob = main.getFieldValues().next();
        assertType(ob, ObjectNode.class);
        ObjectNode imageMap = (ObjectNode) ob;

        assertEquals(5, imageMap.size());
        ob = imageMap.getFieldValue("Width");
        assertTrue(ob.isIntegralNumber());
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

    /**
     * Type mappers should be able to gracefully deal with end of
     * input.
     */
    public void testEOF()
        throws Exception
    {
        JsonFactory jf = new JsonFactory();
        String JSON =
            "{ \"key\": [ { \"a\" : { \"name\": \"foo\",  \"type\": 1\n"
            +"},  \"type\": 3, \"url\": \"http://www.google.com\" } ],\n"
            +"\"name\": \"xyz\", \"type\": 1, \"url\" : null }\n  "
            ;
        JsonParser jp = jf.createJsonParser(new StringReader(JSON));
        JsonTypeMapper mapper = new JsonTypeMapper();
        JsonNode result = mapper.read(jp);
        assertTrue(result.isObject());
        assertEquals(4, result.size());

        assertNull(mapper.read(jp));
    }

    public void testMultipl()
        throws Exception
    {
        JsonFactory jf = new JsonFactory();
        String JSON = "12  \"string\" [ 1, 2, 3 ]";
        JsonParser jp = jf.createJsonParser(new StringReader(JSON));
        JsonTypeMapper mapper = new JsonTypeMapper();

        JsonNode result = mapper.read(jp);
        assertTrue(result.isIntegralNumber());
        assertEquals(12, result.getIntValue());

        result = mapper.read(jp);
        assertTrue(result.isTextual());
        assertEquals("string", result.getTextValue());

        result = mapper.read(jp);
        assertTrue(result.isArray());
        assertEquals(3, result.size());

        assertNull(mapper.read(jp));
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

