package org.codehaus.jackson.main;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.*;

/**
 * Set of basic unit tests for verifying basic generator
 * features.
 */
public class TestJsonGenerator
    extends main.BaseTest
{
    final static class Pojo
    {
        public int getX() { return 4; }
    }

    public void testPojoWriting()
        throws IOException
    {
        JsonFactory jf = new MappingJsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createJsonGenerator(sw);
        gen.writeObject(new Pojo());
        gen.close();
        // trimming needed if main-level object has leading space
        String act = sw.toString().trim();
        assertEquals("{\"x\":4}", act);
    }

    public void testPojoWritingFailing()
        throws IOException
    {
        // regular factory can't do it, without a call to setCodec()
        JsonFactory jf = new JsonFactory();
        try {
            StringWriter sw = new StringWriter();
            JsonGenerator gen = jf.createJsonGenerator(sw);
            gen.writeObject(new Pojo());
            gen.close();
            fail("Expected an exception: got sw '"+sw.toString()+"'");
        } catch (IllegalStateException e) {
            verifyException(e, "No ObjectCodec defined");
        }
    }
}
