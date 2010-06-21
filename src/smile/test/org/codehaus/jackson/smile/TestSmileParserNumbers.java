package org.codehaus.jackson.smile;

import java.io.*;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

public class TestSmileParserNumbers
    extends SmileTestBase
{
    public void testIntsMedium() throws IOException
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
    }

    public void testMinMaxInts() throws IOException
    {
    	byte[] data = _smileDoc(String.valueOf(Integer.MAX_VALUE));
    	SmileParser p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    	assertEquals(Integer.MAX_VALUE, p.getIntValue());

    	data = _smileDoc(String.valueOf(Integer.MIN_VALUE));
    	p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.INT, p.getNumberType());
    	assertEquals(Integer.MIN_VALUE, p.getIntValue());
    }

    public void testIntsInObjectSkipping() throws IOException
    {
    	byte[] data = _smileDoc("{\"a\":200,\"b\":200}");
    	SmileParser p = _parser(data);
    	assertToken(JsonToken.START_OBJECT, p.nextToken());
    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("a", p.getCurrentName());
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	// let's NOT access value, forcing skipping
    	assertToken(JsonToken.FIELD_NAME, p.nextToken());
    	assertEquals("b", p.getCurrentName());
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	// let's NOT access value, forcing skipping
    	assertToken(JsonToken.END_OBJECT, p.nextToken());
    }
    
    public void testBorderLongs() throws IOException
    {
    	long l = (long) Integer.MIN_VALUE - 1L;
    	byte[] data = _smileDoc(String.valueOf(l), false);
    	assertEquals(6, data.length);
    	SmileParser p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
    	assertEquals(l, p.getLongValue());
    	
    	l = 1L + (long) Integer.MAX_VALUE;
    	data = _smileDoc(String.valueOf(l), false);
    	assertEquals(6, data.length);
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
    	assertEquals(l, p.getLongValue());
    	assertEquals(String.valueOf(l), p.getText());

    	l = Long.MIN_VALUE;
    	data = _smileDoc(String.valueOf(l));
    	p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
    	assertEquals(l, p.getLongValue());
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

    public void testFloats() throws IOException
    {
    	ByteArrayOutputStream bo = new ByteArrayOutputStream();
    	SmileGenerator g = _generator(bo, false);
    	float value = 0.37f;
    	g.writeNumber(value);
    	g.close();
    	byte[] data = bo.toByteArray();
    	assertEquals(6, data.length);

    	SmileParser p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
    	assertEquals(JsonParser.NumberType.FLOAT, p.getNumberType());
    	assertEquals(value, p.getFloatValue());
    }

    public void testDoubles() throws IOException
    {
    	ByteArrayOutputStream bo = new ByteArrayOutputStream();
    	SmileGenerator g = _generator(bo, false);
    	double value = -12.0986;
    	g.writeNumber(value);
    	g.close();
    	byte[] data = bo.toByteArray();
    	assertEquals(11, data.length);

    	SmileParser p = _parser(data);
    	assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
    	assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());
    	assertEquals(value, p.getDoubleValue());
    }

    public void testArrayWithDoubles() throws IOException
    {
    	ByteArrayOutputStream bo = new ByteArrayOutputStream();
    	SmileGenerator g = _generator(bo, false);
    	g.writeStartArray();
    	g.writeNumber(0.1f);
    	g.writeNumber(0.333);
    	g.writeEndArray();
    	g.close();
    	byte[] data = bo.toByteArray();
    	assertEquals(19, data.length);

    	SmileParser p = _parser(data);
    	assertToken(JsonToken.START_ARRAY, p.nextToken());
    	assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
    	assertEquals(JsonParser.NumberType.FLOAT, p.getNumberType());
    	assertEquals(0.1f, p.getFloatValue());
    	assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
    	assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());
    	assertEquals(0.333, p.getDoubleValue());
    	assertToken(JsonToken.END_ARRAY, p.nextToken());
    }

}
