package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.impl.JsonTypeMapperBase;

/**
 * This mapper (or, codec) provides mapping between JSON,
 * and Tree-like structure that consists of child-linked 
 * nodes that can be traversed with simple path operations
 * (indexing arrays by element, objects by field name).
 *<p>
 * The main difference to {@link JavaTypeMapper} is that
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
public class JsonTypeMapper
    extends JsonTypeMapperBase
{
    /*
    ////////////////////////////////////////////////////
    // Life-cycle (construction, configuration)
    ////////////////////////////////////////////////////
     */

    public JsonTypeMapper() { super(); }

    /*
    ////////////////////////////////////////////////////
    // Public API, root-level mapping methods,
    // mapping from JSON content to nodes
    ////////////////////////////////////////////////////
     */

    /**
     * Method that will use the current event of the underlying parser
     * (and if there's no event yet, tries to advance to an event)
     * to construct a node, and advance the parser to point to the
     * next event, if any. For structured tokens (objects, arrays),
     * will recursively handle and construct contained nodes.
     */
    public JsonNode read(JsonParser jp)
        throws IOException, JsonParseException
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
    // Public API, exposing JsonNode(s) via stream
    // parsers/generators
    ////////////////////////////////////////////////////
     */

    /**
     * Method that will take in a Java object that could have
     * been created by mappers write methods, and construct
     * a {@link JsonParser} that exposes contents as JSON
     * tokens
     */
    public JsonParser createParserFor(JsonNode node)
        throws JsonParseException
    {
        // !!! TBI: parser for reading from JsonNode (array/map, primitives)
        return null;
    }

    /**
     * Method that will create a JSON generator that will build
     * Java objects as members of the current list, appending
     * them at the end of the list.
     */
    public JsonGenerator createGeneratorFor(JsonNode context)
        throws JsonParseException
    {
        // !!! TBI: generator for writing (appending) to Objects (array/map, primitives)
        return null;
    }

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

    // public BooleanNode booleanNode(boolean v)

    // public IntNode intNode(int v)
    // public LongNode longNode(long v)
    // public DoubleNode doubleode(double v)
    // public DecimalNode decimalNode(BigDecimal v)

    /*
    ////////////////////////////////////////////////////
    // Internal methods, overridable
    ////////////////////////////////////////////////////
     */
}
