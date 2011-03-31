package org.codehaus.jackson.map;

import java.io.*;
import java.net.URL;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.NullNode;

/**
 * This mapper (or, codec) provides mapping between JSON,
 * and Tree-like structure that consists of child-linked 
 * nodes that can be traversed with simple path operations
 * (indexing arrays by element, objects by field name).
 *<p>
 * As of version 0.9.9 of Jackson, most functionality has
 * been moved away from this class: all serialization and
 * deserialization features are now accessible through either
 * {@link ObjectMapper} or {@link JsonParser} and
 * {@link JsonGenerator}. The remaining functionality is limited
 * to {@link JsonNodeFactory} implementation which allows constructing
 * typed {@link JsonNode} instances.
 *
 * @deprecated since 1.0, use {@link org.codehaus.jackson.map.ObjectMapper} instead
 */
@Deprecated
public class TreeMapper
    extends JsonNodeFactory
{
    /**
     * Mapper that handles actual serialization/deserialization
     */
    protected ObjectMapper _objectMapper;

    /*
    /**********************************************************
    /* Life-cycle (construction, configuration)
    /**********************************************************
     */

    public TreeMapper()
    {
        this(null);
    }

    public TreeMapper(ObjectMapper m)
    {
        _objectMapper = m;
    }

    /**
     * Method that can be used to get hold of JSON factory that this
     * mapper uses if it needs to construct JSON parsers and/or generators.
     *
     * @return JSON factory that this mapper uses when it needs to
     *   construct JSON parser and generators
     */
    public JsonFactory getJsonFactory() { return objectMapper().getJsonFactory(); }

    /*
    /**********************************************************
    /* Public API, constructing in-memory trees
    /**********************************************************
     */

    /*
    /**********************************************************
    /* Public API, root-level mapping methods,
    /* mapping from JSON content to nodes
    /**********************************************************
     */

    /**
     * Method that will try to read a sub-tree using given parser,
     * map it to a tree (represented by a root JsonNode) and return
     * it, if possible. Alternatively, if no content is available,
     * null is returned to signal end-of-content.
     */
    public JsonNode readTree(JsonParser jp)
        throws IOException, JsonParseException
    {
        /* 02-Mar-2009, tatu: Behavior here is bit different from that
         *   of ObjectMapper, since we can actually return null to
         *   indicate end-of-content. Because of this we do need to
         *   check for EOF here and not let ObjectMapper encounter
         *   it (since that would throw an exception)
         */
        JsonToken t = jp.getCurrentToken();
        if (t == null) {
            t = jp.nextToken();
            if (t == null) {
                return null;
            }
        }
        // note: called method converts null to NullNode:
        return objectMapper().readTree(jp);
    }

    public JsonNode readTree(File src)
        throws IOException, JsonParseException
    {

        JsonNode n = objectMapper().readValue(src, JsonNode.class);
        return (n == null) ? NullNode.instance : n;
    }

    public JsonNode readTree(URL src)
        throws IOException, JsonParseException
    {
        JsonNode n = objectMapper().readValue(src, JsonNode.class);
        return (n == null) ? NullNode.instance : n;
    }

    public JsonNode readTree(InputStream src)
        throws IOException, JsonParseException
    {
        JsonNode n = objectMapper().readValue(src, JsonNode.class);
        return (n == null) ? NullNode.instance : n;
    }

    public JsonNode readTree(Reader src)
        throws IOException, JsonParseException
    {
        JsonNode n = objectMapper().readValue(src, JsonNode.class);
        return (n == null) ? NullNode.instance : n;
    }

    public JsonNode readTree(String jsonContent)
        throws IOException, JsonParseException
    {
        JsonNode n = objectMapper().readValue(jsonContent, JsonNode.class);
        return (n == null) ? NullNode.instance : n;
    }

    public JsonNode readTree(byte[] jsonContent)
        throws IOException, JsonParseException
    {
        JsonNode n = objectMapper().readValue(jsonContent, 0, jsonContent.length, JsonNode.class);
        return (n == null) ? NullNode.instance : n;
    }

    /*
    /**********************************************************
    /* Public API, root-level mapping methods,
    /* writing nodes as JSON content
    /**********************************************************
     */

    public void writeTree(JsonNode rootNode, File dst)
        throws IOException, JsonParseException
    {
        objectMapper().writeValue(dst, rootNode);
    }

    public void writeTree(JsonNode rootNode, Writer dst)
        throws IOException, JsonParseException
    {
        objectMapper().writeValue(dst, rootNode);
    }

    public void writeTree(JsonNode rootNode, OutputStream dst)
        throws IOException, JsonParseException
    {
        objectMapper().writeValue(dst, rootNode);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected synchronized ObjectMapper objectMapper()
    {
        if (_objectMapper == null) {
            _objectMapper = new ObjectMapper();
        }
        return _objectMapper;
    }
}
