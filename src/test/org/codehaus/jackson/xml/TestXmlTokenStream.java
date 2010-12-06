package org.codehaus.jackson.xml;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.jackson.xml.util.XmlTokenStream;

public class TestXmlTokenStream extends main.BaseTest
{
    public void testSimple() throws Exception
    {
        String XML = "<root><leaf id='123'>abc</leaf></root>";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        // must point to START_ELEMENT, so:
        sr.nextTag();
        XmlTokenStream tokens = new XmlTokenStream(sr, XML);
        assertEquals(XmlTokenStream.XML_START_ELEMENT, tokens.getCurrentToken());
        assertEquals("root", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_START_ELEMENT, tokens.next());
        assertEquals("leaf", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_ATTRIBUTE_NAME, tokens.next());
        assertEquals("id", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_ATTRIBUTE_VALUE, tokens.next());
        assertEquals("123", tokens.getText());
        assertEquals(XmlTokenStream.XML_TEXT, tokens.next());
        assertEquals("abc", tokens.getText());
        assertEquals(XmlTokenStream.XML_END_ELEMENT, tokens.next());
        assertEquals(XmlTokenStream.XML_END_ELEMENT, tokens.next());
        assertEquals(XmlTokenStream.XML_END, tokens.next());
    }

    public void testRootAttributes() throws Exception
    {
        String XML = "<root id='x' />";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        // must point to START_ELEMENT, so:
        sr.nextTag();
        XmlTokenStream tokens = new XmlTokenStream(sr, XML);
        assertEquals(XmlTokenStream.XML_START_ELEMENT, tokens.getCurrentToken());
        assertEquals("root", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_ATTRIBUTE_NAME, tokens.next());
        assertEquals("id", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_ATTRIBUTE_VALUE, tokens.next());
        assertEquals("x", tokens.getText());
        assertEquals(XmlTokenStream.XML_END_ELEMENT, tokens.next());
        assertEquals(XmlTokenStream.XML_END, tokens.next());
    }
    
    public void testEmptyTags() throws Exception
    {
        String XML = "<root><leaf /></root>";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        // must point to START_ELEMENT, so:
        sr.nextTag();
        XmlTokenStream tokens = new XmlTokenStream(sr, XML);
        assertEquals(XmlTokenStream.XML_START_ELEMENT, tokens.getCurrentToken());
        assertEquals("root", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_START_ELEMENT, tokens.next());
        assertEquals("leaf", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_END_ELEMENT, tokens.next());
        assertEquals(XmlTokenStream.XML_END_ELEMENT, tokens.next());
        assertEquals(XmlTokenStream.XML_END, tokens.next());
    }

    public void testNested() throws Exception
    {
        String XML = "<root><a><b><c>abc</c></b></a></root>";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        sr.nextTag();
        XmlTokenStream tokens = new XmlTokenStream(sr, XML);
        assertEquals(XmlTokenStream.XML_START_ELEMENT, tokens.getCurrentToken());
        assertEquals("root", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_START_ELEMENT, tokens.next());
        assertEquals("a", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_START_ELEMENT, tokens.next());
        assertEquals("b", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_START_ELEMENT, tokens.next());
        assertEquals("c", tokens.getLocalName());
        assertEquals(XmlTokenStream.XML_TEXT, tokens.next());
        assertEquals("abc", tokens.getText());
        assertEquals(XmlTokenStream.XML_END_ELEMENT, tokens.next());
        assertEquals(XmlTokenStream.XML_END_ELEMENT, tokens.next());
        assertEquals(XmlTokenStream.XML_END_ELEMENT, tokens.next());
        assertEquals(XmlTokenStream.XML_END_ELEMENT, tokens.next());
        assertEquals(XmlTokenStream.XML_END, tokens.next());
    }
    
}
