package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.io.InputDecorator;
import org.codehaus.jackson.io.OutputDecorator;

/**
 * Unit tests to verify that input and output decorators work as
 * expected
 * 
 * @since 1.8
 */
public class TestDecorators extends main.BaseTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class SimpleInputDecorator extends InputDecorator
    {
        @Override
        public InputStream decorate(IOContext ctxt, InputStream in)
            throws IOException
        {
            return new ByteArrayInputStream("123".getBytes("UTF-8"));
        }

        @Override
        public InputStream decorate(IOContext ctxt, byte[] src, int offset, int length)
            throws IOException
        {
            return new ByteArrayInputStream("456".getBytes("UTF-8"));
        }

        @Override
        public Reader decorate(IOContext ctxt, Reader src) {
            return new StringReader("789");
        }
    }

    static class SimpleOutputDecorator extends OutputDecorator
    {
        @Override
        public OutputStream decorate(IOContext ctxt, OutputStream out) throws IOException
        {
            out.write("123".getBytes("UTF-8"));
            out.flush();
            return new ByteArrayOutputStream();
        }

        @Override
        public Writer decorate(IOContext ctxt, Writer w) throws IOException
        {
            w.write("567");
            w.flush();
            return new StringWriter();
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testInputDecoration() throws IOException
    {
        JsonFactory f = new JsonFactory();
        f.setInputDecorator(new SimpleInputDecorator());
        JsonParser jp;
        // first test with Reader
        jp = f.createJsonParser(new StringReader("{ }"));
        // should be overridden;
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(789, jp.getIntValue());
        jp.close();

        // similarly with InputStream
        jp = f.createJsonParser(new ByteArrayInputStream("[ ]".getBytes("UTF-8")));
        // should be overridden;
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(123, jp.getIntValue());
        jp.close();

        // and with raw bytes
        jp = f.createJsonParser("[ ]".getBytes("UTF-8"));
        // should be overridden;
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(456, jp.getIntValue());
        jp.close();
    }

    public void testOutputDecoration() throws IOException
    {
        JsonFactory f = new JsonFactory();
        f.setOutputDecorator(new SimpleOutputDecorator());
        JsonGenerator jg;

        StringWriter sw = new StringWriter();
        jg = f.createJsonGenerator(sw);
        jg.close();
        assertEquals("567", sw.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        jg = f.createJsonGenerator(out, JsonEncoding.UTF8);
        jg.close();
        assertEquals("123", out.toString("UTF-8"));
    }
}
