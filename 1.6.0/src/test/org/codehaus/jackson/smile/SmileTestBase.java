package org.codehaus.jackson.smile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;

import org.codehaus.jackson.*;

abstract class SmileTestBase
    extends main.BaseTest
{
    protected SmileParser _smileParser(byte[] input) throws IOException {
        return _smileParser(input, false);
    }

    protected SmileParser _smileParser(byte[] input, boolean requireHeader) throws IOException
    {
        SmileFactory f = smileFactory(requireHeader, false, false);
    	return _smileParser(f, input);
    }

    protected SmileParser _smileParser(SmileFactory f, byte[] input)
        throws IOException
    {
        return f.createJsonParser(input);
    }
    
    protected SmileFactory smileFactory(boolean requireHeader,
            boolean writeHeader, boolean writeEndMarker)
        throws IOException
    {
        SmileFactory f = new SmileFactory();
        f.configure(SmileParser.Feature.REQUIRE_HEADER, requireHeader);
        f.configure(SmileGenerator.Feature.WRITE_HEADER, writeHeader);
        f.configure(SmileGenerator.Feature.WRITE_END_MARKER, writeEndMarker);
        return f;
    }
    
    protected byte[] _smileDoc(String json) throws IOException
    {
    	return _smileDoc(json, true);
    }

    protected byte[] _smileDoc(String json, boolean writeHeader) throws IOException
    {
        JsonFactory jf = new JsonFactory();
    	JsonParser jp = jf.createJsonParser(json);
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	JsonGenerator jg = smileGenerator(out, writeHeader);
    	
    	while (jp.nextToken() != null) {
    		jg.copyCurrentEvent(jp);
    	}
    	jp.close();
    	jg.close();
    	return out.toByteArray();
    }
    
    protected SmileGenerator smileGenerator(ByteArrayOutputStream result, boolean addHeader)
        throws IOException
    {
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.WRITE_HEADER, addHeader);
        return f.createJsonGenerator(result, null);
    }
    
    protected void _verifyBytes(byte[] actBytes, byte... expBytes)
    {
        Assert.assertArrayEquals(expBytes, actBytes);
    }
}
