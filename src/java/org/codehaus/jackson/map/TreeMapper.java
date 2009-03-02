package org.codehaus.jackson.map;

import java.io.*;
import java.net.URL;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.TreeMapperBase;

/**
 * This mapper (or, codec) provides mapping between Json,
 * and Tree-like structure that consists of child-linked 
 * nodes that can be traversed with simple path operations
 * (indexing arrays by element, objects by field name).
 *<p>
 * As of version 0.9.9 of Jackson, this class is basically
 * deprecate, since all of its functionality is implemented
 * by {@link ObjectMapper}, and accessible through either
 * {@link ObjectMapper} or {@link JsonParser} and
 * {@link JsonGenerator}.
 *
 * @deprecated Use {@link JsonNode} functionality offered by
 * {@link ObjectMapper}, {@link JsonParser} and {@link JsonGenerator}
 * instead
 *
 */
@Deprecated
public class TreeMapper
    extends TreeMapperBase
{
    /**
     * Mapper that handles actual serialization/deserialization
     */
    protected final ObjectMapper _objectMapper;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (construction, configuration)
    ////////////////////////////////////////////////////
     */

    public TreeMapper()
    {
        this(new ObjectMapper());
    }

    public TreeMapper(ObjectMapper m)
    {
        _objectMapper = m;
    }

    /**
     * Method that can be used to get hold of Json factory that this
     * mapper uses if it needs to construct Json parsers and/or generators.
     *
     * @return Json factory that this mapper uses when it needs to
     *   construct Json parser and generators
     */
    public JsonFactory getJsonFactory() { return _objectMapper.getJsonFactory(); }

    /*
    ////////////////////////////////////////////////////
    // Public API, root-level mapping methods,
    // mapping from JSON content to nodes
    ////////////////////////////////////////////////////
     */

    public JsonNode readTree(JsonParser jp)
        throws IOException, JsonParseException
    {
        return _objectMapper.readValue(jp, JsonNode.class);
    }

    public JsonNode readTree(File src)
        throws IOException, JsonParseException
    {
        return _objectMapper.readValue(src, JsonNode.class);
    }

    public JsonNode readTree(URL src)
        throws IOException, JsonParseException
    {
        return _objectMapper.readValue(src, JsonNode.class);
    }

    public JsonNode readTree(InputStream src)
        throws IOException, JsonParseException
    {
        return _objectMapper.readValue(src, JsonNode.class);
    }

    public JsonNode readTree(Reader src)
        throws IOException, JsonParseException
    {
        return _objectMapper.readValue(src, JsonNode.class);
    }

    public JsonNode readTree(String jsonContent)
        throws IOException, JsonParseException
    {
        return _objectMapper.readValue(jsonContent, JsonNode.class);
    }

    public JsonNode readTree(byte[] jsonContent)
        throws IOException, JsonParseException
    {
        return _objectMapper.readValue(jsonContent, 0, jsonContent.length, JsonNode.class);
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, root-level mapping methods,
    // writing nodes as JSON content
    ////////////////////////////////////////////////////
     */

    public void writeTree(JsonNode rootNode, File dst)
        throws IOException, JsonParseException
    {
        _objectMapper.writeValue(dst, rootNode);
    }

    public void writeTree(JsonNode rootNode, Writer dst)
        throws IOException, JsonParseException
    {
        _objectMapper.writeValue(dst, rootNode);
    }

    public void writeTree(JsonNode rootNode, OutputStream dst)
        throws IOException, JsonParseException
    {
        _objectMapper.writeValue(dst, rootNode);
    }
}
