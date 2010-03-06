package org.codehaus.jackson.main;

import java.io.*;

import org.codehaus.jackson.*;

public class TestJsonFactory
    extends main.BaseTest
{
    public void testGeneratorFeatures() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertNull(f.getCodec());

        f.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        assertTrue(f.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        f.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
        assertFalse(f.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
    }

    public void testParserFeatures() throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertNull(f.getCodec());

        f.configure(JsonParser.Feature.INTERN_FIELD_NAMES, true);
        assertTrue(f.isEnabled(JsonParser.Feature.INTERN_FIELD_NAMES));
        f.configure(JsonParser.Feature.INTERN_FIELD_NAMES, false);
        assertFalse(f.isEnabled(JsonParser.Feature.INTERN_FIELD_NAMES));
    }
    
    @SuppressWarnings("deprecation")
    public void testDeprecatedFeatures() throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.enableParserFeature(JsonParser.Feature.ALLOW_COMMENTS);
        assertTrue(f.isParserFeatureEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        f.disableParserFeature(JsonParser.Feature.ALLOW_COMMENTS);
        assertFalse(f.isParserFeatureEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        f.setParserFeature(JsonParser.Feature.ALLOW_COMMENTS, true);
        assertTrue(f.isParserFeatureEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        
        f.enableGeneratorFeature(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        assertTrue(f.isGeneratorFeatureEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        f.disableGeneratorFeature(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        assertFalse(f.isGeneratorFeatureEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        f.setGeneratorFeature(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        assertTrue(f.isGeneratorFeatureEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        
    }
    
    public void testJsonWithFiles() throws Exception
    {
        File file = File.createTempFile("jackson-test", null);
        file.deleteOnExit();
        
        JsonFactory f = new JsonFactory();

        // First: create file via generator.. and use an odd encoding
        JsonGenerator jg = f.createJsonGenerator(file, JsonEncoding.UTF16_LE);
        jg.writeStartObject();
        jg.writeRaw("   ");
        jg.writeEndObject();
        jg.close();

        // Ok: first read file directly
        JsonParser jp = f.createJsonParser(file);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();

        // Then via URL:
        jp = f.createJsonParser(file.toURI().toURL());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();

        // ok, delete once we are done
        file.delete();
    }
}
