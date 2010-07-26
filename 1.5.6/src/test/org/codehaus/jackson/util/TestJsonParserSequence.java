package org.codehaus.jackson.util;

import java.io.*;

import org.codehaus.jackson.*;

/**
 * Unit tests to verify that {@link JsonParserSequence} works
 * as expected.
 * 
 * @since 1.5
 */
public class TestJsonParserSequence
    extends main.BaseTest
{
    public void testSimple() throws IOException
    {
        // Let's join a TokenBuffer with JsonParser first
        TokenBuffer buf = new TokenBuffer(null);
        buf.writeStartArray();
        buf.writeString("test");
        JsonParser jp = createParserUsingReader("[ true, null ]");
        
        JsonParserSequence seq = JsonParserSequence.createFlattened(buf.asParser(), jp);
        assertEquals(2, seq.containedParsersCount());

        assertFalse(jp.isClosed());
        
        assertFalse(seq.hasCurrentToken());
        assertNull(seq.getCurrentToken());
        assertNull(seq.getCurrentName());

        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_STRING, seq.nextToken());
        assertEquals("test", seq.getText());
        // end of first parser input, should switch over:
        
        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_TRUE, seq.nextToken());
        assertToken(JsonToken.VALUE_NULL, seq.nextToken());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());

        /* 17-Jan-2009, tatus: At this point, we may or may not get an
         *   exception, depending on how underlying parsers work.
         *   Ideally this should be fixed, probably by asking underlying
         *   parsers to disable checking for balanced start/end markers.
         */

        // for this particular case, we won't get an exception tho...
        assertNull(seq.nextToken());
        // not an error to call again...
        assertNull(seq.nextToken());

        // also: original parsers should be closed
        assertTrue(jp.isClosed());
    }

    /**
     * Test to verify that sequences get flattened, to minimize depth
     * of nesting (to reduce call chaining)
     */
    public void testFlattening() throws IOException
    {
        TokenBuffer buf1 = new TokenBuffer(null);
        buf1.writeStartArray();
        TokenBuffer buf2 = new TokenBuffer(null);
        buf2.writeString("a");
        TokenBuffer buf3 = new TokenBuffer(null);
        buf3.writeNumber(13);
        TokenBuffer buf4 = new TokenBuffer(null);
        buf4.writeEndArray();

        JsonParserSequence seq1 = JsonParserSequence.createFlattened(buf1.asParser(), buf2.asParser());
        assertEquals(2, seq1.containedParsersCount());
        JsonParserSequence seq2 = JsonParserSequence.createFlattened(buf3.asParser(), buf4.asParser());
        assertEquals(2, seq2.containedParsersCount());
        JsonParserSequence combo = JsonParserSequence.createFlattened(seq1, seq2);
        // should flatten it to have 4 underlying parsers
        assertEquals(4, combo.containedParsersCount());

        assertToken(JsonToken.START_ARRAY, combo.nextToken());
        assertToken(JsonToken.VALUE_STRING, combo.nextToken());
        assertEquals("a", combo.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, combo.nextToken());
        assertEquals(13, combo.getIntValue());
        assertToken(JsonToken.END_ARRAY, combo.nextToken());
        assertNull(combo.nextToken());        
    }
}
