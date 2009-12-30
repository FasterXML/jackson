package org.codehaus.jackson.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.JsonReadContext;

/**
 * Utility class used for efficient storage of {@link JsonToken}
 * sequences, needed for temporary buffering.
 * Space efficient for different sequence lengths (especially so for smaller
 * ones; but not significantly less efficient for larger), highly efficient
 * for linear iteration and appending. Implemented as segmented/chunked
 * linked list of tokens; only modifications are via appends.
 * 
 * @since 1.5
 */
public class TokenBuffer
//    implements JsonGenerator
{
    protected Segment _first, _last;
    
    public TokenBuffer()
    {
        // at first we have just one segment
        _first = _last = new Segment();
    }

    public void appendValue(JsonParser jp) {
        
    }
    
    /**
     * Method used to create a {@link JsonParser} that can read contents
     * stored in this buffer.
     *<p>
     * Note: instances are not synchronized, that is, they are not thread-safe
     * if there are concurrent appends to the underlying buffer.
     * 
     * @return Parser that can be used for reading contents stored in this buffer
     */
    public JsonParser asParser() {
        // !!! TBI
        return null;
    }

    /*
    *****************************************************************
    * Supporting classes
    *****************************************************************
     */

    protected final static class Parser
        extends JsonParser
    {
        protected ObjectCodec _codec;

        /*
        ////////////////////////////////////////////////////
        // Parsing state
        ////////////////////////////////////////////////////
         */

        /**
         * Currently active segment
         */
        protected Segment _segment;

        /**
         * Pointer to current token within current segment
         */
        protected int _segmentPtr;

        /**
         * Information about parser context, context in which
         * the next token is to be parsed (root, array, object).
         */
        protected JsonReadContext _parsingContext;
        
        protected boolean _closed;
        
        /*
        ////////////////////////////////////////////////////
        // Construction, init
        ////////////////////////////////////////////////////
         */
        
        public Parser(Segment firstSeg, ObjectCodec codec) {
            _segment = firstSeg;
            _segmentPtr = 0;
            _codec = codec;
            _parsingContext = JsonReadContext.createRootContext(-1, -1);
        }

        @Override
        public ObjectCodec getCodec() { return _codec; }

        @Override
        public void setCodec(ObjectCodec c) {
            // TODO Auto-generated method stub
            
        }

        /*
        ////////////////////////////////////////////////////
        // Closeable implementation
        ////////////////////////////////////////////////////
         */

        @Override
        public void close() throws IOException {
            if (!_closed) {
                _closed = true;
            }
        }

        /*
        ////////////////////////////////////////////////////
        // Public API, traversal
        ////////////////////////////////////////////////////
         */
        
        @Override
        public JsonToken nextToken() throws IOException, JsonParseException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public JsonParser skipChildren() throws IOException, JsonParseException
        {
            if (_currToken != JsonToken.START_OBJECT && _currToken != JsonToken.START_ARRAY) {
                return this;
            }
            int open = 1;

            /* Since proper matching of start/end markers is handled
             * by nextToken(), we'll just count nesting levels here
             */
            while (true) {
                JsonToken t = nextToken();
                if (t == null) {
                    // error for most parsers, but ok here
                    return this;
                }
                switch (t) {
                case START_OBJECT:
                case START_ARRAY:
                    ++open;
                    break;
                case END_OBJECT:
                case END_ARRAY:
                    if (--open == 0) {
                        return this;
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isClosed() { return _closed; }

        /*
        ////////////////////////////////////////////////////
        // Public API, token accessors
        ////////////////////////////////////////////////////
         */
        
        @Override
        public JsonStreamContext getParsingContext() { return _parsingContext; }

        @Override
        public JsonLocation getTokenLocation() { return JsonLocation.NA; }

        @Override
        public JsonLocation getCurrentLocation() { return JsonLocation.NA; }

        @Override
        public String getCurrentName() { return _parsingContext.getCurrentName(); }
        
        /*
        ////////////////////////////////////////////////////
        // Public API, access to token information, text
        ////////////////////////////////////////////////////
         */
        
        @Override
        public String getText()
        {
            if (_currToken != null) { // null only before/after document
                switch (_currToken) {
                case FIELD_NAME:
                case VALUE_STRING:
                    return (String) _segment.get(_segmentPtr);
                    // fall through
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                    Object ob = _segment.get(_segmentPtr);
                    return (ob == null) ? null : ob.toString();
                default:
                    return _currToken.asString();
                }
            }
            return null;
        }

        @Override
        public char[] getTextCharacters() {
            String str = getText();
            return (str == null) ? null : str.toCharArray();
        }

        @Override
        public int getTextLength() {
            String str = getText();
            return (str == null) ? 0 : str.length();
        }

        @Override
        public int getTextOffset() { return 0; }
        /*
        ////////////////////////////////////////////////////
        // Public API, access to token information, numeric
        ////////////////////////////////////////////////////
         */

        @Override
        public BigInteger getBigIntegerValue() throws IOException, JsonParseException
        {
            Number n = getNumberValue();
            if (n instanceof BigInteger) {
                return (BigInteger) n;
            }
            switch (getNumberType()) {
            case BIG_DECIMAL:
                return ((BigDecimal) n).toBigInteger();
            }
            // int/long is simple, but let's also just truncate float/double:
            return BigInteger.valueOf(n.longValue());
        }

        @Override
        public BigDecimal getDecimalValue() throws IOException, JsonParseException
        {
            Number n = getNumberValue();
            if (n instanceof BigDecimal) {
                return (BigDecimal) n;
            }
            switch (getNumberType()) {
            case INT:
            case LONG:
                return BigDecimal.valueOf(n.longValue());
            case BIG_INTEGER:
                return new BigDecimal((BigInteger) n);
            }
            // float or double
            return BigDecimal.valueOf(n.doubleValue());
        }

        @Override
        public double getDoubleValue() throws IOException, JsonParseException {
            return getNumberValue().doubleValue();
        }

        @Override
        public float getFloatValue() throws IOException, JsonParseException {
            return getNumberValue().floatValue();
        }

        @Override
        public int getIntValue() throws IOException, JsonParseException {
            return getNumberValue().intValue();
        }

        @Override
        public long getLongValue() throws IOException, JsonParseException {
            return getNumberValue().longValue();
        }

        @Override
        public NumberType getNumberType() throws IOException, JsonParseException
        {
            Number n = getNumberValue();
            if (n instanceof Integer) return NumberType.INT;
            if (n instanceof Long) return NumberType.LONG;
            if (n instanceof Double) return NumberType.DOUBLE;
            if (n instanceof BigDecimal) return NumberType.BIG_DECIMAL;
            if (n instanceof Float) return NumberType.FLOAT;
            if (n instanceof BigInteger) return NumberType.BIG_INTEGER;
            return null;
        }

        @Override
        public final Number getNumberValue() throws IOException, JsonParseException {
            _checkIsNumber();
            return (Number) _segment.get(_segmentPtr);
        }
        
        /*
        ////////////////////////////////////////////////////
        // Public API, access to token information, binary
        ////////////////////////////////////////////////////
         */

        @Override
        public byte[] getBinaryValue(Base64Variant b64variant) throws IOException, JsonParseException {
            // TODO Auto-generated method stub
            return null;
        }

        /*
        ////////////////////////////////////////////////////
        // Internal methods
        ////////////////////////////////////////////////////
         */

        protected void _checkIsNumber() throws JsonParseException
        {
            if (_currToken == null || !_currToken.isNumeric()) {
               throw new JsonParseException("Current token ("+_currToken+") not numeric, can not use numeric value accessors",
                       getCurrentLocation());
            }
        }
    }
    
    /**
     * Individual segment of TokenBuffer that can store up to 16 tokens
     * (limited by 4 bits per token type marker requirement).
     * Current implementation uses fixed length array; could alternatively
     * use 16 distinct fields and switch statement (slightly more efficient
     * storage, slightly slower access)
     */
    protected final static class Segment 
    {
        public final static int TOKENS_PER_SEGMENT = 16;
        
        /**
         * Static array used for fast conversion between token markers and
         * matching {@link JsonToken} instances
         */
        private final static JsonToken[] TOKEN_TYPES_BY_INDEX;
        static {
            // ... here we know that there are <= 16 values in JsonToken enum
            TOKEN_TYPES_BY_INDEX = new JsonToken[16];
            JsonToken[] t = JsonToken.values();
            System.arraycopy(t, 1, TOKEN_TYPES_BY_INDEX, 1, Math.min(15, t.length - 1));
        }
        
        // // // State

        /**
         * Bit field used to store types of buffered tokens; 4 bits per token.
         * Value 0 is reserved for "not in use"
         */
        protected long _tokenTypes;

        
        // Actual tokens

        protected final Object[] _tokens = new Object[TOKENS_PER_SEGMENT];

        public Segment() { }

        // // // Accessors

        public JsonToken type(int index)
        {
            long l = _tokenTypes;
            if (index > 0) {
                l >>= (index << 2);
            }
            int ix = ((int) l) & 0xF;
            return TOKEN_TYPES_BY_INDEX[ix];
        }
        
        public Object get(int index) {
            return _tokens[index];
        }

        // // // Mutators

        public void set(int index, JsonToken tokenType, Object value)
        {
            _tokens[index] = value;
            long typeCode = tokenType.ordinal();
            /* Assumption here is that there are no overwrites, just appends;
             * and so no masking is needed
             */
            if (index > 0) {
                typeCode <<= (index << 2);
            }
            _tokenTypes |= typeCode;
        }
    }
}
