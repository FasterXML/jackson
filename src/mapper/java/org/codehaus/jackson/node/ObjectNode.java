package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Note that maps to Json Object structures in Json content.
 */
public class ObjectNode
    extends ContainerNode
{
    LinkedHashMap<String, JsonNode> _children = null;

    public ObjectNode(JsonNodeFactory nc) { super(nc); }

    /*
    /**********************************************************
    /* Implementation of core JsonNode API
    /**********************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.START_OBJECT; }

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

    /*
    /**********************************************************
    /* Public API, serialization
    /**********************************************************
     */

    /**
     * Method that can be called to serialize this node and
     * all of its descendants using specified JSON generator.
     */
    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeStartObject();
        if (_children != null) {
            for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
                jg.writeFieldName(en.getKey());
                /* 17-Feb-2009, tatu: Can we trust that all nodes will always
                 *   extend BaseJsonNode? Or if not, at least implement
                 *   JsonSerializable? Let's start with former, change if
                 *   we must.
                 */
                ((BaseJsonNode) en.getValue()).serialize(jg, provider);
            }
        }
        jg.writeEndObject();
    }

    /*
    /**********************************************************
    /* Extended ObjectNode API, accessors
    /**********************************************************
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
    /**********************************************************
    /* Extended ObjectNode API, mutators, generic
    /**********************************************************
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

    public ObjectNode removeAll()
    {
        _children = null;
        return this;
    }

    /**
     * Method for adding given properties to this object node, overriding
     * any existing values for those properties.
     * 
     * @param properties Properties to add
     * 
     * @return This node (to allow chaining)
     * 
     * @since 1.3
     */
    public JsonNode putAll(Map<String,JsonNode> properties)
    {
        if (_children == null) {
            _children = new LinkedHashMap<String, JsonNode>(properties);
        } else {
            for (Map.Entry<String, JsonNode> en : properties.entrySet()) {
                JsonNode n = en.getValue();
                if (n == null) {
                    n = nullNode();
                }
                _children.put(en.getKey(), n);
            }
        }
        return this;
    }

    /**
     * Method for adding all properties of the given Object, overriding
     * any existing values for those properties.
     * 
     * @param other Object of which properties to add to this object
     * 
     * @return This node (to allow chaining)
     * 
     * @since 1.3
     */
    public JsonNode putAll(ObjectNode other)
    {
        int len = other.size();
        if (len > 0) {
            if (_children == null) {
                _children = new LinkedHashMap<String, JsonNode>(len);
            }
            other.putContentsTo(_children);
        }
        return this;
    }
    
    /*
    /**********************************************************
    /* Extended ObjectNode API, mutators, typed
    /**********************************************************
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
     * Method that will construct an ObjectNode and add it as a
     * field of this ObjectNode, replacing old value, if any.
     *
     * @return Newly constructed ObjectNode (NOT the old value,
     *   which could be of any type)
     */
    public ObjectNode putObject(String fieldName)
    {
        ObjectNode n  = objectNode();
        _put(fieldName, n);
        return n;
    }

    public void putPOJO(String fieldName, Object pojo)
    {
        _put(fieldName, POJONode(pojo));
    }

    public void putNull(String fieldName)
    {
        _put(fieldName, nullNode());
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
    public void put(String fieldName, BigDecimal v) {
        if (v == null) {
            putNull(fieldName);
        } else {
            _put(fieldName, numberNode(v));
        }
    }

    /**
     * Method for setting value of a field to specified String value.
     */
    public void put(String fieldName, String v) {
        if (v == null) {
            putNull(fieldName);
        } else {
            _put(fieldName, textNode(v));
        }
    }

    /**
     * Method for setting value of a field to specified String value.
     */
    public void put(String fieldName, boolean v) { _put(fieldName, booleanNode(v)); }

    /**
     * Method for setting value of a field to specified binary value
     */
    public void put(String fieldName, byte[] v) {
        if (v == null) {
            putNull(fieldName);
        } else {
            _put(fieldName, binaryNode(v));
        }
    }

    /*
    /**********************************************************
    /* Package methods (for other node classes to use)
    /**********************************************************
     */

    /**
     * @since 1.6
     */
    protected void putContentsTo(Map<String,JsonNode> dst)
    {
        if (_children != null) {
            for (Map.Entry<String,JsonNode> en : _children.entrySet()) {
                dst.put(en.getKey(), en.getValue());
            }
        }
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
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
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private final JsonNode _put(String fieldName, JsonNode value)
    {
        if (_children == null) {
            _children = new LinkedHashMap<String, JsonNode>();
        }
        return _children.put(fieldName, value);
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
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
