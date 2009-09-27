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

    /*
     *********************************************
     * State
     *********************************************
     */

    /*
     *********************************************
     * Life-cycle
     *********************************************
     */

    public NodeTraversingParser(JsonNode n) {
        //!!! TBI
    }

    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

    /*
     *********************************************
     * Closeable implementation
     *********************************************
     */

    @Override
    public void close() throws IOException {
        // Nothing to do here...
    }

    /*
     *********************************************
     * Public API, traversal
     *********************************************
     */

    @Override
    public JsonToken nextToken() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonToken nextValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonParser skipChildren() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isClosed() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     *********************************************
     * Public API, token accessors
     *********************************************
     */

    @Override
    public String getCurrentName() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonStreamContext getParsingContext() {
        // TODO Auto-generated method stub
        return null;
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
    public String getText() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public char[] getTextCharacters() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getTextLength() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
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
            JsonParseException {
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
            JsonProcessingException {
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
}
