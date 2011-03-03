package org.codehaus.jackson.smile;

import java.io.ByteArrayOutputStream;

public class TestSmileGeneratorSymbols extends SmileTestBase
{
	/**
	 * Simple test to verify that second reference will not output new String, but
	 * rather references one output earlier.
	 */
    public void testSharedNameSimple() throws Exception
    {
        // false, no header (or frame marker)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
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

    // same as above, but with name >= 64 characters
    public void testSharedNameSimpleLong() throws Exception
    {
    	String digits = "01234567899";
    	final String LONG_NAME = "a"+digits+"b"+digits+"c"+digits+"d"+digits+"e"+digits+"f"+digits;
        // false, no header (or frame marker)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmileGenerator gen = smileGenerator(out, false);
        gen.writeStartArray();
        gen.writeStartObject();
        gen.writeNumberField(LONG_NAME, 1);
        gen.writeEndObject();
        gen.writeStartObject();
        gen.writeNumberField(LONG_NAME, 2);
        gen.writeEndObject();
        gen.writeEndArray();
        gen.close();
        byte[] result = out.toByteArray();
        assertEquals(83, result.length);
    }
}
