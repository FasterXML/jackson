package org.codehaus.jackson.smile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;

public class BaseSmileTest
{
    protected SmileParser _parser(byte[] input)
        throws IOException
    {
        SmileFactory f = new SmileFactory();
        return f.createJsonParser(input);
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
