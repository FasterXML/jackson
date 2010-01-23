package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;

/**
 * Deserializer that can build instances of {@link JsonNode} from any
 * Json content, using appropriate {@link JsonNode} type.
 */
public class JsonNodeDeserializer
    extends BaseNodeDeserializer<JsonNode>
{
    /**
     * Singleton instance of generic deserializer for {@link JsonNode}
     *
     * @deprecated Use {@link #getDeserializer} accessor instead
     */
    @Deprecated
    public final static JsonNodeDeserializer instance = new JsonNodeDeserializer();

    protected JsonNodeDeserializer() { super(JsonNode.class); }

    public static JsonDeserializer<? extends JsonNode> getDeserializer(Class<?> nodeClass)
    {
        if (ObjectNode.class.isAssignableFrom(nodeClass)) {
            return ObjectDeserializer.instance;
        }
        if (ArrayNode.class.isAssignableFrom(nodeClass)) {
            return ArrayDeserializer.instance;
        }
        // For others, generic one works fine
        return JsonNodeDeserializer.instance;
    }
    
    /*
    /////////////////////////////////////////////////////
    // Actual deserializer implementations
    /////////////////////////////////////////////////////
    */

    /**
     * Implementation that will produce types of any JSON nodes; not just one
     * deserializer is registered to handle (in case of more specialized handler).
     * Overridden by typed sub-classes for more thorough checking
     */
    public JsonNode deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return deserializeAny(jp, ctxt);
    }

    /*
    /////////////////////////////////////////////////////
    // Specific instances for more accurate types
    /////////////////////////////////////////////////////
     */

    final static class ObjectDeserializer
        extends BaseNodeDeserializer<ObjectNode>
    {
        final static ObjectDeserializer instance = new ObjectDeserializer();

        protected ObjectDeserializer() {
            super(ObjectNode.class);
        }

        @Override
        public ObjectNode deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
                jp.nextToken();
                return deserializeObject(jp, ctxt);
            }
            if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
                return deserializeObject(jp, ctxt);
            }
            throw ctxt.mappingException(ObjectNode.class);
         }
    }
        
    final static class ArrayDeserializer
        extends BaseNodeDeserializer<ArrayNode>
    {
        final static ArrayDeserializer instance = new ArrayDeserializer();

        protected ArrayDeserializer() {
            super(ArrayNode.class);
        }
        
        @Override
        public ArrayNode deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (jp.getCurrentToken() == JsonToken.START_ARRAY) {
                return deserializeArray(jp, ctxt);
            }
            throw ctxt.mappingException(ArrayNode.class);
        }
    }
}

/**
 * Base class for all actual {@link JsonNode} deserializer
 * implementations
 */
abstract class BaseNodeDeserializer<N extends JsonNode>
    extends StdDeserializer<N>
{
    protected JsonNodeFactory _nodeFactory;
    
    public BaseNodeDeserializer(Class<N> nodeClass)
    {
        super(nodeClass);
        _nodeFactory = JsonNodeFactory.instance;
    }
    
    public JsonNodeFactory getNodeFactory() { return _nodeFactory; }
    public void setNodeFactory(JsonNodeFactory nf) { _nodeFactory = nf; }
    
    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        /* Output can be as JSON Object, Array or scalar: no way to know
         * a priori. So:
         */
        return typeDeserializer.deserializeTypedFromAny(jp, ctxt);
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
    
    /*
    /////////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////////
    */
    
    protected final ObjectNode deserializeObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        ObjectNode node = _nodeFactory.objectNode();
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            JsonNode value = deserializeAny(jp, ctxt);
            JsonNode old = node.put(fieldName, value);
            if (old != null) {
                _handleDuplicateField(fieldName, node, old, value);
            }
        }
        return node;
    }
    
    protected final ArrayNode deserializeArray(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        ArrayNode node = _nodeFactory.arrayNode();
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            node.add(deserializeAny(jp, ctxt));
        }
        return node;
    }

    protected final JsonNode deserializeAny(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        switch (jp.getCurrentToken()) {
        case START_OBJECT:
        case FIELD_NAME:
            return deserializeObject(jp, ctxt);

        case START_ARRAY:
            return deserializeArray(jp, ctxt);

        case VALUE_STRING:
            return _nodeFactory.textNode(jp.getText());

        case VALUE_NUMBER_INT:
            {
                JsonParser.NumberType nt = jp.getNumberType();
                if (nt == JsonParser.NumberType.BIG_INTEGER
                    || ctxt.isEnabled(DeserializationConfig.Feature.USE_BIG_INTEGER_FOR_INTS)) {
                    return _nodeFactory.numberNode(jp.getIntValue());
                }
                if (nt == JsonParser.NumberType.INT) {
                    return _nodeFactory.numberNode(jp.getIntValue());
                }
                return _nodeFactory.numberNode(jp.getLongValue());
            }

        case VALUE_NUMBER_FLOAT:
            {
                JsonParser.NumberType nt = jp.getNumberType();
                if (nt == JsonParser.NumberType.BIG_DECIMAL
                    || ctxt.isEnabled(DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    return _nodeFactory.numberNode(jp.getDecimalValue());
                }
                return _nodeFactory.numberNode(jp.getDoubleValue());
            }

        case VALUE_TRUE:
            return _nodeFactory.booleanNode(true);

        case VALUE_FALSE:
            return _nodeFactory.booleanNode(false);

        case VALUE_NULL:
            return _nodeFactory.nullNode();

            // These states can not be mapped; input stream is
            // off by an event or two

        case END_OBJECT:
        case END_ARRAY:
        default:
            throw ctxt.mappingException(getValueClass());
        }
    }
}
