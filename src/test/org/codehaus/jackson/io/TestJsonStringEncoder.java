package org.codehaus.jackson.io;

import static org.junit.Assert.*;

public class TestJsonStringEncoder
    extends main.BaseTest
{
    public void testQuoteAsString() throws Exception
    {
        JsonStringEncoder encoder = new JsonStringEncoder();
        char[] result = encoder.quoteAsString("foobar");
        assertArrayEquals("foobar".toCharArray(), result);
        result = encoder.quoteAsString("\"x\"");
        assertArrayEquals("\\\"x\\\"".toCharArray(), result);
    }

    public void testQuoteAsUTF8() throws Exception
    {
        
    }

    public void encodeAsUTF8() throws Exception
    {
        
    }
    
}

