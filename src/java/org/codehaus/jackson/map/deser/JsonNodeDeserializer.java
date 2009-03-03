package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;

/**
 * Deserializer that can build instances of {@link JsonNode} from any
 * Json content.
 */
public class JsonNodeDeserializer
    extends StdDeserializer<JsonNode>
{
    public final static JsonNodeDeserializer instance = new JsonNodeDeserializer();

    protected JsonNodeFactory _nodeFactory;

    public JsonNodeDeserializer()
    {
        super(JsonNode.class);
        _nodeFactory = JsonNodeFactory.instance;
    }

    public JsonNodeFactory getNodeFactory() { return _nodeFactory; }
    public void setNodeFactory(JsonNodeFactory nf) { _nodeFactory = nf; }

    /*
    /////////////////////////////////////////////////////
    // Actual deserializer implementation
    /////////////////////////////////////////////////////
     */

    public JsonNode deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        switch (jp.getCurrentToken()) {
        case START_OBJECT:
            {
                ObjectNode node = _nodeFactory.objectNode();
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = jp.getCurrentName();
                    jp.nextToken();
                    JsonNode value = deserialize(jp, ctxt);
                    JsonNode old = node.put(fieldName, value);
                    if (old != null) {
                        _handleDuplicateField(fieldName, node, old, value);
                    }
                }
                return node;
            }

        case START_ARRAY:
            {
                ArrayNode node = _nodeFactory.arrayNode();
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    node.add(deserialize(jp, ctxt));
                }
                return node;
            }

        case VALUE_STRING:
            return _nodeFactory.textNode(jp.getText());

        case VALUE_NUMBER_INT:
            if (jp.getNumberType() == JsonParser.NumberType.INT) {
                return _nodeFactory.numberNode(jp.getIntValue());
            }
            return _nodeFactory.numberNode(jp.getLongValue());

        case VALUE_NUMBER_FLOAT:
            if (jp.getNumberType() == JsonParser.NumberType.BIG_DECIMAL) {
                return _nodeFactory.numberNode(jp.getDecimalValue());
            }
            return _nodeFactory.numberNode(jp.getDoubleValue());

        case VALUE_TRUE:
            return _nodeFactory.booleanNode(true);

        case VALUE_FALSE:
            return _nodeFactory.booleanNode(false);

        case VALUE_NULL:
            return _nodeFactory.nullNode();

            // These states can not be mapped; input stream is
            // off by an event or two

        case FIELD_NAME:
        case END_OBJECT:
        case END_ARRAY:
        default:
            throw ctxt.mappingException(getValueClass());
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Overridable methods
    /////////////////////////////////////////////////////
     */

    protected void _reportProblem(JsonParser jp, String msg)
        throws JsonMappingException
    {
        throw new JsonMappingException(msg, jp.getTokenLocation());
    }

    /**
     * Method called when there is a duplicate value for a field.
     * By default we don't care, and the last value is used.
     * Can be overridden to provide alternate handling, such as throwing
     * an exception, or choosing different strategy for combining values
     * or choosing which one to keep.
     *
     * @param fieldName Name of the field for which duplicate value was found
     * @param objectNode Object node that contains values
     * @param oldValue Value that existed for the object node before newValue
     *   was added
     * @param newValue Newly added value just added to the object node
     */
    protected void _handleDuplicateField(String fieldName, ObjectNode objectNode,
                                         JsonNode oldValue, JsonNode newValue)
        throws JsonProcessingException
    {
        // By default, we don't do anything
        ;
    }
}
