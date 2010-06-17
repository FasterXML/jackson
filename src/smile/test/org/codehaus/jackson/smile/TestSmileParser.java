package org.codehaus.jackson.smile;

import java.io.*;

import org.codehaus.jackson.JsonParser;
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

    public void testInts() throws IOException
    {
    	byte[] data = _smileDoc("255");
    	SmileParser p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(255, p.getIntValue());
    	assertEquals("255", p.getText());

    	data = _smileDoc("-999");
    	p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    	assertEquals(-999, p.getIntValue());
    	assertEquals("-999", p.getText());

    	data = _smileDoc("123456789");
    	p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    	assertEquals(123456789, p.getIntValue());
    	
    	data = _smileDoc(String.valueOf(Integer.MAX_VALUE));
    	p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    	assertEquals(Integer.MAX_VALUE, p.getIntValue());

    	long l = 1L + Integer.MAX_VALUE;
    	data = _smileDoc(String.valueOf(l));
    	p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
    	assertEquals(l, p.getLongValue());
    }

    public void testLongs() throws IOException
    {
    	long l = Long.MAX_VALUE;
    	byte[] data = _smileDoc(String.valueOf(l));
    	SmileParser p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
    	assertEquals(l, p.getIntValue());
    	assertEquals(String.valueOf(l), p.getText());

    	l = Long.MIN_VALUE;
    	data = _smileDoc(String.valueOf(l));
    	p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
    	assertEquals(l, p.getIntValue());
    	assertEquals(String.valueOf(l), p.getText());
    }
    
    public void testArrayWithInts() throws IOException
    {
    	byte[] data = _smileDoc("[ 1, 0, -1, 255, -999, "
    			+Integer.MIN_VALUE+","+Integer.MAX_VALUE+","
    			+Long.MIN_VALUE+", "+Long.MAX_VALUE+" ]");
    	SmileParser p = _parser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.START_ARRAY, p.nextToken());

    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(1, p.getIntValue());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(0, p.getIntValue());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(-1, p.getIntValue());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());

    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(255, p.getIntValue());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(-999, p.getIntValue());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());

    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    	assertEquals(Integer.MIN_VALUE, p.getIntValue());
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(Integer.MAX_VALUE, p.getIntValue());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());

    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
    	assertEquals(Long.MIN_VALUE, p.getLongValue());
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(Long.MAX_VALUE, p.getLongValue());
    	assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
    	
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
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
}
