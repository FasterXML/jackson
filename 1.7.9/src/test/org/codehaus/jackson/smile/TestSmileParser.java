package org.codehaus.jackson.smile;

import java.io.*;

import org.codehaus.jackson.JsonToken;

public class TestSmileParser
    extends SmileTestBase
{
    /**
     * Unit tests for verifying that if header/signature is required,
     * lacking it is fatal
     */
    public void testMandatoryHeader() throws IOException
    {
        // first test failing case
        byte[] data = _smileDoc("[ null ]", false);
        try {
            _smileParser(data, true);
            fail("Should have gotten exception for missing header");
        } catch (Exception e) {
            verifyException(e, "does not start with Smile format header");
        }

        // and then test passing one
        SmileParser p = _smileParser(data, false);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
    }

    public void testSimple() throws IOException
    {
    	byte[] data = _smileDoc("[ true, null, false ]");
    	SmileParser p = _smileParser(data);
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
    	SmileParser p = _smileParser(data);
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
    	SmileParser p = _smileParser(data);
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
    	p = _smileParser(data);
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
    	p = _smileParser(data);
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

    	SmileParser p = _smileParser(data);
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

    	SmileParser p = _smileParser(data);
    	assertNull(p.getCurrentToken());
    	assertToken(JsonToken.VALUE_STRING, p.nextToken());
    	assertEquals(LONG, p.getText());
    	assertNull(p.nextToken());
    }
    
    public void testTrivialObject() throws IOException
    {
    	byte[] data = _smileDoc("{\"abc\":13}");
    	SmileParser p = _smileParser(data);
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
    	SmileParser p = _smileParser(data);
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

    public void testNestedObject() throws IOException
    {
        byte[] data = _smileDoc("[{\"a\":{\"b\":[1]}}]");
        SmileParser p = _smileParser(data);
        assertNull(p.getCurrentToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // a
        assertEquals("a", p.getCurrentName());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // b
        assertEquals("b", p.getCurrentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
    }
    
    public void testJsonSampleDoc() throws IOException
    {
    	byte[] data = _smileDoc(SAMPLE_DOC_JSON_SPEC);
    	verifyJsonSpecSampleDoc(_smileParser(data), true);
    }

    public void testUnicodeStringValues() throws IOException
    {
        String uc = "\u00f6stl. v. Greenwich \u3333?";
        byte[] data = _smileDoc("[" +quote(uc)+"]");

        // First, just skipping
        SmileParser p = _smileParser(data);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();

        // Then accessing data
        p = _smileParser(data);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(uc, p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();

        // and then let's create longer text segment as well
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 200) {
            sb.append(uc);
        }
        final String longer = sb.toString();
        data = _smileDoc("["+quote(longer)+"]");

        // Ok once again, first skipping, then accessing
        p = _smileParser(data);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();

        p = _smileParser(data);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(longer, p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    public void testUnicodePropertyNames() throws IOException
    {
        String uc = "\u00f6stl. v. Greenwich \u3333";
        byte[] data = _smileDoc("{" +quote(uc)+":true}");

        // First, just skipping
        SmileParser p = _smileParser(data);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();

        // Then accessing data
        p = _smileParser(data);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(uc, p.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    /**
     * Simple test to verify that byte 0 is not used (an implementation
     * might mistakenly consider it a string value reference)
     * 
     * @throws IOException
     */
    public void testInvalidByte() throws IOException
    {
        byte[] data = new byte[] { SmileConstants.TOKEN_LITERAL_START_ARRAY,
                (byte) SmileConstants.TOKEN_PREFIX_SHARED_STRING_SHORT,
                (byte) SmileConstants.TOKEN_LITERAL_END_ARRAY
        };
        SmileParser p = _smileParser(data);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        // And now should get an error
        try {
            JsonToken t = p.nextToken();
            fail("Expected parse error, got: "+t);
        } catch (IOException e) {
            verifyException(e, "Invalid token byte 0x00");
        }
    }
}
