package org.codehaus.jackson.impl;

import static org.junit.Assert.assertArrayEquals;

import java.io.*;

import org.codehaus.jackson.*;

public class TestBase64Parsing
    extends main.BaseTest
{
    public void testBase64UsingInputStream() throws Exception
    {
        _testBase64Text(true);
    }

    public void testBase64UsingReader() throws Exception
    {
        _testBase64Text(false);
    }

    /*
    /**********************************************************
    /* Test helper methods
    /**********************************************************
     */
    
    // Test for [JACKSON-631]
    public void _testBase64Text(boolean useBytes) throws Exception
    {
        // let's actually iterate over sets of encoding modes, lengths
        
        final int[] LENS = { 1, 2, 3, 4, 7, 9, 32, 33, 34, 35 };
        final Base64Variant[] VARIANTS = {
                Base64Variants.MIME,
                Base64Variants.MIME_NO_LINEFEEDS,
                Base64Variants.MODIFIED_FOR_URL,
                Base64Variants.PEM
        };

        JsonFactory jsonFactory = new JsonFactory();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter chars = null;
        for (int len : LENS) {
            byte[] input = new byte[len];
            for (int i = 0; i < input.length; ++i) {
                input[i] = (byte) i;
            }
            for (Base64Variant variant : VARIANTS) {
                JsonGenerator jgen;
                if (useBytes) {
                    bytes.reset();
                    jgen = jsonFactory.createJsonGenerator(bytes, JsonEncoding.UTF8);
                } else {
                    chars = new StringWriter();
                    jgen = jsonFactory.createJsonGenerator(chars);
                }
                jgen.writeBinary(variant, input, 0, input.length);
                jgen.close();
                JsonParser jp;
                if (useBytes) {
                    jp = jsonFactory.createJsonParser(bytes.toByteArray());
                } else {
                    jp = jsonFactory.createJsonParser(chars.toString());
                }
                assertToken(JsonToken.VALUE_STRING, jp.nextToken());
                byte[] data = null;
                try {
                    data = jp.getBinaryValue(variant);
                } catch (Exception e) {
                    throw new IOException("Failed (variant "+variant+", data length "+len+"): "+e.getMessage(), e);
                }
                assertNotNull(data);
                assertArrayEquals(data, input);
                assertNull(jp.nextToken());
                jp.close();
            }
        }
    }

}
