package org.codehaus.jackson.node;

import java.math.BigDecimal;

/**
 * Interface that specifies methods for getting access to
 * Node instances -- either newly constructed, or shared, depending
 * on type.
 */
public interface JsonNodeFactory
{
    /*
    ////////////////////////////////////////////////////////
    // Factory methods for literal values
    ////////////////////////////////////////////////////////
     */

    /**
     * Factory method for getting an instance of Json boolean value
     * (either literal 'true' or 'false')
     */
    public BooleanNode booleanNode(boolean v);

    /**
     * Factory method for getting an instance of Json null node (which
     * represents literal null value)
     */
    public NullNode nullNode();

    /*
    ////////////////////////////////////////////////////////
    // Factory methods for numeric values
    ////////////////////////////////////////////////////////
     */

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 8-bit value
     */
    public NumericNode numberNode(byte v);

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 16-bit integer value
     */
    public NumericNode numberNode(short v);

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 32-bit integer value
     */
    public NumericNode numberNode(int v);

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 64-bit integer value
     */
    public NumericNode numberNode(long v);

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 32-bit floating point value
     */
    public NumericNode numberNode(float v);

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 64-bit floating point value
     */
    public NumericNode numberNode(double v);

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given unlimited precision floating point value
     */
    public NumericNode numberNode(BigDecimal v);

    /*
    ////////////////////////////////////////////////////////
    // Factory methods for textual values
    ////////////////////////////////////////////////////////
     */

    /**
     * Factory method for constructing a node that represents Json
     * String value
     */
    public TextNode textNode(String text);

    /**
     * Factory method for constructing a node that represents given
     * binary data, and will get serialized as equivalent base64-encoded
     * String value
     */
    public BinaryNode binaryNode(byte[] data);

    /**
     * Factory method for constructing a node that represents given
     * binary data, and will get serialized as equivalent base64-encoded
     * String value
     */
    public BinaryNode binaryNode(byte[] data, int offset, int length);

    /*
    ////////////////////////////////////////////////////////
    // Factory method for structured values
    ////////////////////////////////////////////////////////
     */

    /**
     * Factory method for constructing an empty Json Array node
     */
    public ArrayNode arrayNode();

    /**
     * Factory method for constructing an empty Json Object ("struct") node
     */
    public ObjectNode objectNode();

    /**
     * Factory method for constructing a wrapper for POJO
     * ("Plain Old Java Object") objects; these will get serialized
     * using data binding, usually as Json Objects, but in some
     * cases as Json Strings or other node types.
     */
    public POJONode POJONode(Object pojo);
}

