package org.codehaus.jackson.smile;

import java.io.*;
import java.util.Random;

import org.codehaus.jackson.*;
import org.codehaus.jackson.sym.BytesToNameCanonicalizer;

/**
 * Unit tests for verifying that symbol handling works as planned, including
 * efficient reuse of names encountered during parsing.
 */
public class TestSmileParserSymbolHandling
	extends SmileTestBase
{
    public void testSimple() throws IOException
    {
        final String STR1 = "a";
		
    	byte[] data = _smileDoc("{ "+quote(STR1)+":1, \"foobar\":2, \"longername\":3 }");
    	SmileFactory f = new SmileFactory();
    	SmileParser p = _smileParser(f, data);
    	final BytesToNameCanonicalizer symbols1 = p._symbols;
    	assertEquals(0, symbols1.size());
    	
    	assertEquals(JsonToken.START_OBJECT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	// field names are interned:
    	assertSame(STR1, p.getCurrentName());
    	assertEquals(1, symbols1.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	assertSame("foobar", p.getCurrentName());
    	assertEquals(2, symbols1.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	assertSame("longername", p.getCurrentName());
    	assertEquals(3, symbols1.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.END_OBJECT, p.nextToken());
    	assertNull(p.nextToken());
    	assertEquals(3, symbols1.size());
        p.close();

        // but let's verify that symbol table gets reused properly
    	p = _smileParser(f, data);
    	BytesToNameCanonicalizer symbols2 = p._symbols;
    	// symbol tables are not reused, but contents are:
    	assertNotSame(symbols1, symbols2);
    	assertEquals(3, symbols2.size());

    	assertEquals(JsonToken.START_OBJECT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	// field names are interned:
    	assertSame(STR1, p.getCurrentName());
    	assertEquals(3, symbols2.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	assertSame("foobar", p.getCurrentName());
    	assertEquals(3, symbols2.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	assertSame("longername", p.getCurrentName());
    	assertEquals(3, symbols2.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.END_OBJECT, p.nextToken());
    	assertNull(p.nextToken());
    	assertEquals(3, symbols2.size());
        p.close();

        assertEquals(3, symbols2.size());
    	p.close();
    }

    public void testSharedNames() throws IOException
    {
        final int COUNT = 19000;
        
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.WRITE_HEADER, false);
        f.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream(4000);
        JsonGenerator gen = f.createJsonGenerator(out);
        gen.writeStartArray();
        Random rnd = new Random(COUNT);
        for (int i = 0; i < COUNT; ++i) {
            gen.writeStartObject();
            int nr = rnd.nextInt() % 1200;
            gen.writeNumberField("f"+nr, nr);
            gen.writeEndObject();
        }
        gen.writeEndArray();
        gen.close();
        byte[] json = out.toByteArray();

        // And verify 
        f.configure(SmileParser.Feature.REQUIRE_HEADER, false);
        JsonParser jp = f.createJsonParser(json);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        rnd = new Random(COUNT);
        for (int i = 0; i < COUNT; ++i) {
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            int nr = rnd.nextInt() % 1200;
            String name = "f"+nr;
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            assertEquals(name, jp.getCurrentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(nr, jp.getIntValue());
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
            
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
    }

    public void testSharedStrings() throws IOException
    {
        final int count = 19000;
        byte[] baseline = writeStringValues(false, count);
        assertEquals(396119, baseline.length);
        verifyStringValues(baseline, count);
        
        // and then shared; should be much smaller
        byte[] shared = writeStringValues(true, count);
        if (shared.length >= baseline.length) {
            fail("Expected shared String length < "+baseline.length+", was "+shared.length);
        }
        verifyStringValues(baseline, count);
    }

    private byte[] writeStringValues(boolean enableSharing, int COUNT) throws IOException
    {
        String MORE_CHARS = "01234567890";
        MORE_CHARS += MORE_CHARS;
        MORE_CHARS += MORE_CHARS;
        MORE_CHARS += MORE_CHARS; // -> 80 characters

        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.WRITE_HEADER, true);
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, enableSharing);
        ByteArrayOutputStream out = new ByteArrayOutputStream(4000);
        JsonGenerator gen = f.createJsonGenerator(out);
        gen.writeStartArray();
        Random rnd = new Random(COUNT);
        for (int i = 0; i < COUNT; ++i) {
            int nr = rnd.nextInt() % 1200;
            // Actually, let's try longer ones too
            String str = "value"+nr;
            if (nr > 900) {
                str += MORE_CHARS;
            }
            gen.writeString(str);
        }
        gen.writeEndArray();
        gen.close();
        return out.toByteArray();
    }

    private void verifyStringValues(byte[] json, int COUNT) throws IOException
    {
        String MORE_CHARS = "01234567890";
        MORE_CHARS += MORE_CHARS;
        MORE_CHARS += MORE_CHARS;
        MORE_CHARS += MORE_CHARS; // -> 80 characters
        
        SmileFactory f = new SmileFactory();
        JsonParser jp = f.createJsonParser(json);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        Random rnd = new Random(COUNT);
        for (int i = 0; i < COUNT; ++i) {
            int nr = rnd.nextInt() % 1200;
            // Actually, let's try longer ones too
            String str = "value"+nr;
            if (nr > 900) {
                str += MORE_CHARS;
            }
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(str, jp.getText());
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
    }
}
