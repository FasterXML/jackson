package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.type.TypeReference;

/**
 * Facade over {@link JsonNode} that implements {@link JsonParser} to allow
 * accessing contents of JSON tree in alternate form (stream of tokens).
 * Useful when a streaming source is expected by code, such as data binding
 * functionality.
 * 
 * @author tatu
 */
public class TreeTraversingParser extends JsonParser
{
    /*
     *********************************************
     * Configuration
     *********************************************
     */

    protected ObjectCodec _objectCodec;

    /**
     * Traversal context within tree
     */
    protected NodeCursor _nodeCursor;

    /*
     *********************************************
     * State
     *********************************************
     */

    /**
     * Sometimes parser needs to buffer a single look-ahead token; if so,
     * it'll be stored here. This is currently used for handling 
     */
    protected JsonToken _nextToken;

    /**
     * Flag needed to handle recursion into contents of child
     * Array/Object nodes.
     */
    protected boolean _startContainer;
    
    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;

    /*
     *********************************************
     * Life-cycle
     *********************************************
     */

    public TreeTraversingParser(JsonNode n) { this(n, null); }

    public TreeTraversingParser(JsonNode n, ObjectCodec codec)
    {
        if (n.isArray()) {
            _nextToken = JsonToken.START_ARRAY;
            _nodeCursor = new NodeCursor.Array(n, null);
        } else if (n.isObject()) {
            _nextToken = JsonToken.START_OBJECT;
            _nodeCursor = new NodeCursor.Object(n, null);
        } else { // value node
            _nodeCursor = new NodeCursor.RootValue(n, null);
        }
    }

    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    /*
     *********************************************
     * Closeable implementation
     *********************************************
     */

    @Override
    public void close() throws IOException
    {
        if (!_closed) {
            _closed = true;
            _nodeCursor = null;
            _currToken = null;
        }
    }

    /*
     *********************************************
     * Public API, traversal
     *********************************************
     */

    @Override
    public JsonToken nextToken() throws IOException, JsonParseException
    {
        if (_nextToken != null) {
            _currToken = _nextToken;
            _nextToken = null;
            return _currToken;
        }
        // are we to descend to a container child?
        if (_startContainer) {
            _startContainer = false;
            // minor optimization: empty containers can be skipped
            if (!_nodeCursor.currentHasChildren()) {
                _currToken = (_currToken == JsonToken.START_OBJECT) ?
                    JsonToken.END_OBJECT : JsonToken.END_ARRAY;
                return _currToken;
            }
            _nodeCursor = _nodeCursor.iterateChildren();
            return (_currToken = _nodeCursor.nextToken());
        }
        // No more content?
        if (_nodeCursor == null) {
            _closed = true; // if not already set
            return null;
        }
        // Otherwise, next entry from current cursor
        _currToken = _nodeCursor.nextToken();
        if (_currToken != null) {
            if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
                _startContainer = true;
            }
            return _currToken;
        }
        // null means no more children; need to return end marker
        _currToken = _nodeCursor.endToken();
        _nodeCursor = _nodeCursor.getParent();
        return _currToken;
    }

    // default works well here:
    //public JsonToken nextValue() throws IOException, JsonParseException

    @Override
    public JsonParser skipChildren() throws IOException, JsonParseException
    {
        if (_currToken == JsonToken.START_OBJECT) {
            _currToken = JsonToken.END_OBJECT;
        } else if (_currToken == JsonToken.START_ARRAY) {
            _currToken = JsonToken.END_ARRAY;
        }
        return this;
    }

    @Override
    public boolean isClosed() {
        return _closed;
    }

    /*
     *********************************************
     * Public API, token accessors
     *********************************************
     */

    @Override
    public String getCurrentName() {
        return (_nodeCursor == null) ? null : _nodeCursor.getCurrentName();
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return _nodeCursor;
    }

    @Override
    public JsonLocation getTokenLocation() {
        return JsonLocation.NA;
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return JsonLocation.NA;
    }

    /*
     *********************************************
     * Public API, access to textual content
     *********************************************
     */

    @Override
    public String getText()
    {
        if (_closed) return null;
        // need to separate handling a bit...
        if (_currToken == JsonToken.FIELD_NAME) {
            return _nodeCursor.getCurrentName();
        }
        JsonNode n = currentNode();
        // only non-null for actual text nodes...
        String text = (n == null) ? null : n.getTextValue();
        if (text != null) {
            return text;
        }
        // otherwise, default to whatever Token produces...
        return _currToken.asString();
    }

    @Override
    public char[] getTextCharacters() throws IOException, JsonParseException {
        return getText().toCharArray();
    }

    @Override
    public int getTextLength() throws IOException, JsonParseException {
        return getText().length();
    }

    @Override
    public int getTextOffset() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     *********************************************
     * Public API, typed non-text access
     *********************************************
     */

    @Override
    public BigInteger getBigIntegerValue() throws IOException,
            JsonParseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte getByteValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getDoubleValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloatValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getIntValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLongValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public NumberType getNumberType() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number getNumberValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public short getShortValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    public Object getEmbeddedObject() {
        if (!_closed) {
            JsonNode n = currentNode();
            if (n != null && n.isPojo()) {
                return ((POJONode) n).getPojo();
            }
        }
        return null;
    }

    /*
     *********************************************
     * Public API, typed binary (base64) access
     *********************************************
     */

    @Override
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException,
            JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     *********************************************
     * Internal methods
     *********************************************
     */

    protected JsonNode currentNode() {
        if (_closed || _nodeCursor == null) {
            return null;
        }
        return _nodeCursor.currentNode();
    }
}
