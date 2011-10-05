package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.node.ArrayNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Ryan Heaton
 */
public class DomElementJsonDeserializer
    extends StdDeserializer<Element>
{
    private final DocumentBuilder builder;

    public DomElementJsonDeserializer()
    {
        super(Element.class);
        try {
            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            builder = bf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException();
        }
    }

    public DomElementJsonDeserializer(DocumentBuilder builder)
    {
        super(Element.class);
        this.builder = builder;
    }

    @Override
    public Element deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        Document document = builder.newDocument();
        return fromNode(document, jp.readValueAsTree());
    }

    protected Element fromNode(Document document, JsonNode jsonNode)
            throws IOException
    {
        String ns = jsonNode.get("namespace") != null ? jsonNode.get("namespace").asText() : null;
        String name = jsonNode.get("name") != null ? jsonNode.get("name").asText() : null;
        if (name == null) {
            throw new JsonMappingException("No name for DOM element was provided in the JSON object.");
        }
        Element element = document.createElementNS(ns, name);

        JsonNode attributesNode = jsonNode.get("attributes");
        if (attributesNode != null && attributesNode instanceof ArrayNode) {
            Iterator<JsonNode> atts = attributesNode.getElements();
            while (atts.hasNext()) {
                JsonNode node = atts.next();
                ns = node.get("namespace") != null ? node.get("namespace").asText() : null;
                name = node.get("name") != null ? node.get("name").asText() : null;
                String value = node.get("$") != null ? node.get("$").asText() : null;

                if (name != null) {
                    element.setAttributeNS(ns, name, value);
                }
            }
        }

        JsonNode childsNode = jsonNode.get("children");
        if (childsNode != null && childsNode instanceof ArrayNode) {
            Iterator<JsonNode> els = childsNode.getElements();
            while (els.hasNext()) {
                JsonNode node = els.next();
                name = node.get("name") != null ? node.get("name").asText() : null;
                String value = node.get("$") != null ? node.get("$").asText() : null;

                if (value != null) {
                    element.appendChild(document.createTextNode(value));
                }
                else if (name != null) {
                    element.appendChild(fromNode(document, node));
                }
            }
        }

        return element;
    }
}
