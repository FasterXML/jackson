package org.codehaus.jackson.smile;

import java.io.ByteArrayOutputStream;

public class TestSmileGeneratorSymbols
	extends SmileTestBase
{
	/**
	 * Simple test to verify that second reference will not output new String, but
	 * rather references one output earlier.
	 */
    public void testSharedNameSimple() throws Exception
    {
        // false, no header (or frame marker)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = _generator(out, false);
        gen.writeStartArray();
        gen.writeStartObject();
        gen.writeNumberField("abc", 1);
        gen.writeEndObject();
        gen.writeStartObject();
        gen.writeNumberField("abc", 2);
        gen.writeEndObject();
        gen.writeEndArray();
        gen.close();
        byte[] result = out.toByteArray();
        assertEquals(13, result.length);
    }
}
