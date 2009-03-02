package org.codehaus.jackson.map;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;

/**
 * This mapper (or, codec) provides mapping between Json,
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
 */
public class TreeMapper
    implements JsonNodeFactory
{
    /**
     * Enumeration that defines strategies available for dealing with
     * duplicate field names (when mapping JSON to Java types).
     */
    public enum DupFields {
        ERROR /* default */
            , USE_FIRST
            , USE_LAST
            ;
    }

    /**
     * This option defines how duplicate field names (from JSON input)
     * are to be handled. Default is to throw a {@link JsonParseException}.
     */
    protected DupFields _cfgDupFields = DupFields.ERROR;

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
    // JsonNodeFactory implementation
    ////////////////////////////////////////////////////
     */

    public ArrayNode arrayNode() { return new ArrayNode(this); }
    public ObjectNode objectNode() { return new ObjectNode(this); }
    public POJONode POJONode(Object pojo) { return new POJONode(pojo); }
    public NullNode nullNode() { return NullNode.getInstance(); }

    public TextNode textNode(String text) { return TextNode.valueOf(text); }

    public BinaryNode binaryNode(byte[] data) { return BinaryNode.valueOf(data); }
    public BinaryNode binaryNode(byte[] data, int offset, int length) {
        return BinaryNode.valueOf(data, offset, length);
    }

    public BooleanNode booleanNode(boolean v) {
        return v ? BooleanNode.getTrue() : BooleanNode.getFalse();
    }

    public NumericNode numberNode(byte v) { return IntNode.valueOf(v); }
    public NumericNode numberNode(short v) { return IntNode.valueOf(v); }
    public NumericNode numberNode(int v) { return IntNode.valueOf(v); }
    public NumericNode numberNode(long v) { return LongNode.valueOf(v); }
    public NumericNode numberNode(float v) { return DoubleNode.valueOf((double) v); }
    public NumericNode numberNode(double v) { return DoubleNode.valueOf(v); }
    public NumericNode numberNode(BigDecimal v) { return DecimalNode.valueOf(v); }

    /*
    ////////////////////////////////////////////////////
    // Public API, constructing in-memory trees
    ////////////////////////////////////////////////////
     */

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
