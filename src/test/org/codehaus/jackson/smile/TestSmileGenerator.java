package org.codehaus.jackson.smile;

import java.io.*;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

public class TestSmileGenerator
    extends main.BaseTest
{
    public void testSimpleLiterals()
        throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected JsonGenerator _generator(ByteArrayOutputStream result, boolean addHeader)
        throws IOException
    {
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.WRITE_HEADER, addHeader);
        return f.createJsonGenerator(result, null);
    }
}
