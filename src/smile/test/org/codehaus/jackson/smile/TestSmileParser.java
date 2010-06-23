package org.codehaus.jackson.smile;

import java.io.*;

import org.codehaus.jackson.JsonToken;

public class TestSmileParser
	extends SmileTestBase
{
    public void testSimple() throws IOException
    {
    	byte[] data = _smileDoc("[ true, null, false ]");
    	SmileParser p = _parser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.START_ARRAY, p.nextToken());
    	assertToken(JsonToken.VALUE_TRUE, p.nextToken());
    	assertToken(JsonToken.VALUE_NULL, p.nextToken());
    	assertToken(JsonToken.VALUE_FALSE, p.nextToken());
    	assertToken(JsonToken.END_ARRAY, p.nextToken());
    	assertNull(p.nextToken());
    	p.close();
    }

    public void testArrayWithString() throws IOException
    {
    	byte[] data = _smileDoc("[ \"abc\" ]");
    	SmileParser p = _parser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.START_ARRAY, p.nextToken());
    	assertToken(JsonToken.VALUE_STRING, p.nextToken());
    	assertEquals("abc", p.getText());
    	assertEquals(0, p.getTextOffset());
    	assertEquals(3, p.getTextLength());
    	assertToken(JsonToken.END_ARRAY, p.nextToken());
    	p.close();
    }

    public void testEmptyStrings() throws IOException
    {
    	// first, empty key
    	byte[] data = _smileDoc("{ \"\":true }");
    	SmileParser p = _parser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.START_OBJECT, p.nextToken());
    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("", p.getCurrentName());
    	assertToken(JsonToken.VALUE_TRUE, p.nextToken());
    	assertToken(JsonToken.END_OBJECT, p.nextToken());
    	assertNull(p.nextToken());
    	p.close();

    	// then empty value
    	data = _smileDoc("{ \"abc\":\"\" }");
    	p = _parser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.START_OBJECT, p.nextToken());
    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("abc", p.getCurrentName());
    	assertToken(JsonToken.VALUE_STRING, p.nextToken());
    	assertEquals("", p.getText());
    	assertToken(JsonToken.END_OBJECT, p.nextToken());
    	assertNull(p.nextToken());
    	p.close();
    	
    	// and combinations
    	data = _smileDoc("{ \"\":\"\", \"\":\"\" }");
    	p = _parser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.START_OBJECT, p.nextToken());
    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("", p.getCurrentName());
    	assertToken(JsonToken.VALUE_STRING, p.nextToken());
    	assertEquals("", p.getText());
    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("", p.getCurrentName());
    	assertToken(JsonToken.VALUE_STRING, p.nextToken());
    	assertEquals("", p.getText());
    	assertToken(JsonToken.END_OBJECT, p.nextToken());
    	assertNull(p.nextToken());
    	p.close();
    }
    
    /**
     * Test for ascii String values longer than 64 bytes; separate
     * since handling differs
     */
    public void testLongAsciiString() throws IOException
    {
    	final String DIGITS = "1234567890";
    	String LONG = DIGITS + DIGITS + DIGITS + DIGITS;
    	LONG = LONG + LONG + LONG + LONG;
    	byte[] data = _smileDoc(quote(LONG));

    	SmileParser p = _parser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.VALUE_STRING, p.nextToken());
    	assertEquals(LONG, p.getText());
    	assertNull(p.nextToken());
    }

    /**
     * Test for non-ASCII String values longer than 64 bytes; separate
     * since handling differs
     */
    public void testLongUnicodeString() throws IOException
    {
    	final String DIGITS = "1234567890";
    	final String UNIC = "\u00F06"; // o with umlauts
    	String LONG = DIGITS + UNIC + DIGITS + UNIC + UNIC + DIGITS + DIGITS;
    	LONG = LONG + LONG + LONG;
    	byte[] data = _smileDoc(quote(LONG));

    	SmileParser p = _parser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.VALUE_STRING, p.nextToken());
    	assertEquals(LONG, p.getText());
    	assertNull(p.nextToken());
    }
    
    public void testTrivialObject() throws IOException
    {
    	byte[] data = _smileDoc("{\"abc\":13}");
    	SmileParser p = _parser(data);
    	assertNull(p.getCurrentToken());

    	assertToken(JsonToken.START_OBJECT, p.nextToken());
    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("abc", p.getCurrentName());
    	assertEquals("abc", p.getText());
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(13, p.getIntValue());    	
    	assertToken(JsonToken.END_OBJECT, p.nextToken());
    }
    
    public void testSimpleObject() throws IOException
    {
    	byte[] data = _smileDoc("{\"a\":8, \"b\" : [ true ], \"c\" : { }, \"d\":{\"e\":null}}");
    	SmileParser p = _parser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.START_OBJECT, p.nextToken());

    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("a", p.getCurrentName());
    	assertEquals("a", p.getText());
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(8, p.getIntValue());

    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("b", p.getCurrentName());
    	assertToken(JsonToken.START_ARRAY, p.nextToken());
    	assertToken(JsonToken.VALUE_TRUE, p.nextToken());
    	assertToken(JsonToken.END_ARRAY, p.nextToken());

    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("c", p.getCurrentName());
    	assertToken(JsonToken.START_OBJECT, p.nextToken());
    	assertToken(JsonToken.END_OBJECT, p.nextToken());

    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("d", p.getCurrentName());

    	assertToken(JsonToken.START_OBJECT, p.nextToken());
    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("e", p.getCurrentName());
    	assertToken(JsonToken.VALUE_NULL, p.nextToken());
    	assertToken(JsonToken.END_OBJECT, p.nextToken());

    	assertToken(JsonToken.END_OBJECT, p.nextToken());
    	p.close();
    }

    public void testJsonSampleDoc() throws IOException
    {
    	byte[] data = _smileDoc(SAMPLE_DOC_JSON_SPEC);
    	verifyJsonSpecSampleDoc(_parser(data), true);
    }
}
