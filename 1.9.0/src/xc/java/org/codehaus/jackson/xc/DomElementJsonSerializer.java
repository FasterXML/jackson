package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.codehaus.jackson.node.ObjectNode;
import org.w3c.dom.*;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author Ryan Heaton
 */
public class DomElementJsonSerializer
        extends SerializerBase<Element>
{
    public DomElementJsonSerializer() { super(Element.class); }

    @Override
    public void serialize(Element value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
    {
        jgen.writeStartObject();
        jgen.writeStringField("name", value.getTagName());
        if (value.getNamespaceURI() != null) {
            jgen.writeStringField("namespace", value.getNamespaceURI());
        }
        NamedNodeMap attributes = value.getAttributes();
        if (attributes != null && attributes.getLength() > 0) {
            jgen.writeArrayFieldStart("attributes");
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr) attributes.item(i);
                jgen.writeStartObject();
                jgen.writeStringField("$", attribute.getValue());
                jgen.writeStringField("name", attribute.getName());
                String ns = attribute.getNamespaceURI();
                if (ns != null) {
                    jgen.writeStringField("namespace", ns);
                }
                jgen.writeEndObject();
            }
            jgen.writeEndArray();
        }

        NodeList children = value.getChildNodes();
        if (children != null && children.getLength() > 0) {
            jgen.writeArrayFieldStart("children");
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                switch (child.getNodeType()) {
                    case Node.CDATA_SECTION_NODE:
                    case Node.TEXT_NODE:
                        jgen.writeStartObject();
                        jgen.writeStringField("$", child.getNodeValue());
                        jgen.writeEndObject();
                        break;
                    case Node.ELEMENT_NODE:
                        serialize((Element) child, jgen, provider);
                        break;
                }
            }
            jgen.writeEndArray();
        }
        jgen.writeEndObject();
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
    {
        ObjectNode o = createSchemaNode("object", true);
        o.put("name", createSchemaNode("string"));
        o.put("namespace", createSchemaNode("string", true));
        o.put("attributes", createSchemaNode("array", true));
        o.put("children", createSchemaNode("array", true));
        return o;
    }
}
