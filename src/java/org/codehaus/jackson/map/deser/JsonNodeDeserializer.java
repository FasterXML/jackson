package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;

/**
 * Deserializer that can build instances of {@link JsonNode} from any
 * Json content.
 */
public class JsonNodeDeserializer
    extends StdDeserializer<JsonNode>
    implements JsonNodeFactory
{
    public final static JsonNodeDeserializer instance = new JsonNodeDeserializer();

    public JsonNodeDeserializer() { super(JsonNode.class); }

    /*
    /////////////////////////////////////////////////////
    // Factory methods for JsonNodeFactory, exposed and used
    // by the mapper
    // (can also be overridden by sub-classes for extra
    // functionality)
    /////////////////////////////////////////////////////
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
                ObjectNode node = objectNode();
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
                ArrayNode node = arrayNode();
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    node.add(deserialize(jp, ctxt));
                }
                return node;
            }

        case VALUE_STRING:
            return textNode(jp.getText());

        case VALUE_NUMBER_INT:
            if (jp.getNumberType() == JsonParser.NumberType.INT) {
                return numberNode(jp.getIntValue());
            }
            return numberNode(jp.getLongValue());

        case VALUE_NUMBER_FLOAT:
            if (jp.getNumberType() == JsonParser.NumberType.BIG_DECIMAL) {
                return numberNode(jp.getDecimalValue());
            }
            return numberNode(jp.getDoubleValue());

        case VALUE_TRUE:
            return booleanNode(true);

        case VALUE_FALSE:
            return booleanNode(false);

        case VALUE_NULL:
            return nullNode();

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
