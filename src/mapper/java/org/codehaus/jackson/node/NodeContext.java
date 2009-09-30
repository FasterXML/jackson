package org.codehaus.jackson.node;

import java.util.*;

import org.codehaus.jackson.*;

/**
 * Helper class used by {@link TreeTraversingParser} to keep track
 * of hierarchic location within traversed JSON tree.
 */
abstract class NodeContext
    extends JsonStreamContext
{
    final NodeContext _parent;

    public NodeContext(int contextType, NodeContext p)
    {
        super(contextType);
        _parent = p;
    }

    /*
     *********************************************
     * JsonStreamContext impl
     *********************************************
     */

    // note: co-variant return type
    public final NodeContext getParent() { return _parent; }

    public abstract String getCurrentName();

    /*
     *********************************************
     * Extended API
     *********************************************
     */

    public abstract JsonToken nextToken();

    public abstract JsonToken nextValue();

    public abstract JsonNode currentNode();

    /**
     * Method called to create a new context for iterating all
     * contents of the current structured value (JSON array or object)
     */
    public NodeContext iterateChildren() {
        JsonNode n = currentNode();
        if (n == null) throw new IllegalStateException("No current node");
        if (n.isArray()) {
                return new Array(n, this);
        }
        if (n.isObject()) {
            return new Object(n, this);
        }
        throw new IllegalStateException("Current node of type "+n.getClass().getName());
    }

    /*
     *********************************************
     * Concrete implementations
     *********************************************
     */

    /**
     * Context matching root-level value nodes (i.e. anything other
     * than JSON Object and Array).
     * Note that context is NOT created for leaf values.
     */
    protected final static class RootValue
        extends NodeContext
    {
        JsonNode _node;

        protected boolean _done = false;

        public RootValue(JsonNode n, NodeContext p) {
            super(JsonStreamContext.TYPE_ROOT, p);
            _node = n;
        }

        public String getCurrentName() { return null; }

        public JsonToken nextToken() {
            if (_node != null) {
                JsonToken t = _node.asToken();
                _node = null;
                return t;
            }
            return null;
        }
        
        public JsonToken nextValue() { return nextToken(); }

        public JsonNode currentNode() { return _node; }
    }

    protected final static class Array
        extends NodeContext
    {
        Iterator<JsonNode> _contents;

        JsonNode _currentNode;

        public Array(JsonNode n, NodeContext p) {
            super(JsonStreamContext.TYPE_ARRAY, p);
            _contents = n.getElements();
        }

        public String getCurrentName() { return null; }

        public JsonToken nextToken() {
            if (!_contents.hasNext()) return null;
            _currentNode = _contents.next();
            return _currentNode.asToken();
        }

        public JsonToken nextValue() { return nextToken(); }

        public JsonNode currentNode() { return _currentNode; }
    }

    protected final static class Object
        extends NodeContext
    {
        Iterator<Map.Entry<String, JsonNode>> _contents;

        Map.Entry<String,JsonNode> _current;

        boolean _needEntry;

        public Object(JsonNode n, NodeContext p) {
            super(JsonStreamContext.TYPE_OBJECT, p);
            _contents = ((ObjectNode) n).getFields();
            _needEntry = true;
        }

        public String getCurrentName() {
            return (_current == null) ? null : _current.getKey();
        }

        public JsonToken nextToken() {
            if (_needEntry) {
                if (!_contents.hasNext()) return null;
                _needEntry = false;
                _current = _contents.next();
                return JsonToken.FIELD_NAME;
            }
            _needEntry = true;
            return _current.getValue().asToken();
        }

        public JsonToken nextValue() {
            if (_needEntry) {
                if (!_contents.hasNext()) return null;
                _current = _contents.next();
            } else {
                _needEntry = true;
            }
            return _current.getValue().asToken();
        }

        public JsonNode currentNode() {
            return _current.getValue();
        }
    }
}
