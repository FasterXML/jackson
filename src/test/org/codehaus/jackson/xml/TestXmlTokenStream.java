package org.codehaus.jackson.xml;

import java.io.*;

import javax.xml.stream.*;

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
}
