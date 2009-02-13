package org.codehaus.jackson.map.node;

import java.math.BigDecimal;

/**
 * Interface that specifies methods for getting access to
 * Node instances -- either newly constructed, or shared, depending
 * on type.
 */
public interface JsonNodeFactory
{
    public ArrayNode arrayNode();
    public ObjectNode objectNode();
    public NullNode nullNode();

    public BooleanNode booleanNode(boolean v);

    public NumericNode numberNode(byte v);
    public NumericNode numberNode(short v);
    public NumericNode numberNode(int v);
    public NumericNode numberNode(long v);
    public NumericNode numberNode(float v);
    public NumericNode numberNode(double v);
    public NumericNode numberNode(BigDecimal v);

    public TextNode textNode(String text);

    public BinaryNode binaryNode(byte[] data);
    public BinaryNode binaryNode(byte[] data, int offset, int length);
}

