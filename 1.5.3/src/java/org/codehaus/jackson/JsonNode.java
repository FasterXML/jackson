package org.codehaus.jackson;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;


/**
 * Base class for all JSON nodes, which form the basis of JSON
 * Tree Model that Jackson implements.
 * One way to think of these nodes is to considere them
 * similar to DOM nodes in XML DOM trees.
 *<p>
 * As a general design rule, most accessors ("getters") are included
 * in this base class, to allow for traversing structure without
 * type casts. Most mutators, however, need to be accessed through
 * specific sub-classes. This seems sensible because proper type
 * information is generally available when building or modifying
 * trees, but less often when reading a tree (newly built from
 * parsed Json content).
 *<p>
 * Actual concrete sub-classes can be found from package
 * {@link org.codehaus.jackson.node}.
 */
public abstract class JsonNode
    implements Iterable<JsonNode>
{
    final static List<JsonNode> NO_NODES = Collections.emptyList();
    final static List<String> NO_STRINGS = Collections.emptyList();

    protected JsonNode() { }

    /*
    ////////////////////////////////////////////////////
    // Public API, type introspection
    ////////////////////////////////////////////////////
     */

    // // First high-level division between values, containers and "missing"

    /**
     * Method that returns true for all value nodes: ones that 
     * are not containers, and that do not represent "missing" nodes
     * in the path. Such value nodes represent String, Number, Boolean
     * and null values from JSON.
     *<p>
     * Note: one and only one of methods {@link #isValueNode},
     * {@link #isContainerNode} and {@link #isMissingNode} ever
     * returns true for any given node.
     */
    public boolean isValueNode() { return false; }

    /**
     * Method that returns true for container nodes: Arrays and Objects.
     *<p>
     * Note: one and only one of methods {@link #isValueNode},
     * {@link #isContainerNode} and {@link #isMissingNode} ever
     * returns true for any given node.
     */
    public boolean isContainerNode() { return false; }

    /**
     * Method that returns true for "virtual" nodes which represent
     * missing entries constructed by path accessor methods when
     * there is no actual node matching given criteria.
     *<p>
     * Note: one and only one of methods {@link #isValueNode},
     * {@link #isContainerNode} and {@link #isMissingNode} ever
     * returns true for any given node.
     */
    public boolean isMissingNode() { return false; }

    // // Then more specific type introspection
    // // (along with defaults to be overridden)

    /**
     * @return True if this node represents Json Array
     */
    public boolean isArray() { return false; }

    /**
     * @return True if this node represents Json Object
     */
    public boolean isObject() { return false; }

    /**
     * Method that can be used to check if the node is a wrapper
     * for a POJO ("Plain Old Java Object" aka "bean".
     * Returns true only for
     * instances of {@link org.codehaus.jackson.node.POJONode}.
     *
     * @return True if this node wraps a POJO
     */
    public boolean isPojo() { return false; }

    /**
     * @return True if this node represents a numeric Json
     *   value
     */
    public boolean isNumber() { return false; }

    /**
     * @return True if this node represents an integral (integer)
     *   numeric Json value
     */
    public boolean isIntegralNumber() { return false; }

    /**
     * @return True if this node represents a non-integral
     *   numeric Json value
     */
    public boolean isFloatingPointNumber() { return false; }

    /**
     * @return True if this node represents an integral
     *   numeric Json value that withs in Java int value space
     */
    public boolean isInt() { return false; }

    /**
     * @return True if this node represents an integral
     *   numeric Json value that fits in Java long value space
     *   (but not int value space, i.e. {@link #isInt} returns false)
     */
    public boolean isLong() { return false; }

    public boolean isDouble() { return false; }
    public boolean isBigDecimal() { return false; }
    public boolean isBigInteger() { return false; }

    public boolean isTextual() { return false; }

    /**
     * Method that can be used to check if this node was created from
     * Json boolean value (literals "true" and "false").
     */
    public boolean isBoolean() { return false; }

    /**
     * Method that can be used to check if this node was created from
     * Json liternal null value.
     */
    public boolean isNull() { return false; }

    /**
     * Method that can be used to check if this node represents
     * binary data (Base64 encoded). Although this will be externally
     * written as Json String value, {@link #isTextual} will
     * return false if this method returns true.
     *
     * @return True if this node represents base64 encoded binary data
     */
    public boolean isBinary() { return false; }

    /**
     * Method that can be used for efficient type detection
     * when using stream abstraction for traversing nodes.
     * Will return the first {@link JsonToken} that equivalent
     * stream event would produce (for most nodes there is just
     * one token but for structured/container types multiple)
     *
     * @since 1.3
     */
    public abstract JsonToken asToken();

    /**
     * If this node is a numeric type (as per {@link #isNumber}),
     * returns native type that node uses to store the numeric
     * value.
     */
    public abstract JsonParser.NumberType getNumberType();

    /*
    ////////////////////////////////////////////////////
    // Public API, value access
    ////////////////////////////////////////////////////
     */

    /**
     * Method to use for accessing String values.
     * Does <b>NOT</b> do any conversions for non-String value nodes;
     * for non-String values (ones for which {@link #isTextual} returns
     * false) null will be returned.
     * For String values, null is never returned (but empty Strings may be)
     *
     * @return Textual value this node contains, iff it is a textual
     *   json node (comes from Json String value entry)
     */
    public String getTextValue() { return null; }

    /**
     * Method to use for accessing binary content of binary nodes (nodes
     * for which {@link #isBinary} returns true); or for Text Nodes
     * (ones for which {@link #getTextValue} returns non-null value),
     * to read decoded base64 data.
     * For other types of nodes, returns null.
     *
     * @return Binary data this node contains, iff it is a binary
     *   node; null otherwise
     */
    public byte[] getBinaryValue() throws IOException
    {
        return null;
    }

    public boolean getBooleanValue() { return false; }

    /**
     * Returns numeric value for this node, <b>if and only if</b>
     * this node is numeric ({@link #isNumber} returns true); otherwise
     * returns null
     *
     * @return Number value this node contains, if any (null for non-number
     *   nodes).
     */
    public Number getNumberValue() { return null; }

    public int getIntValue() { return 0; }
    public long getLongValue() { return 0L; }
    public double getDoubleValue() { return 0.0; }
    public BigDecimal getDecimalValue() { return BigDecimal.ZERO; }
    public BigInteger getBigIntegerValue() { return BigInteger.ZERO; }

    /**
     * Method for accessing value of the specified element of
     * an array node. For other nodes, null is always returned.
     *<p>
     * For array nodes, index specifies
     * exact location within array and allows for efficient iteration
     * over child elements (underlying storage is guaranteed to
     * be efficiently indexable, i.e. has random-access to elements).
     * If index is less than 0, or equal-or-greater than
     * <code>node.size()</code>, null is returned; no exception is
     * thrown for any index.
     *
     * @return Node that represent value of the specified element,
     *   if this node is an array and has specified element.
     *   Null otherwise.
     */
    public JsonNode get(int index) { return null; }

    /**
     * Method for accessing value of the specified field of
     * an object node. If this node is not an object (or it
     * does not have a value for specified field name), or
     * if there is no field with such name, null is returned.
     *
     * @return Node that represent value of the specified field,
     *   if this node is an object and has value for the specified
     *   field. Null otherwise.
     */
    public JsonNode get(String fieldName) { return null; }

    /**
     * Alias for {@link #get(String)}.
     *
     * @deprecated Use {@link #get(String)} instead.
     */
    @Deprecated
	public final JsonNode getFieldValue(String fieldName) { return get(fieldName); }

    /**
     * Alias for {@link #get(int)}.
     *
     * @deprecated Use {@link #get(int)} instead.
     */
    @Deprecated
	public final JsonNode getElementValue(int index) { return get(index); }


    /**
     * Method that will return valid String representation of
     * the container value, if the node is a value node
     * (method {@link #isValueNode} returns true), otherwise null.
     *<p>
     * Note: to serialize nodes of any type, you should call
     * {@link #toString} instead.
     */
    public abstract String getValueAsText();

    /*
    ////////////////////////////////////////////////////
    // Public API, container access
    ////////////////////////////////////////////////////
     */

    /**
     * Method that returns number of child nodes this node contains:
     * for Array nodes, number of child elements, for Object nodes,
     * number of fields, and for all other nodes 0.
     *
     * @return For non-container nodes returns 0; for arrays number of
     *   contained elements, and for objects number of fields.
     */
    public int size() { return 0; }

    /**
     * Same as calling {@link #getElements}; implemented so that
     * convenience "for-each" loop can be used for looping over elements
     * of Json Array constructs.
     */
    public final Iterator<JsonNode> iterator() { return getElements(); }

    /**
     * Method for accessing all value nodes of this Node, iff
     * this node is a Json Array or Object node. In case of Object node,
     * field names (keys) are not included, only values.
     * For other types of nodes, returns empty iterator.
     */
    public Iterator<JsonNode> getElements() { return NO_NODES.iterator(); }

    /**
     * Method for accessing names of all fields for this Node, iff
     * this node is a Json Object node.
     */
    public Iterator<String> getFieldNames() { return NO_STRINGS.iterator(); }

    /*
    ////////////////////////////////////////////////////
    // Public API, path handling
    ////////////////////////////////////////////////////
     */

    /**
     * This method is similar to {@link #get(String)}, except
     * that instead of returning null if no such value exists (due
     * to this node not being an object, or object not having value
     * for the specified field),
     * a "missing node" (node that returns true for
     * {@link #isMissingNode}) will be returned. This allows for
     * convenient and safe chained access via path calls.
     */
    public abstract JsonNode path(String fieldName);

    /**
     * Alias of {@link #path(String)}.
     *
     * @deprecated Use {@link #path(String)} instead
     */
    @Deprecated
    public final JsonNode getPath(String fieldName) { return path(fieldName); }

    /**
     * This method is similar to {@link #get(int)}, except
     * that instead of returning null if no such element exists (due
     * to index being out of range, or this node not being an array),
     * a "missing node" (node that returns true for
     * {@link #isMissingNode}) will be returned. This allows for
     * convenient and safe chained access via path calls.
     */
    public abstract JsonNode path(int index);

    /**
     * Alias of {@link #path(int)}.
     *
     * @deprecated Use {@link #path(int)} instead
     */
    @Deprecated
    public final JsonNode getPath(int index) { return path(index); }

    /*
    ////////////////////////////////////////////////////
    // Public API, serialization
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be called to serialize this node and
     * all of its descendants using specified JSON generator.
     *
     * @deprecated Use methods that are part of {@link JsonGenerator}
     *   or {@link org.codehaus.jackson.map.ObjectMapper}
     *   instead.
     */
    public abstract void writeTo(JsonGenerator jg)
        throws IOException, JsonGenerationException;

    /*
    ////////////////////////////////////////////////////
    // Public API: converting to/from Streaming API
    ////////////////////////////////////////////////////
     */


    /**
     * Method for constructing a {@link JsonParser} instance for
     * iterating over contents of the tree that this
     * node is root of.
     * Functionally equivalent to first serializing tree
     * using {@link #writeTo} and then re-parsing but much
     * more efficient.
     */
    public abstract JsonParser traverse();

    /*
    ////////////////////////////////////////////////////
    // Overridden standard methods
    ////////////////////////////////////////////////////
     */
    
    /**
     *<p>
     * Note: marked as abstract to ensure all implementation
     * classes define it properly.
     */
    @Override
    public abstract String toString();

    /**
     *<p>
     * Note: marked as abstract to ensure all implementation
     * classes define it properly.
     */
    @Override
    public abstract boolean equals(Object o);
}
