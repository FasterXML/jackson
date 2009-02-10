package org.codehaus.jackson.map;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

/**
 * Base class for all JSON nodes, used with the "dynamic" (JSON type)
 * mapper. One way to think of these nodes is to think them as being
 * similar to DOM nodes in XML DOM trees.
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

    /*
    ////////////////////////////////////////////////////
    // Public API, value access
    ////////////////////////////////////////////////////
     */

    /**
     * Method to use for accessing String values (list values, Object
     * field values, root-level values).
     * Does <b>NOT</b> do any conversions for non-String value nodes;
     * for non-String values (ones for which {@link #isTextual} returns
     * false) null will be returned.
     * For String values, null is never returned; empty Strings are returned
     * as is.
     *
     * @return Textual value this node contains, iff it is a textual
     *   json node (comes from Json String value entry)
     */
    public String getTextValue() { return null; }

    /**
     * Method to use for accessing binary content of binary nodes (nodes
     * for which {@link #isBinary} returns true).
     * For non-binary nodes, returns null.
     *
     * @return Binary data this node contains, iff it is a binary
     *   node; null otherwise
     */
    public byte[] getBinaryValue() { return null; }

    public boolean getBooleanValue() { return false; }
    public Number getNumberValue() { return Integer.valueOf(getIntValue()); }
    public int getIntValue() { return 0; }
    public long getLongValue() { return 0L; }
    public double getDoubleValue() { return 0.0; }
    public BigDecimal getDecimalValue() { return BigDecimal.ZERO; }

    /**
     * Method for accessing value of the specified element of
     * an array node. If this node is not an array (or index is
     * out of range), null will be returned.
     *
     * @return Node that represent value of the specified element,
     *   if this node is an array and has specified element.
     *   Null otherwise.
     */
    public JsonNode getElementValue(int index) { return null; }

    /**
     * Method for accessing value of the specified field of
     * an object node. If this node is not an object (or it
     * does not have a value for specified field name), null
     * is returned.
     *
     * @return Node that represent value of the specified field,
     *   if this node is an object and has value for the specified
     *   field. Null otherwise.
     */
    public JsonNode getFieldValue(String fieldName) { return null; }

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
     * @return For non-container nodes returns 0; for arrays number of
     *   contained elements, and for objects number of fields.
     */
    public int size() { return 0; }

    /**
     * Method for accessing all value nodes of this Node, iff
     * this node is a Json Object node.
     * <b>NOTE:</b> does NOT allow iterating over field/value pairs of
     * Json Object constructs (instead, need to call
     * {@link #getFieldValues} to do that).
     */
    public Iterator<JsonNode> getElements() { return NO_NODES.iterator(); }

    /**
     * Same as calling {@link #getElements}; implemented so that
     * convenience "for-each" loop can be used for looping over elements
     * of Json Array constructs.
     */
    public final Iterator<JsonNode> iterator() { return getElements(); }

    /**
     * Method for accessing names of all fields for this Node, iff
     * this node is a Json Object node.
     */
    public Iterator<String> getFieldNames() { return NO_STRINGS.iterator(); }
    /**
     * Method for accessing field value nodes , iff
     * this node is a Json Object node.
     */
    public Iterator<JsonNode> getFieldValues() { return NO_NODES.iterator(); }

    /*
    ////////////////////////////////////////////////////
    // Public API, container mutators
    ////////////////////////////////////////////////////
     */

    /**
     * Method for appending a value Node as the list child of
     * this node. Only works for Array
     * nodes, i.e. nodes for which {@link #isArray} returns true;
     * for Arrays given node gets added as the last child element.
     */
    public void appendElement(JsonNode node) {
        throw _constructNoArrayMods();
    }

    // !!! TODO: add convenience methods (appendElement(int x) etc)

    /**
     * Method for inserting specified node, at specified index, within
     * this Array node.
     * Only works for Array nodes, i.e. nodes for which {@link #isArray} returns true.
     */
    public void insertElement(int index, JsonNode value) {
        throw _constructNoArrayMods();
    }

    /**
     * Method for removing specified value of this Array node.
     * Only works for Array nodes, i.e. nodes for which {@link #isArray} returns true.
     */
    public JsonNode removeElement(int index) {
        throw _constructNoArrayMods();
    }

    /**
     * Method for removing specified value of this Object node.
     * Only works for Object nodes, i.e. nodes for which {@link #isObject} returns true.
     */
    public JsonNode removeElement(String fieldName) {
        throw _constructNoObjectMods();
    }

    // TODO: add convenience methods (insertElement(int x) etc)

    /**
     * Method for setting specified value of this Array node.
     * Only works for Array nodes, i.e. nodes for which {@link #isArray} returns true.
     */
    public JsonNode setElement(int index, JsonNode value) {
        throw _constructNoArrayMods();
    }

    /**
     * Method for setting value of specified field of this Object node.
     * Only works for Object nodes, i.e. nodes for which {@link #isObject}
     * returns true.
     */
    public JsonNode setElement(String fieldName, JsonNode value) {
        throw _constructNoObjectMods();
    }

    // TODO: add convenience methods (setElement(String, int) etc)

    /*
    ////////////////////////////////////////////////////
    // Public API, path handling
    ////////////////////////////////////////////////////
     */

    /**
     * This method is similar to {@link #getFieldValue}, except
     * that instead of returning null if no such value exists (due
     * to this node not being an object, or object not having value
     * for the specified field),
     * a "missing node" (node that returns true for
     * {@link #isMissingNode}) will be returned. This allows for
     * convenient and safe chained access via path calls.
     */
    public abstract JsonNode getPath(String fieldName);

    /**
     * This method is similar to {@link #getElementValue}, except
     * that instead of returning null if no such element exists (due
     * to index being out of range, or this node not being an array),
     * a "missing node" (node that returns true for
     * {@link #isMissingNode}) will be returned. This allows for
     * convenient and safe chained access via path calls.
     */
    public abstract JsonNode getPath(int index);

    /*
    ////////////////////////////////////////////////////
    // Public API, serialization
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be called to serialize this node and
     * all of its descendants using specified JSON generator.
     */
    public abstract void writeTo(JsonGenerator jg)
        throws IOException, JsonGenerationException;

    /*
    ////////////////////////////////////////////////////
    // Overridden standard methods
    ////////////////////////////////////////////////////
     */
    
    /**
     * Let's mark this standard method as abstract to ensure all
     * implementation classes define it
     */
    @Override
    public abstract String toString();

    /**
     * Let's mark this standard method as abstract to ensure all
     * implementation classes define it
     */
    @Override
    public abstract boolean equals(Object o);

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    protected UnsupportedOperationException _constructNoArrayMods()
    {
        return new UnsupportedOperationException("Node of type "+getClass()+" does not support appendElement, insertElement or setElement(int, ...) operations (only ArrayNodes do)");
    }

    protected UnsupportedOperationException _constructNoObjectMods()
    {
        return new UnsupportedOperationException("Node of type "+getClass()+" does not support setElement(String, ...) operations (only ObjectNodes do)");
    }
}
