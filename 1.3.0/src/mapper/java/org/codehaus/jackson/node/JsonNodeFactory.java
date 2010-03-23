package org.codehaus.jackson.node;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Base class that specifies methods for getting access to
 * Node instances (newly constructed, or shared, depending
 * on type), as well as basic implementation of the methods. 
 * Designed to be sub-classed if extended functionality (additions
 * to behavior of node types, mostly) is needed.
 */
public class JsonNodeFactory
{
    /**
     * Default singleton instance that construct "standard" node instances:
     * given that this class is stateless, a globally shared singleton
     * can be used.
     */
    public final static JsonNodeFactory instance = new JsonNodeFactory();

    protected JsonNodeFactory() { }

    /*
    ////////////////////////////////////////////////////////
    // Factory methods for literal values
    ////////////////////////////////////////////////////////
     */

    /**
     * Factory method for getting an instance of Json boolean value
     * (either literal 'true' or 'false')
     */
    public BooleanNode booleanNode(boolean v) {
        return v ? BooleanNode.getTrue() : BooleanNode.getFalse();
    }

    /**
     * Factory method for getting an instance of Json null node (which
     * represents literal null value)
     */
    public NullNode nullNode() { return NullNode.getInstance(); }

    /*
    ////////////////////////////////////////////////////////
    // Factory methods for numeric values
    ////////////////////////////////////////////////////////
     */

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 8-bit value
     */
    public NumericNode numberNode(byte v) { return IntNode.valueOf(v); }

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 16-bit integer value
     */
    public NumericNode numberNode(short v) { return IntNode.valueOf(v); }

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 32-bit integer value
     */
    public NumericNode numberNode(int v) { return IntNode.valueOf(v); }

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 64-bit integer value
     */
    public NumericNode numberNode(long v) { return LongNode.valueOf(v); }

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given unlimited range integer value
     */
    public NumericNode numberNode(BigInteger v) { return BigIntegerNode.valueOf(v); }

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 32-bit floating point value
     */
    public NumericNode numberNode(float v) { return DoubleNode.valueOf((double) v); }

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given 64-bit floating point value
     */
    public NumericNode numberNode(double v) { return DoubleNode.valueOf(v); }

    /**
     * Factory method for getting an instance of Json numeric value
     * that expresses given unlimited precision floating point value
     */
    public NumericNode numberNode(BigDecimal v) { return DecimalNode.valueOf(v); }

    /*
    ////////////////////////////////////////////////////////
    // Factory methods for textual values
    ////////////////////////////////////////////////////////
     */

    /**
     * Factory method for constructing a node that represents Json
     * String value
     */
    public TextNode textNode(String text) { return TextNode.valueOf(text); }

    /**
     * Factory method for constructing a node that represents given
     * binary data, and will get serialized as equivalent base64-encoded
     * String value
     */
    public BinaryNode binaryNode(byte[] data) { return BinaryNode.valueOf(data); }

    /**
     * Factory method for constructing a node that represents given
     * binary data, and will get serialized as equivalent base64-encoded
     * String value
     */
    public BinaryNode binaryNode(byte[] data, int offset, int length) {
        return BinaryNode.valueOf(data, offset, length);
    }

    /*
    ////////////////////////////////////////////////////////
    // Factory method for structured values
    ////////////////////////////////////////////////////////
     */

    /**
     * Factory method for constructing an empty Json Array node
     */
    public ArrayNode arrayNode() { return new ArrayNode(this); }

    /**
     * Factory method for constructing an empty Json Object ("struct") node
     */
    public ObjectNode objectNode() { return new ObjectNode(this); }

    /**
     * Factory method for constructing a wrapper for POJO
     * ("Plain Old Java Object") objects; these will get serialized
     * using data binding, usually as Json Objects, but in some
     * cases as Json Strings or other node types.
     */
    public POJONode POJONode(Object pojo) { return new POJONode(pojo); }
}

