package org.codehaus.jackson.node;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestTreeTraversingParser
    extends BaseMapTest
{
    public void testSimple() throws Exception
    {
        // For convenience, parse tree from JSON first
        final String JSON =
            "{ \"a\" : 123, \"list\" : [ 12.25, null, true, { }, [ ] ] }";
        ObjectMapper m = new ObjectMapper();
        JsonNode tree = m.readTree(JSON);
        JsonParser jp = tree.traverse();

        assertNull(jp.getCurrentToken());
        assertNull(jp.getCurrentName());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertNull(jp.getCurrentName());
        assertEquals(JsonToken.START_OBJECT.asString(), jp.getText());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("a", jp.getCurrentName());
        assertEquals("a", jp.getText());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals("a", jp.getCurrentName());
        assertEquals(123, jp.getIntValue());
        assertEquals("123", jp.getText());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("list", jp.getCurrentName());
        assertEquals("list", jp.getText());

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals("list", jp.getCurrentName());
        assertEquals(JsonToken.START_ARRAY.asString(), jp.getText());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertNull(jp.getCurrentName());
        assertEquals(12.25, jp.getDoubleValue());
        assertEquals("12.25", jp.getText());

        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertNull(jp.getCurrentName());
        assertEquals(JsonToken.VALUE_NULL.asString(), jp.getText());

        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertNull(jp.getCurrentName());
        assertTrue(jp.getBooleanValue());
        assertEquals(JsonToken.VALUE_TRUE.asString(), jp.getText());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.getCurrentName());

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.getCurrentName());

        assertToken(JsonToken.END_ARRAY, jp.nextToken());

        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.getCurrentName());

        assertNull(jp.nextToken());

        jp.close();
        assertTrue(jp.isClosed());
    }
}

