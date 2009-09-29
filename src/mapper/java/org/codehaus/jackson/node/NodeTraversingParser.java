package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.type.TypeReference;

public class NodeTraversingParser extends JsonParser
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
    protected NodeContext _nodeContext;

    /*
     *********************************************
     * State
     *********************************************
     */

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

    public NodeTraversingParser(JsonNode n) { this(n, null); }

    public NodeTraversingParser(JsonNode n, ObjectCodec codec)
    {
        if (n.isArray()) {
            _nodeContext = new NodeContext.Array(n, null);
        } else if (n.isObject()) {
            _nodeContext = new NodeContext.Object(n, null);
        } else { // value node
            _nodeContext = new NodeContext.RootValue(n, null);
        }
        _nodeContext = null;
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
        if (_closed) return null; // closed: no more tokens
        // are we to descend to a child?
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            _nodeContext = _nodeContext.iterateChildren();
        }
        do {
            _currToken = _nodeContext.nextToken();
            if (_currToken != null) return _currToken;
            _nodeContext = _nodeContext.getParent();
        } while (_nodeContext != null);

        // Ok, we hit the end
        close();
        return null;
    }

    @Override
    public JsonToken nextValue() throws IOException, JsonParseException
    {
        if (_closed) return null; // closed: no more tokens
        // are we to descend to a child?
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            _nodeContext = _nodeContext.iterateChildren();
        }
        do {
            _currToken = _nodeContext.nextValue();
            if (_currToken != null) return _currToken;
            _nodeContext = _nodeContext.getParent();
        } while (_nodeContext != null);
        close();
        return null;
    }

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
        return (_nodeContext == null) ? null : _nodeContext.getCurrentName();
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return _nodeContext;
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
            return _nodeContext.getCurrentName();
        }
        JsonNode n = currentNode();
        // only non-null for actual text nodes...
        String text = n.getTextValue();
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
            if (n.isPojo()) {
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
     * Public API, data binding
     *********************************************
     */

    @Override
    public <T> T readValueAs(Class<T> valueType) throws IOException,
            JsonProcessingException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readValueAs(TypeReference<?> valueTypeRef) throws IOException,
            JsonProcessingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonNode readValueAsTree() throws IOException, JsonProcessingException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     *********************************************
     * Internal methods
     *********************************************
     */

    protected JsonNode currentNode() {
        if (_closed) return null;
        return _nodeContext.currentNode();
    }
}
