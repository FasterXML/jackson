package org.codehaus.jackson.smile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;

import org.codehaus.jackson.*;

class SmileTestBase
	extends main.BaseTest
{
    protected SmileParser _parser(byte[] input)
        throws IOException
    {
    	SmileFactory f = new SmileFactory();
        return f.createJsonParser(input);
    }

    protected byte[] _smileDoc(String json) throws IOException
    {
    	return _smileDoc(json, true);
    }

    protected byte[] _smileDoc(String json, boolean writeHeader) throws IOException
    {
        JsonFactory jf = new JsonFactory();
    	JsonParser jp = jf.createJsonParser(json);
    	SmileFactory sf = new SmileFactory();
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	JsonGenerator jg = sf.createJsonGenerator(out, JsonEncoding.UTF8);
    	
    	while (jp.nextToken() != null) {
    		jg.copyCurrentEvent(jp);
    	}
    	jp.close();
    	jg.close();
    	return out.toByteArray();
    }
    
    protected SmileGenerator _generator(ByteArrayOutputStream result, boolean addHeader)
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
