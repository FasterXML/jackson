package org.codehaus.jackson.smile;

import java.io.IOException;

import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

public class TestSmileParserLocation
    extends SmileTestBase
{
    /**
     * Basic unit test to verify that [JACKSON-] has been resolved.
     */
    public void testSimpleOffsets() throws IOException
    {
        byte[] data = _smileDoc("[ true, null, false, 511 ]", true); // true -> write header
        
        JsonParser p = _smileParser(data);
        assertNull(p.getCurrentToken());
        JsonLocation loc = p.getCurrentLocation();
        assertNotNull(loc);
        // first: -1 for "not known", for character-based stuff
        assertEquals(-1, loc.getCharOffset());
        assertEquals(-1, loc.getColumnNr());
        assertEquals(-1, loc.getLineNr());
        // but first 4 bytes are for header
        assertEquals(4, loc.getByteOffset());

        // array marker is a single byte, so:
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals(5, p.getCurrentLocation().getByteOffset());
        assertEquals(4, p.getTokenLocation().getByteOffset());

        // same for true and others except for last int
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals(6, p.getCurrentLocation().getByteOffset());
        assertEquals(5, p.getTokenLocation().getByteOffset());

        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertEquals(7, p.getCurrentLocation().getByteOffset());
        assertEquals(6, p.getTokenLocation().getByteOffset());

        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals(8, p.getCurrentLocation().getByteOffset());
        assertEquals(7, p.getTokenLocation().getByteOffset());

        // 0x1FF takes 3 bytes (type byte, 7/6 bit segments)
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(511, p.getIntValue());
        assertEquals(11, p.getCurrentLocation().getByteOffset());
        assertEquals(8, p.getTokenLocation().getByteOffset());
        
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals(12, p.getCurrentLocation().getByteOffset());
        assertEquals(11, p.getTokenLocation().getByteOffset());

        assertNull(p.nextToken());
        p.close();
    }
}
