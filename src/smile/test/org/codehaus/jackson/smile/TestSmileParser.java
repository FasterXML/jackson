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
