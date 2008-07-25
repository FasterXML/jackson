package org.codehaus.jackson.map;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

/**
 * Base class for all JSON nodes, used with the "dynamic" (JSON type)
 * mapper
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

    public boolean isArray() { return false; }
    public boolean isObject() { return false; }
    public boolean isNumber() { return false; }
    public boolean isIntegralNumber() { return false; }
    public boolean isFloatingPointNumber() { return false; }

    public boolean isInt() { return false; }
    public boolean isLong() { return false; }
    public boolean isDouble() { return false; }
    public boolean isBigDecimal() { return false; }

    public boolean isTextual() { return false; }
    public boolean isBoolean() { return false; }
    public boolean isNull() { return false; }

    /*
    ////////////////////////////////////////////////////
    // Public API, value access
    ////////////////////////////////////////////////////
     */

    public String getTextValue() { return null; }
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

    public Iterator<JsonNode> getElements() { return NO_NODES.iterator(); }

    public final Iterator<JsonNode> iterator() { return getElements(); }

    public Iterator<String> getFieldNames() { return NO_STRINGS.iterator(); }
    public Iterator<JsonNode> getFieldValues() { return NO_NODES.iterator(); }

    /*
    ////////////////////////////////////////////////////
    // Public API, container mutators
    ////////////////////////////////////////////////////
     */

    public void appendElement(JsonNode node) {
        reportNoArrayMods();
    }

    // TODO: add convenience methods (appendElement(int x) etc)

    public void insertElement(int index, JsonNode value) {
        reportNoArrayMods();
    }

    public JsonNode removeElement(int index) {
        reportNoArrayMods();
        return null;
    }

    public JsonNode removeElement(String fieldName) {
        reportNoObjectMods();
        return null;
    }

    // TODO: add convenience methods (insertElement(int x) etc)

    public JsonNode setElement(int index, JsonNode value) {
        reportNoArrayMods();
        return null;
    }

    public JsonNode setElement(String fieldName, JsonNode value) {
        reportNoObjectMods();
        return null;
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

    protected JsonNode reportNoArrayMods()
    {
        throw new UnsupportedOperationException("Node of type "+getClass()+" does not support appendElement, insertElement or setElement(int, ...) operations (only ArrayNodes do)");
    }

    protected JsonNode reportNoObjectMods()
    {
        throw new UnsupportedOperationException("Node of type "+getClass()+" does not support setElement(String, ...) operations (only ObjectNodes do)");
    }
}
