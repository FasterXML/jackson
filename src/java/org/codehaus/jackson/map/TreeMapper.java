package org.codehaus.jackson.map;

import java.io.*;
import java.net.URL;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.node.TreeMapperBase;

/**
 * This mapper (or, codec) provides mapping between JSON,
 * and Tree-like structure that consists of child-linked 
 * nodes that can be traversed with simple path operations
 * (indexing arrays by element, objects by field name).
 *<p>
 * The main difference to {@link ObjectMapper} is that
 * no casting should ever be necessary, and as such
 * access is more convenient if expected structure is
 * known in advance. Typing in general is simple, 
 * since only the base node type is needed for
 * all operations.
 *<p>
 * Thing to note about serializing (writing) json types:
 * mapper does not add specific support, since
 * {@link JsonNode} instances already have
 * {@link JsonNode#writeTo} method.
 */
public class TreeMapper
    extends TreeMapperBase
{
    /*
    ////////////////////////////////////////////////////
    // Life-cycle (construction, configuration)
    ////////////////////////////////////////////////////
     */

    public TreeMapper()
    {
        this(null);
    }

    public TreeMapper(JsonFactory jf)
    {
        super(jf);
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, root-level mapping methods,
    // mapping from JSON content to nodes
    ////////////////////////////////////////////////////
     */

    public JsonNode readTree(File src)
        throws IOException, JsonParseException
    {
        return _readMapAndClose(_jsonFactory.createJsonParser(src));
    }

    public JsonNode readTree(URL src)
        throws IOException, JsonParseException
    {
        return _readMapAndClose(_jsonFactory.createJsonParser(src));
    }

    public JsonNode readTree(InputStream src)
        throws IOException, JsonParseException
    {
        return _readMapAndClose(_jsonFactory.createJsonParser(src));
    }

    public JsonNode readTree(Reader src)
        throws IOException, JsonParseException
    {
        return _readMapAndClose(_jsonFactory.createJsonParser(src));
    }

    public JsonNode readTree(String jsonContent)
        throws IOException, JsonParseException
    {
        return _readMapAndClose(_jsonFactory.createJsonParser(jsonContent));
    }

    public JsonNode readTree(byte[] jsonContent)
        throws IOException, JsonParseException
    {
        return _readMapAndClose(_jsonFactory.createJsonParser(jsonContent));
    }

    /**
     * Method that will use the current event of the underlying parser
     * (and if there's no event yet, tries to advance to an event)
     * to construct a node, and advance the parser to point to the
     * next event, if any. For structured tokens (objects, arrays),
     * will recursively handle and construct contained nodes.
     */
    public JsonNode readTree(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        JsonToken curr = jp.getCurrentToken();
        if (curr == null) {
            curr  = jp.nextToken();
            // We hit EOF? Nothing more to do, if so:
            if (curr == null) {
                return null;
            }
        }

        JsonNode result = readAndMap(jp, curr);

        /* Need to also advance the reader, if we get this far,
         * to allow handling of root level sequence of values
         */
        jp.nextToken();
        return result;
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
        _writeNodeAndClose(_jsonFactory.createJsonGenerator(dst, JsonEncoding.UTF8), rootNode);
    }

    public void writeTree(JsonNode rootNode, Writer dst)
        throws IOException, JsonParseException
    {
        _writeNodeAndClose(_jsonFactory.createJsonGenerator(dst), rootNode);
    }

    public void writeTree(JsonNode rootNode, OutputStream dst)
        throws IOException, JsonParseException
    {
        _writeNodeAndClose(_jsonFactory.createJsonGenerator(dst, JsonEncoding.UTF8), rootNode);
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, exposing JsonNode(s) via stream
    // parsers/generators
    ////////////////////////////////////////////////////
     */

    /**
     * Method that will take in a Node object and construct
     * a {@link JsonParser} that can be used to traverse json
     * content as streaming events.
     * Method is meant for interoperability use cases, in cases where
     * some code expects a {@link JsonParser}, but you already
     * have content parser into {@link JsonNode} structure.
     * This is more efficient than writing intermediate textual
     * JSON serialization and parsing it via regular json stream reader.
     *<p>
     * Note that this should (and can) <b>NOT</b> be used to parse
     * textual json content: for that purpose you should 
     * use {@link org.codehaus.jackson.JsonFactory#createJsonParser(java.io.InputStream)}
     * instead.
     */
    /*
    public JsonParser createParserFor(JsonNode node)
        throws JsonParseException
    {
        // !!! TBI: parser for reading from JsonNode (array/map, primitives)
        throw new UnsupportedOperationException();
    }
    */

    /**
     * Method that will construct a JSON generator that will build
     * {@link JsonNode}s when generator's write methods are called.
     * Method is meant for interoperability use cases, in cases where
     * some code expects a {@link JsonGenerator} to use, but the output
     * should be in {@link JsonNode} structure.
     * This is more efficient than writing intermediate textual
     * JSON serialization and parsing and mapping it to
     * {@link JsonNode} based structure.
     *<p>
     * Note that this should (and can) <b>NOT</b> be used to output regular
     * textual json content: for that purpose you should 
     * use {@link org.codehaus.jackson.JsonFactory#createJsonGenerator(java.io.Writer)}
     * instead.
     */
    /*
    public JsonGenerator createGeneratorFor(JsonNode context)
        throws JsonParseException
    {
        // !!! TBI: generator for writing (appending) to Objects (array/map, primitives)
        throw new UnsupportedOperationException();
    }
    */

    /*
    ////////////////////////////////////////////////////
    // Factory methods
    ////////////////////////////////////////////////////
     */

    // Note: these come straight from the base class:

    // public ArrayNode arrayNode()
    // public ObjectNode objectNode()
    // public NullNode nullNode()

    // public TextNode textNode(String text)
    // public BinaryNode binaryNode(byte[] data)

    // public BooleanNode booleanNode(boolean v)

    //public NumericNode numberNode(int v)
    //public NumericNode numberNode(long v)
    //public NumericNode numberNode(double v)
    //public NumericNode numberNode(BigDecimal v)

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    /**
     * Method used to read and map Json content read from passed-in
     * parser, and then close the parser. This is done when caller
     * passed as a source instead of a parser; in which case mapper
     * is responsible for closing resources.
     */
    protected JsonNode _readMapAndClose(JsonParser jp)
        throws IOException, JsonParseException
    {
        try {
            return readTree(jp);
        } finally {
            try {
                jp.close();
            } catch (IOException ioe) { }
        }
    }

    /**
     * Method called to serialize a Json content tree (identified
     * by the given root node) using given {@link JsonGenerator}.
     */
    protected void _writeNodeAndClose(JsonGenerator jg, JsonNode rootNode)
        throws IOException, JsonParseException
    {
        try {
            rootNode.writeTo(jg);
        } finally {
            try {
                jg.close();
            } catch (IOException ioe) { }
        }
    }
}
