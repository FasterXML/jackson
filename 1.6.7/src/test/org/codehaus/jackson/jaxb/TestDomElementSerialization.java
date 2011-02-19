package org.codehaus.jackson.jaxb;

import main.BaseTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.CustomDeserializerFactory;
import org.codehaus.jackson.map.deser.StdDeserializerProvider;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.codehaus.jackson.xc.DomElementJsonDeserializer;
import org.codehaus.jackson.xc.DomElementJsonSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;

/**
 * @author Ryan Heaton
 */
public class TestDomElementSerialization extends BaseTest
{
    public void testBasicDomElementSerializationDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();
        sf.addGenericMapping(Element.class, new DomElementJsonSerializer());
        CustomDeserializerFactory df = new CustomDeserializerFactory();
        df.addSpecificMapping(Element.class, new DomElementJsonDeserializer());
        mapper.setSerializerFactory(sf);
        mapper.setDeserializerProvider(new StdDeserializerProvider(df));

        StringBuilder builder = new StringBuilder()
                .append("<document xmlns=\"urn:hello\" att1=\"value1\" att2=\"value2\">")
                .append("<childel>howdy</childel>")
                .append("</document>");

        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        bf.setNamespaceAware(true);
        Document document = bf.newDocumentBuilder().parse(new ByteArrayInputStream(builder.toString().getBytes("utf-8")));
        StringWriter jsonElement = new StringWriter();
        mapper.writeValue(jsonElement, document.getDocumentElement());
//        System.out.println(jsonElement.toString());

        Element el = mapper.readValue(jsonElement.toString(), Element.class);
        assertEquals(3, el.getAttributes().getLength());
        assertEquals("value1", el.getAttributeNS(null, "att1"));
        assertEquals("value2", el.getAttributeNS(null, "att2"));
        assertEquals(1, el.getChildNodes().getLength());
        assertEquals("childel", el.getChildNodes().item(0).getLocalName());
        assertEquals("urn:hello", el.getChildNodes().item(0).getNamespaceURI());
        assertEquals("howdy", el.getChildNodes().item(0).getTextContent());
    }
}
