package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

/**
 * Note that maps to Json Object structures in Json content.
 */
public final class ObjectNode
    extends ContainerNode
{
    LinkedHashMap<String, JsonNode> _children = null;

    public ObjectNode(JsonNodeFactory nc) { super(nc); }

    /*
    ///////////////////////////////////////////////////////////
    // Implementation of core JsonNode API
    ///////////////////////////////////////////////////////////
     */

    @Override
    public boolean isObject() { return true; }

    @Override
    public int size() {
        return (_children == null) ? 0 : _children.size();
    }

    @Override
    public Iterator<JsonNode> getElements()
    {
        return (_children == null) ? NoNodesIterator.instance() : _children.values().iterator();
    }

    @Override
        public JsonNode get(int index) { return null; }

    @Override
        public JsonNode get(String fieldName)
    {
        if (_children != null) {
            return _children.get(fieldName);
        }
        return null;
    }

    @Override
    public Iterator<String> getFieldNames()
    {
        return (_children == null) ? NoStringsIterator.instance() : _children.keySet().iterator();
    }

    @Override
    public JsonNode path(int index)
    {
        return MissingNode.getInstance();
    }

    @Override
        public JsonNode path(String fieldName)
    {
        if (_children != null) {
            JsonNode n = _children.get(fieldName);
            if (n != null) {
                return n;
            }
        }
        return MissingNode.getInstance();
    }

    @Override
	public void writeTo(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeStartObject();
        if (_children != null) {
            for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
                jg.writeFieldName(en.getKey());
                en.getValue().writeTo(jg);
            }
        }
        jg.writeEndObject();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Extended ObjectNode API, accessors
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method to use for accessing all fields (with both names
     * and values) of this Json Object.
     */
    public Iterator<Map.Entry<String, JsonNode>> getFields()
    {
        if (_children == null) {
            return NoFieldsIterator.instance;
        }
        return _children.entrySet().iterator();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Extended ObjectNode API, mutators, generic
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method that will set specified field, replacing old value,
     * if any.
     *
     * @param value to set field to; if null, will be converted
     *   to a {@link NullNode} first  (to remove field entry, call
     *   {@link #remove} instead)
     *
     * @return Old value of the field, if any; null if there was no
     *   old value.
     */
    public JsonNode put(String fieldName, JsonNode value)
    {
        if (value == null) { // let's not store 'raw' nulls but nodes
            value = nullNode();
        }
        return _put(fieldName, value);
    }

    /**
     * Method for removing field entry from this ObjectNode.
     * Will return value of the field, if such field existed;
     * null if not.
     */
    public JsonNode remove(String fieldName)
    {
        if (_children != null) {
            return _children.remove(fieldName);
        }
        return null;
    }

    /*
    ///////////////////////////////////////////////////////////
    // Extended ObjectNode API, mutators, typed
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method that will construct an ArrayNode and add it as a
     * field of this ObjectNode, replacing old value, if any.
     *
     * @return Newly constructed ArrayNode (NOT the old value,
     *   which could be of any type)
     */
    public ArrayNode putArray(String fieldName)
    {
        ArrayNode n  = arrayNode();
        _put(fieldName, n);
        return n;
    }

    /**
     * Method that will construct an ArrayNode and add it as a
     * field of this ObjectNode, replacing old value, if any.
     *
     * @return Newly constructed ArrayNode (NOT the old value,
     *   which could be of any type)
     */
    public ObjectNode putObject(String fieldName)
    {
        ObjectNode n  = objectNode();
        _put(fieldName, n);
        return n;
    }

    /**
     * Method for setting value of a field to specified numeric value.
     */
    public void put(String fieldName, int v) { _put(fieldName, numberNode(v)); }

    /**
     * Method for setting value of a field to specified numeric value.
     */
    public void put(String fieldName, long v) { _put(fieldName, numberNode(v)); }

    /**
     * Method for setting value of a field to specified numeric value.
     */
    public void put(String fieldName, float v) { _put(fieldName, numberNode(v)); }

    /**
     * Method for setting value of a field to specified numeric value.
     */
    public void put(String fieldName, double v) { _put(fieldName, numberNode(v)); }

    /**
     * Method for setting value of a field to specified numeric value.
     */
    public void put(String fieldName, BigDecimal v) { _put(fieldName, numberNode(v)); }

    /**
     * Method for setting value of a field to specified String value.
     */
    public void put(String fieldName, String v) { _put(fieldName, textNode(v)); }

    /**
     * Method for setting value of a field to specified String value.
     */
    public void put(String fieldName, boolean v) { _put(fieldName, booleanNode(v)); }

    /**
     * Method for setting value of a field to specified binary value
     */
    public void put(String fieldName, byte[] v) { _put(fieldName, binaryNode(v)); }

    /*
    ////////////////////////////////////////////////////////
    // Standard methods
    ////////////////////////////////////////////////////////
     */

    @Override
	public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) {
            return false;
        }
        ObjectNode other = (ObjectNode) o;
        if (other.size() != size()) {
            return false;
        }
        if (_children != null) {
            for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
                String key = en.getKey();
                JsonNode value = en.getValue();

                JsonNode otherValue = other.get(key);

                if (otherValue == null || !otherValue.equals(value)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return (_children == null) ? -1 : _children.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(32 + (size() << 4));
        sb.append("{");
        if (_children != null) {
            int count = 0;
            for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
                if (count > 0) {
                    sb.append(",");
                }
                ++count;
                TextNode.appendQuoted(sb, en.getKey());
                sb.append(':');
                sb.append(en.getValue().toString());
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /*
    ////////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////////
     */

    private final JsonNode _put(String fieldName, JsonNode value)
    {
        if (_children == null) {
            _children = new LinkedHashMap<String, JsonNode>();
        }
        return _children.put(fieldName, value);
    }

    /*
    ////////////////////////////////////////////////////////
    // Helper classes
    ////////////////////////////////////////////////////////
     */

    /**
     * For efficiency, let's share the "no fields" iterator...
     */
    protected static class NoFieldsIterator
        implements Iterator<Map.Entry<String, JsonNode>>
    {
        final static NoFieldsIterator instance = new NoFieldsIterator();

        private NoFieldsIterator() { }

        public boolean hasNext() { return false; }
        public Map.Entry<String,JsonNode> next() { throw new NoSuchElementException(); }

        public void remove() { // or IllegalOperationException?
            throw new IllegalStateException();
        }
    }
}
