package org.codehaus.jackson.node;

import java.util.*;

import org.codehaus.jackson.*;

/**
 * Helper class used by {@link TreeTraversingParser} to keep track
 * of current location within traversed JSON tree.
 */
abstract class NodeCursor
    extends JsonStreamContext
{
    /**
     * Parent cursor of this cursor, if any; null for root
     * cursors.
     */
    final NodeCursor _parent;

    public NodeCursor(int contextType, NodeCursor p)
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
    public final NodeCursor getParent() { return _parent; }

    public abstract String getCurrentName();

    /*
     *********************************************
     * Extended API
     *********************************************
     */

    public abstract JsonToken nextToken();
    public abstract JsonToken nextValue();
    public abstract JsonToken endToken();

    public abstract JsonNode currentNode();
    public abstract boolean currentHasChildren();

    /**
     * Method called to create a new context for iterating all
     * contents of the current structured value (JSON array or object)
     */
    public final NodeCursor iterateChildren() {
        JsonNode n = currentNode();
        if (n == null) throw new IllegalStateException("No current node");
        if (n.isArray()) { // false since we have already returned START_ARRAY
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
        extends NodeCursor
    {
        JsonNode _node;

        protected boolean _done = false;

        public RootValue(JsonNode n, NodeCursor p) {
            super(JsonStreamContext.TYPE_ROOT, p);
            _node = n;
        }

        public String getCurrentName() { return null; }

        public JsonToken nextToken() {
            if (!_done) {
                _done = true;
                return _node.asToken();
            }
            _node = null;
            return null;
        }
        
        public JsonToken nextValue() { return nextToken(); }
        public JsonToken endToken() { return null; }
        public JsonNode currentNode() { return _node; }
        public boolean currentHasChildren() { return false; }
    }

    /**
     * Cursor used for traversing non-empty JSON Array nodes
     */
    protected final static class Array
        extends NodeCursor
    {
        Iterator<JsonNode> _contents;

        JsonNode _currentNode;

        public Array(JsonNode n, NodeCursor p) {
            super(JsonStreamContext.TYPE_ARRAY, p);
            _contents = n.getElements();
        }

        public String getCurrentName() { return null; }

        public JsonToken nextToken()
        {
            if (!_contents.hasNext()) {
                _currentNode = null;
                return null;
            }
            _currentNode = _contents.next();
            return _currentNode.asToken();
        }

        public JsonToken nextValue() { return nextToken(); }
        public JsonToken endToken() { return JsonToken.END_ARRAY; }

        public JsonNode currentNode() { return _currentNode; }
        public boolean currentHasChildren() {
            // note: ONLY to be called for container nodes
            return ((ContainerNode) currentNode()).size() > 0;
        }
    }

    /**
     * Cursor used for traversing non-empty JSON Object nodes
     */
    protected final static class Object
        extends NodeCursor
    {
        Iterator<Map.Entry<String, JsonNode>> _contents;
        Map.Entry<String, JsonNode> _current;

        boolean _needEntry;
        
        public Object(JsonNode n, NodeCursor p)
        {
            super(JsonStreamContext.TYPE_OBJECT, p);
            _contents = ((ObjectNode) n).getFields();
            _needEntry = true;
        }

        public String getCurrentName() {
            return (_current == null) ? null : _current.getKey();
        }

        public JsonToken nextToken()
        {
            // Need a new entry?
            if (_needEntry) {
                if (!_contents.hasNext()) {
                    _current = null;
                    return null;
                }
                _needEntry = false;
                _current = _contents.next();
                return JsonToken.FIELD_NAME;
            }
            _needEntry = true;
            return _current.getValue().asToken();
        }

        public JsonToken nextValue()
        {
            JsonToken t = nextToken();
            if (t == JsonToken.FIELD_NAME) {
                t = nextToken();
            }
            return t;
        }

        public JsonToken endToken() { return JsonToken.END_OBJECT; }

        public JsonNode currentNode() {
            return (_current == null) ? null : _current.getValue();
        }
        public boolean currentHasChildren() {
            // note: ONLY to be called for container nodes
            return ((ContainerNode) currentNode()).size() > 0;
        }
    }
}
