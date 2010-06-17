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
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
}
