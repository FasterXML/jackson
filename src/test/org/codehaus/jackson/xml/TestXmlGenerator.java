package org.codehaus.jackson.xml;

import java.io.*;

import javax.xml.namespace.QName;

public class TestXmlGenerator extends XmlTestBase
{
    public void testSimpleElement() throws Exception
    {
        XmlFactory f = new XmlFactory();
        StringWriter out = new StringWriter();
        ToXmlGenerator gen = f.createJsonGenerator(out);
        // root name is special, need to be fed first:
        gen.setNextName(new QName("root"));
        gen.writeStartObject();
        gen.writeFieldName("elem");
        gen.writeString("value");
        gen.writeEndObject();
        gen.close();
        String xml = out.toString();
        // one more thing: remove that annoying 'xmlns' decl, if it's there:
        xml = removeSjsxpNamespace(xml);
        assertEquals("<root><elem>value</elem></root>", xml);
    }

    public void testSimpleAttribute() throws Exception
    {
        XmlFactory f = new XmlFactory();
        StringWriter out = new StringWriter();
        ToXmlGenerator gen = f.createJsonGenerator(out);
        // root name is special, need to be fed first:
        gen.setNextName(new QName("root"));
        gen.writeStartObject();
        // and also need to force attribute
        gen.setNextIsAttribute(true);
        gen.writeFieldName("attr");
        gen.writeString("value");
        gen.writeEndObject();
        gen.close();
        String xml = out.toString();
        // one more thing: remove that annoying 'xmlns' decl, if it's there:
        xml = removeSjsxpNamespace(xml);
        assertEquals("<root attr=\"value\"/>", xml);
    }

    public void testSecondLevelAttribute() throws Exception
    {
        XmlFactory f = new XmlFactory();
        StringWriter out = new StringWriter();
        ToXmlGenerator gen = f.createJsonGenerator(out);
        gen.setNextName(new QName("root"));
        gen.writeStartObject();
        gen.writeFieldName("elem");
        gen.writeStartObject();
        // and also need to force attribute
        gen.setNextIsAttribute(true);
        gen.writeFieldName("attr");
        gen.writeString("value");
        gen.writeEndObject();
        gen.writeEndObject();
        gen.close();
        String xml = out.toString();
        // one more thing: remove that annoying 'xmlns' decl, if it's there:
        xml = removeSjsxpNamespace(xml);
        assertEquals("<root><elem attr=\"value\"/></root>", xml);
    }

    public void testAttrAndElem() throws Exception
    {
        XmlFactory f = new XmlFactory();
        StringWriter out = new StringWriter();
        ToXmlGenerator gen = f.createJsonGenerator(out);
        gen.setNextName(new QName("root"));
        gen.writeStartObject();
        // and also need to force attribute
        gen.writeFieldName("attr");
        gen.setNextIsAttribute(true);
        gen.writeNumber(-3);

        // Also let's add child element as well
        gen.setNextIsAttribute(false);
        gen.writeFieldName("elem");
        gen.writeNumber(13);
        gen.writeEndObject();
        gen.close();
        String xml = removeSjsxpNamespace(out.toString());
        assertEquals("<root attr=\"-3\"><elem>13</elem></root>", xml);
    }
    
}
