package org.codehaus.jackson.smile;

import java.io.ByteArrayOutputStream;

import org.codehaus.jackson.*;

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

    	// Base is 76 chars; loop over couple of shorter ones too
    	
    	final String LONG_NAME = "a"+digits+"b"+digits+"c"+digits+"d"+digits+"e"+digits+"f"+digits+"ABCD";
    	
    	for (int i = 0; i < 4; ++i) {
    	    int strLen = LONG_NAME.length() - i;
    	    String field = LONG_NAME.substring(0, strLen);
            // false, no header (or frame marker)
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            SmileGenerator gen = smileGenerator(out, false);
            gen.writeStartArray();
            gen.writeStartObject();
            gen.writeNumberField(field, 1);
            gen.writeEndObject();
            gen.writeStartObject();
            gen.writeNumberField(field, 2);
            gen.writeEndObject();
            gen.writeEndArray();
            gen.close();
            byte[] result = out.toByteArray();
            assertEquals(11 + field.length(), result.length);
    
            // better also parse it back...
            JsonParser parser = _smileParser(result);
            assertToken(JsonToken.START_ARRAY, parser.nextToken());
    
            assertToken(JsonToken.START_OBJECT, parser.nextToken());
            assertToken(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals(field, parser.getCurrentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(1, parser.getIntValue());
            assertToken(JsonToken.END_OBJECT, parser.nextToken());
    
            assertToken(JsonToken.START_OBJECT, parser.nextToken());
            assertToken(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals(field, parser.getCurrentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(2, parser.getIntValue());
            assertToken(JsonToken.END_OBJECT, parser.nextToken());
    
            assertToken(JsonToken.END_ARRAY, parser.nextToken());
    	}
    }

    public void testLongNamesNonShared() throws Exception
    {
        _testLongNames(false);
    }
    
    public void testLongNamesShared() throws Exception
    {
        _testLongNames(true);
    }

    /*
    /**********************************************************
    /* Secondary methods
    /**********************************************************
     */
    
    // For issue [JACKSON-552]
    public void _testLongNames(boolean shareNames) throws Exception
    {
        // 68 bytes long (on boundary)
        final String FIELD_NAME = "dossier.domaine.supportsDeclaratifsForES.SupportDeclaratif.reference";
        final String VALUE = "11111";
        
        SmileFactory factory = new SmileFactory();
        factory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, shareNames);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonGenerator gen = factory.createJsonGenerator(os);
        gen.writeStartObject();
        gen.writeObjectFieldStart("query");
        gen.writeStringField(FIELD_NAME, VALUE);
        gen.writeEndObject();
        gen.writeEndObject();
        gen.close();
        
        JsonParser parser = factory.createJsonParser(os.toByteArray());
        assertNull(parser.getCurrentToken());
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("query", parser.getCurrentName());
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(FIELD_NAME, parser.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals(VALUE, parser.getText());
        assertToken(JsonToken.END_OBJECT, parser.nextToken());
        assertToken(JsonToken.END_OBJECT, parser.nextToken());
    }

}
