package org.codehaus.jackson.util;

import java.io.*;

import org.codehaus.jackson.*;

public class TestTokenBuffer extends main.BaseTest
{
    /**
     * Test writing of individual simple values
     */
    public void testSimpleWrites() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null); // no ObjectCodec

        // First, with empty buffer
        JsonParser jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertNull(jp.nextToken());
        jp.close();

        // Then with simple text
        buf.writeString("abc");

        // First, simple text
        jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("abc", jp.getText());
        assertNull(jp.nextToken());
        jp.close();

        // Then, let's append at root level
        buf.writeNumber(13);
        jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(13, jp.getIntValue());
        assertNull(jp.nextToken());
        jp.close();
    }

    public void testSimpleArray() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null); // no ObjectCodec

        // First, empty array
        assertTrue(buf.getOutputContext().inRoot());
        buf.writeStartArray();
        assertTrue(buf.getOutputContext().inArray());
        buf.writeEndArray();
        assertTrue(buf.getOutputContext().inRoot());

        JsonParser jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertTrue(jp.getParsingContext().inRoot());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertTrue(jp.getParsingContext().inArray());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertTrue(jp.getParsingContext().inRoot());
        assertNull(jp.nextToken());
        jp.close();

        // Then one with simple contents
        buf = new TokenBuffer(null);
        buf.writeStartArray();
        buf.writeBoolean(true);
        buf.writeNull();
        buf.writeEndArray();
        jp = buf.asParser();
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertTrue(jp.getBooleanValue());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();

        // And finally, with array-in-array
        buf = new TokenBuffer(null);
        buf.writeStartArray();
        buf.writeStartArray();
        buf.writeBinary(new byte[3]);
        buf.writeEndArray();
        buf.writeEndArray();
        jp = buf.asParser();
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        // TokenBuffer exposes it as embedded object...
        assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, jp.nextToken());
        Object ob = jp.getEmbeddedObject();
        assertNotNull(ob);
        assertTrue(ob instanceof byte[]);
        assertEquals(3, ((byte[]) ob).length);
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
    }

    public void testSimpleObject() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null);

        // First, empty JSON Object
        assertTrue(buf.getOutputContext().inRoot());
        buf.writeStartObject();
        assertTrue(buf.getOutputContext().inObject());
        buf.writeEndObject();
        assertTrue(buf.getOutputContext().inRoot());

        JsonParser jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertTrue(jp.getParsingContext().inRoot());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertTrue(jp.getParsingContext().inObject());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertTrue(jp.getParsingContext().inRoot());
        assertNull(jp.nextToken());
        jp.close();

        // Then one with simple contents
        buf = new TokenBuffer(null);
        buf.writeStartObject();
        buf.writeNumberField("num", 1.25);
        buf.writeEndObject();

        jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(1.25, jp.getDoubleValue());
        // should still have access to name
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        // but not any more
        assertNull(jp.getCurrentName());
        assertNull(jp.nextToken());
        jp.close();
    }

    /**
     * Verify handling of that "standard" test document (from JSON
     * specification)
     */
    public void testWithJSONSampleDoc() throws Exception
    {
        // First, copy events from known good source (StringReader)
        JsonParser jp = createParserUsingReader(SAMPLE_DOC_JSON_SPEC);
        TokenBuffer tb = new TokenBuffer(null);
        while (jp.nextToken() != null) {
            tb.copyCurrentEvent(jp);
        }

        // And then request verification; first structure only:
        verifyJsonSpecSampleDoc(tb.asParser(), false);

        // then content check too:
        verifyJsonSpecSampleDoc(tb.asParser(), true);
    }
}
