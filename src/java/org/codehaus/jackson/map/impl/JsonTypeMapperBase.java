package org.codehaus.jackson.map.impl;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.JsonNode;
import org.codehaus.jackson.map.BaseMapper;
import org.codehaus.jackson.map.node.*;

/**
 * This intermediate base class is needed to access non-public
 * (package) interface of node implementations during building.
 */
public abstract class JsonTypeMapperBase
    extends BaseMapper
{
    protected JsonTypeMapperBase() { }

    /*
    /////////////////////////////////////////////////////
    // Actual factory methods exposed and used by the mapper
    // (can also be overridden by sub-classes for extra
    // functionality)
    /////////////////////////////////////////////////////
     */

    public ArrayNode arrayNode() { return new ArrayNode(); }
    public ObjectNode objectNode() { return new ObjectNode(); }
    public NullNode nullNode() { return NullNode.getInstance(); }

    public TextNode textNode(String text) { return TextNode.valueOf(text); }

    public BooleanNode booleanNode(boolean v) {
        return v ? BooleanNode.getTrue() : BooleanNode.getFalse();
    }

    public NumericNode numberNode(int v) { return IntNode.valueOf(v); }
    public NumericNode numberNode(long v) { return LongNode.valueOf(v); }
    public NumericNode numberNode(double v) { return DoubleNode.valueOf(v); }
    public NumericNode numberNode(BigDecimal v) { return DecimalNode.valueOf(v); }

    /*
    /////////////////////////////////////////////////////
    // Mapping functionality
    /////////////////////////////////////////////////////
     */

    protected JsonNode readAndMap(JsonParser jp, JsonToken currToken)
        throws IOException, JsonParseException
    {
        switch (currToken) {
        case START_OBJECT:
            {
                ObjectNode node = objectNode();
                while ((currToken = jp.nextToken()) != JsonToken.END_OBJECT) {
                    if (currToken != JsonToken.FIELD_NAME) {
                        reportProblem(jp, "Unexpected token ("+currToken+"), expected FIELD_NAME");
                    }
                    String fieldName = jp.getText();
                    JsonNode value = readAndMap(jp, jp.nextToken());

                    if (_cfgDupFields == DupFields.ERROR) {
                        JsonNode old = node.setElement(fieldName, value);
                        if (old != null) {
                            reportProblem(jp, "Duplicate value for field '"+fieldName+"', when dup fields mode is "+_cfgDupFields);
                        }
                    } else if (_cfgDupFields == DupFields.USE_LAST) {
                        // Easy, just add
                        node.setElement(fieldName, value);
                    } else { // use first; need to ensure we don't yet have it
                        if (node.getFieldValue(fieldName) == null) {
                            node.setElement(fieldName, value);
                        }
                    }
                }
                return node;
            }

        case START_ARRAY:
            {
                ArrayNode node = arrayNode();
                while ((currToken = jp.nextToken()) != JsonToken.END_ARRAY) {
                    JsonNode value = readAndMap(jp, currToken);
                    node.appendElement(value);
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
            reportProblem(jp, "Can not map token "+currToken+": stream off by a token or two?");

        default: // sanity check, should never happen
            throwInternal("Unrecognized event type: "+currToken);
            return null; // never gets this far
        }
    }
}
