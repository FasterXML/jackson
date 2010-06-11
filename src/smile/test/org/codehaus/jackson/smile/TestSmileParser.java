package org.codehaus.jackson.smile;

import java.io.*;

import org.junit.Assert;

import static org.codehaus.jackson.smile.SmileConstants.*;

public class TestSmileParser
{
    public void testSimple() throws Exception
    {
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected SmileGenerator _generator(ByteArrayOutputStream result, boolean addHeader)
        throws IOException
    {
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.WRITE_HEADER, addHeader);
        return f.createJsonGenerator(result, null);
    }
}
