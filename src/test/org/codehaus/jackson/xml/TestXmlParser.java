package org.codehaus.jackson.xml;

import java.io.*;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;

public class TestXmlParser extends main.BaseTest
{
    public void testSimplest() throws Exception
    {
        assertEquals("{\"leaf\":\"abc\"}",
                _readXmlWriteJson("<root><leaf>abc</leaf></root>"));
    }

    public void testSimpleWithEmpty() throws Exception
    {
        assertEquals("{\"leaf\":null}",
                _readXmlWriteJson("<root><leaf /></root>"));
    }

    public void testSimpleNested() throws Exception
    {
        assertEquals("{\"a\":{\"b\":{\"c\":\"xyz\"}}}",
                _readXmlWriteJson("<root><a><b><c>xyz</c></b></a></root>"));
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private String _readXmlWriteJson(String xml) throws IOException
    {
        JsonFactory jf = new JsonFactory();
        XmlFactory xf = new XmlFactory();
        StringWriter w = new StringWriter();

        JsonParser jp = xf.createJsonParser(xml);
        JsonGenerator jg = jf.createJsonGenerator(w);
        while (jp.nextToken() != null) {
            jg.copyCurrentEvent(jp);
        }
        jg.close();
        return w.toString();
    }
}
