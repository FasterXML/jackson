package org.codehaus.jackson.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.JsonReadContext;
import org.codehaus.jackson.impl.JsonWriteContext;

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
/* Won't use JsonGeneratorBase, to minimize overhead for validity
 * checking
 */
    extends JsonGenerator
{
    final static int DEFAULT_FEATURES = JsonParser.Feature.collectDefaults();

    /*
    ***********************************************
    * Configuration
    ***********************************************
     */

    /**
     * Object codec to use for stream-based object
     *   conversion through parser/generator interfaces. If null,
     *   such methods can not be used.
     */
    protected ObjectCodec _objectCodec;

    /**
     * Bit flag composed of bits that indicate which
     * {@link org.codehaus.jackson.JsonGenerator.Feature}s
     * are enabled.
     *<p>
     * NOTE: most features have no effect on this class
     */
    protected int _generatorFeatures;

    protected boolean _closed;
    
    /*
    ***********************************************
    * Token buffering state
    ***********************************************
     */

    /**
     * First segment, for contents this buffer has
     */
    protected Segment _first;

    /**
     * Last segment of this buffer, one that is used
     * for appending more tokens
     */
    protected Segment _last;
    
    /**
     * Offset within last segment, 
     */
    protected int _appendOffset;

    /*
    ***********************************************
    * Output state
    ***********************************************
     */

    protected JsonWriteContext _writeContext;

    /*
    ***********************************************
    * Life-cycle
    ***********************************************
     */

    /**
     * @param codec Object codec to use for stream-based object
     *   conversion through parser/generator interfaces. If null,
     *   such methods can not be used.
     */
    public TokenBuffer(ObjectCodec codec)
    {
        _objectCodec = codec;
        _generatorFeatures = DEFAULT_FEATURES;
        _writeContext = JsonWriteContext.createRootContext();
        // at first we have just one segment
        _first = _last = new Segment();
        _appendOffset = 0;
    }
    
    /**
     * Method used to create a {@link JsonParser} that can read contents
     * stored in this buffer. Will use default <code>_objectCodec</code> for
     * object conversions.
     *<p>
     * Note: instances are not synchronized, that is, they are not thread-safe
     * if there are concurrent appends to the underlying buffer.
     * 
     * @return Parser that can be used for reading contents stored in this buffer
     */
    public JsonParser asParser()
    {
        return asParser(_objectCodec);
    }

    /**
     * Method used to create a {@link JsonParser} that can read contents
     * stored in this buffer.
     *<p>
     * Note: instances are not synchronized, that is, they are not thread-safe
     * if there are concurrent appends to the underlying buffer.
     *
     * @param codec Object codec to use for stream-based object
     *   conversion through parser/generator interfaces. If null,
     *   such methods can not be used.
     * 
     * @return Parser that can be used for reading contents stored in this buffer
     */
    public JsonParser asParser(ObjectCodec codec)
    {
        return new Parser(_first, codec);
    }

    /**
     * @param src Parser to use for accessing source information
     *    like location, configured codec
     */
    public JsonParser asParser(JsonParser src)
    {
        Parser p = new Parser(_first, src.getCodec());
        p.setLocation(src.getTokenLocation());
        return p;
    }
    
    /*
    *****************************************************************
    * Other custom methods not needed for implementing interfaces
    *****************************************************************
     */

    /**
     * Helper method that will write all contents of this buffer
     * using given {@link JsonGenerator}.
     *<p>
     * Note: this method would be enough to implement
     * <code>JsonSerializer</code>  for <code>TokenBuffer</code> type;
     * but we can not have upwards
     * references (from core to mapper package); and as such we also
     * can not take second argument.
     */
    public void serialize(JsonGenerator jgen)
        throws IOException, JsonGenerationException
    {
        Segment segment = _first;
        int ptr = -1;

        while (true) {
            if (++ptr >= Segment.TOKENS_PER_SEGMENT) {
                ptr = 0;
                segment = segment.next();
                if (segment == null) break;
            }
            JsonToken t = segment.type(ptr);
            if (t == null) break;

            // Note: copied from 'copyCurrentEvent'...
            switch (t) {
            case START_OBJECT:
                jgen.writeStartObject();
                break;
            case END_OBJECT:
                jgen.writeEndObject();
                break;
            case START_ARRAY:
                jgen.writeStartArray();
                break;
            case END_ARRAY:
                jgen.writeEndArray();
                break;
            case FIELD_NAME:
                jgen.writeFieldName((String) segment.get(ptr));
                break;
            case VALUE_STRING:
                jgen.writeString((String) segment.get(ptr));
                break;
            case VALUE_NUMBER_INT:
                {
                    Number n = (Number) segment.get(ptr);
                    if (n instanceof BigInteger) {
                        jgen.writeNumber((BigInteger) n);
                    } else if (n instanceof Long) {
                        jgen.writeNumber(n.longValue());
                    } else {
                        jgen.writeNumber(n.intValue());
                    }
                }
                break;
            case VALUE_NUMBER_FLOAT:
                {
                    Number n = (Number) segment.get(ptr);
                    if (n instanceof BigDecimal) {
                        jgen.writeNumber((BigDecimal) n);
                    } else if (n instanceof Float) {
                        jgen.writeNumber(n.floatValue());
                    } else {
                        jgen.writeNumber(n.doubleValue());
                    }
                }
                break;
            case VALUE_TRUE:
                jgen.writeBoolean(true);
                break;
            case VALUE_FALSE:
                jgen.writeBoolean(false);
                break;
            case VALUE_NULL:
                jgen.writeNull();
                break;
            case VALUE_EMBEDDED_OBJECT:
                jgen.writeObject(segment.get(ptr));
                break;
            default:
                throw new RuntimeException("Internal error: should never end up through this code path");
            }
        }
    }

    @Override
    public String toString()
    {
        // Let's print up to 100 first tokens...
        final int MAX_COUNT = 100;

        StringBuilder sb = new StringBuilder();
        sb.append("[TokenBuffer: ");
        JsonParser jp = asParser();
        int count = 0;

        while (true) {
            JsonToken t;
            try {
                t = jp.nextToken();
            } catch (IOException ioe) { // should never occur
                throw new IllegalStateException(ioe);
            }
            if (t == null) break;
            if (count < MAX_COUNT) {
                if (count > 0) {
                    sb.append(", ");
                }
                sb.append(t.toString());
            }
            ++count;
        }

        if (count >= MAX_COUNT) {
            sb.append(" ... (truncated ").append(count-MAX_COUNT).append(" entries)");
        }
        sb.append(']');
        return sb.toString();
    }
        
    /*
    *****************************************************************
    * JsonGenerator implementation: configuration
    *****************************************************************
     */

    @Override
    public JsonGenerator enable(Feature f) {
        _generatorFeatures |= f.getMask();
        return this;
    }

    @Override
    public JsonGenerator disable(Feature f) {
        _generatorFeatures &= ~f.getMask();
        return this;
    }

    //public JsonGenerator configure(Feature f, boolean state) { }

    @Override
    public boolean isEnabled(Feature f) {
        return (_generatorFeatures & f.getMask()) != 0;
    }

    public JsonGenerator useDefaultPrettyPrinter() {
        // No-op: we don't indent
        return this;
    }

    public JsonGenerator setCodec(ObjectCodec oc) {
        _objectCodec = oc;
        return this;
    }

    public ObjectCodec getCodec() { return _objectCodec; }

    public final JsonWriteContext getOutputContext() { return _writeContext; }

    /*
    *****************************************************************
    * JsonGenerator implementation: low-level output handling
    *****************************************************************
     */

    public void flush() throws IOException { /* NOP */ }

    public void close() throws IOException {
        _closed = true;
    }

    public boolean isClosed() { return _closed; }

    /*
    *****************************************************************
    * JsonGenerator implementation: write methods, structural
    *****************************************************************
     */

    @Override
    public final void writeStartArray()
        throws IOException, JsonGenerationException
    {
        _append(JsonToken.START_ARRAY);
        _writeContext = _writeContext.createChildArrayContext();
    }

    @Override
    public final void writeEndArray()
        throws IOException, JsonGenerationException
    {
        _append(JsonToken.END_ARRAY);
        // Let's allow unbalanced tho... i.e. not run out of root level, ever
        JsonWriteContext c = _writeContext.getParent();
        if (c != null) {
            _writeContext = c;
        }
    }

    @Override
    public final void writeStartObject()
        throws IOException, JsonGenerationException
    {
        _append(JsonToken.START_OBJECT);
        _writeContext = _writeContext.createChildObjectContext();
    }

    public final void writeEndObject()
        throws IOException, JsonGenerationException
    {
        _append(JsonToken.END_OBJECT);
        // Let's allow unbalanced tho... i.e. not run out of root level, ever
        JsonWriteContext c = _writeContext.getParent();
        if (c != null) {
            _writeContext = c;
        }
    }

    public final void writeFieldName(String name)
        throws IOException, JsonGenerationException
    {
        _append(JsonToken.FIELD_NAME, name);
        _writeContext.writeFieldName(name);
    }

    /*
    *****************************************************************
    * JsonGenerator implementation: write methods, textual
    *****************************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException {
        if (text == null) {
            writeNull();
        } else {
            _append(JsonToken.VALUE_STRING, text);
        }
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        writeString(new String(text, offset, len));
    }

    @Override
    public void writeRaw(String text) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    /*
    *****************************************************************
    * JsonGenerator implementation: write methods, primitive types
    *****************************************************************
     */

    @Override
    public void writeNumber(int i) throws IOException, JsonGenerationException {
        _append(JsonToken.VALUE_NUMBER_INT, Integer.valueOf(i));
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException {
        _append(JsonToken.VALUE_NUMBER_INT, Long.valueOf(l));
    }

    @Override
    public void writeNumber(double d) throws IOException,JsonGenerationException {
        _append(JsonToken.VALUE_NUMBER_FLOAT, Double.valueOf(d));
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException {
        _append(JsonToken.VALUE_NUMBER_FLOAT, Float.valueOf(f));
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException,JsonGenerationException {
        if (dec == null) {
            writeNull();
        } else {
            _append(JsonToken.VALUE_NUMBER_FLOAT, dec);
        }
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException {
        if (v == null) {
            writeNull();
        } else {
            _append(JsonToken.VALUE_NUMBER_INT, v);
        }
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException, JsonGenerationException {
        /* Hmmh... let's actually support this as regular String value write:
         * should work since there is no quoting when buffering
         */
        writeString(encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) throws IOException,JsonGenerationException {
        _append(state ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE);
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException {
        _append(JsonToken.VALUE_NULL);
    }

    /*
    *****************************************************************
    * JsonGenerator implementation: write methods for POJOs/trees
    *****************************************************************
     */

    public void writeObject(Object value)
        throws IOException, JsonProcessingException
    {
        // embedded means that no conversions should be done...
        _append(JsonToken.VALUE_EMBEDDED_OBJECT, value);
    }

    public void writeTree(JsonNode rootNode)
        throws IOException, JsonProcessingException
    {
        /* 31-Dec-2009, tatu: no need to convert trees either is there?
         *  (note: may need to re-evaluate at some point)
         */
        _append(JsonToken.VALUE_EMBEDDED_OBJECT, rootNode);
    }

    /*
     *****************************************************************
     * JsonGenerator implementation; binary
     *****************************************************************
      */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
        throws IOException, JsonGenerationException
    {
        /* 31-Dec-2009, tatu: can do this using multiple alternatives; but for
         *   now, let's try to limit number of conversions.
         *   The only (?) tricky thing is that of whether to preserve variant,
         *   seems pointless, so let's not worry about it unless there's some
         *   compelling reason to.
         */
        byte[] copy = new byte[len];
        System.arraycopy(data, offset, copy, 0, len);
        writeObject(copy);
    }

    /*
     *****************************************************************
     * JsonGenerator implementation; pass-through copy
     *****************************************************************
      */

    @Override
    public void copyCurrentEvent(JsonParser jp) throws IOException, JsonProcessingException
    {
        switch (jp.getCurrentToken()) {
        case START_OBJECT:
            writeStartObject();
            break;
        case END_OBJECT:
            writeEndObject();
            break;
        case START_ARRAY:
            writeStartArray();
            break;
        case END_ARRAY:
            writeEndArray();
            break;
        case FIELD_NAME:
            writeFieldName(jp.getCurrentName());
            break;
        case VALUE_STRING:
            writeString(jp.getTextCharacters(), jp.getTextOffset(), jp.getTextLength());
            break;
        case VALUE_NUMBER_INT:
            switch (jp.getNumberType()) {
            case INT:
                writeNumber(jp.getIntValue());
                break;
            case BIG_INTEGER:
                writeNumber(jp.getBigIntegerValue());
                break;
            default:
                writeNumber(jp.getLongValue());
            }
            break;
        case VALUE_NUMBER_FLOAT:
            switch (jp.getNumberType()) {
            case BIG_DECIMAL:
                writeNumber(jp.getDecimalValue());
                break;
            case FLOAT:
                writeNumber(jp.getFloatValue());
                break;
            default:
                writeNumber(jp.getDoubleValue());
            }
            break;
        case VALUE_TRUE:
            writeBoolean(true);
            break;
        case VALUE_FALSE:
            writeBoolean(false);
            break;
        case VALUE_NULL:
            writeNull();
            break;
        case VALUE_EMBEDDED_OBJECT:
            writeObject(jp.getEmbeddedObject());
            break;
        default:
            throw new RuntimeException("Internal error: should never end up through this code path");
        }
    }

    @Override
    public void copyCurrentStructure(JsonParser jp) throws IOException, JsonProcessingException {
        JsonToken t = jp.getCurrentToken();

        // Let's handle field-name separately first
        if (t == JsonToken.FIELD_NAME) {
            writeFieldName(jp.getCurrentName());
            t = jp.nextToken();
            // fall-through to copy the associated value
        }

        switch (t) {
        case START_ARRAY:
            writeStartArray();
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                copyCurrentStructure(jp);
            }
            writeEndArray();
            break;
        case START_OBJECT:
            writeStartObject();
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                copyCurrentStructure(jp);
            }
            writeEndObject();
            break;
        default: // others are simple:
            copyCurrentEvent(jp);
        }
    }
    
    /*
     *****************************************************************
     * Internal methods
     *****************************************************************
      */
    protected final void _append(JsonToken type) {
        Segment next = _last.append(_appendOffset, type);
        if (next == null) {
            ++_appendOffset;
        } else {
            _last = next;
            _appendOffset = 1; // since we added first at 0
        }
    }

    protected final void _append(JsonToken type, Object value) {
        Segment next = _last.append(_appendOffset, type, value);
        if (next == null) {
            ++_appendOffset;
        } else {
            _last = next;
            _appendOffset = 1;
        }
    }
    
    protected void _reportUnsupportedOperation() {
        throw new UnsupportedOperationException("Called operation not supported for TokenBuffer");
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

        protected transient ByteArrayBuilder _byteBuilder;

        protected JsonLocation _location = null;
        
        /*
        ////////////////////////////////////////////////////
        // Construction, init
        ////////////////////////////////////////////////////
         */
        
        public Parser(Segment firstSeg, ObjectCodec codec) {
            _segment = firstSeg;
            _segmentPtr = -1; // not yet read
            _codec = codec;
            _parsingContext = JsonReadContext.createRootContext(-1, -1);
        }

        public void setLocation(JsonLocation l) {
            _location = l;
        }
        
        @Override
        public ObjectCodec getCodec() { return _codec; }

        @Override
        public void setCodec(ObjectCodec c) { _codec = c; }

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
        public JsonToken nextToken() throws IOException, JsonParseException
        {
            // If we are closed, nothing more to do
            if (_closed || (_segment == null)) return null;

            // Ok, then: any more tokens?
            if (++_segmentPtr >= Segment.TOKENS_PER_SEGMENT) {
                _segmentPtr = 0;
                _segment = _segment.next();
                if (_segment == null) {
                    return null;
                }
            }
            _currToken = _segment.type(_segmentPtr);
            // Field name? Need to update context
            if (_currToken == JsonToken.FIELD_NAME) {
                _parsingContext.setCurrentName((String) _currentObject());
            } else if (_currToken == JsonToken.START_OBJECT) {
                _parsingContext = _parsingContext.createChildObjectContext(-1, -1);
            } else if (_currToken == JsonToken.START_ARRAY) {
                _parsingContext = _parsingContext.createChildArrayContext(-1, -1);
            } else if (_currToken == JsonToken.END_OBJECT
                    || _currToken == JsonToken.END_ARRAY) {
                // Closing JSON Object/Array? Close matching context
                _parsingContext = _parsingContext.getParent();
                // but allow unbalanced cases too (more close markers)
                if (_parsingContext == null) {
                    _parsingContext = JsonReadContext.createRootContext(-1, -1);
                }
            }
            return _currToken;
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
        public JsonLocation getTokenLocation() { return getCurrentLocation(); }

        @Override
        public JsonLocation getCurrentLocation() {
            return (_location == null) ? JsonLocation.NA : _location;
        }

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
                    return (String) _currentObject();
                    // fall through
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                    Object ob = _currentObject();
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
            return (Number) _currentObject();
        }
        
        /*
        ////////////////////////////////////////////////////
        // Public API, access to token information, other
        ////////////////////////////////////////////////////
         */

        private final static int INT_SPACE = 0x0020;

        public Object getEmbeddedObject()
        {
            if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
                return _currentObject();
            }
            return null;
        }

        @Override
        public byte[] getBinaryValue(Base64Variant b64variant) throws IOException, JsonParseException
        {
            // First: maybe we some special types?
            if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
                // Embedded byte array would work nicely...
                Object ob = _currentObject();
                if (ob instanceof byte[]) {
                    return (byte[]) ob;
                }
                // fall through to error case
            }
            if (_currToken != JsonToken.VALUE_STRING) {
                throw _constructError("Current token ("+_currToken+") not VALUE_STRING (or VALUE_EMBEDDED_OBJECT with byte[]), can not access as binary");
            }
            final String str = getText();
            if (str == null) {
                return null;
            }
            ByteArrayBuilder builder = _byteBuilder;
            if (builder == null) {
                _byteBuilder = builder = new ByteArrayBuilder(100);
            }
            _decodeBase64(str, builder, b64variant);
            return builder.toByteArray();
        }

        /*
        ////////////////////////////////////////////////////
        // Internal methods
        ////////////////////////////////////////////////////
         */

        protected void _decodeBase64(String str, ByteArrayBuilder builder, Base64Variant b64variant)
            throws IOException, JsonParseException
        {
            int ptr = 0;
            int len = str.length();
            
            main_loop:
            while (ptr < len) {
                // first, we'll skip preceding white space, if any
                char ch;
                do {
                    ch = str.charAt(ptr++);
                    if (ptr >= len) {
                        break main_loop;
                    }
                } while (ch <= INT_SPACE);
                int bits = b64variant.decodeBase64Char(ch);
                if (bits < 0) {
                    _reportInvalidBase64(b64variant, ch, 0, null);
                }
                int decodedData = bits;
                // then second base64 char; can't get padding yet, nor ws
                if (ptr >= len) {
                    _reportBase64EOF();
                }
                ch = str.charAt(ptr++);
                bits = b64variant.decodeBase64Char(ch);
                if (bits < 0) {
                    _reportInvalidBase64(b64variant, ch, 1, null);
                }
                decodedData = (decodedData << 6) | bits;
                // third base64 char; can be padding, but not ws
                if (ptr >= len) {
                    _reportBase64EOF();
                }
                ch = str.charAt(ptr++);
                bits = b64variant.decodeBase64Char(ch);
                
                // First branch: can get padding (-> 1 byte)
                if (bits < 0) {
                    if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                        _reportInvalidBase64(b64variant, ch, 2, null);
                    }
                    // Ok, must get padding
                    if (ptr >= len) {
                        _reportBase64EOF();
                    }
                    ch = str.charAt(ptr++);
                    if (!b64variant.usesPaddingChar(ch)) {
                        _reportInvalidBase64(b64variant, ch, 3, "expected padding character '"+b64variant.getPaddingChar()+"'");
                    }
                    // Got 12 bits, only need 8, need to shift
                    decodedData >>= 4;
                    builder.append(decodedData);
                    continue;
                }
                // Nope, 2 or 3 bytes
                decodedData = (decodedData << 6) | bits;
                // fourth and last base64 char; can be padding, but not ws
                if (ptr >= len) {
                    _reportBase64EOF();
                }
                ch = str.charAt(ptr++);
                bits = b64variant.decodeBase64Char(ch);
                if (bits < 0) {
                    if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                        _reportInvalidBase64(b64variant, ch, 3, null);
                    }
                    decodedData >>= 2;
                    builder.appendTwoBytes(decodedData);
                } else {
                    // otherwise, our triple is now complete
                    decodedData = (decodedData << 6) | bits;
                    builder.appendThreeBytes(decodedData);
                }
            }
        }

        protected final Object _currentObject() {
            return _segment.get(_segmentPtr);
        }

        protected void _checkIsNumber() throws JsonParseException
        {
            if (_currToken == null || !_currToken.isNumeric()) {
                throw _constructError("Current token ("+_currToken+") not numeric, can not use numeric value accessors");
            }
        }

        /**
         * @param bindex Relative index within base64 character unit; between 0
         *   and 3 (as unit has exactly 4 characters)
         */
        protected void _reportInvalidBase64(Base64Variant b64variant, char ch, int bindex, String msg)
            throws JsonParseException
        {
            String base;
            if (ch <= INT_SPACE) {
                base = "Illegal white space character (code 0x"+Integer.toHexString(ch)+") as character #"+(bindex+1)+" of 4-char base64 unit: can only used between units";
            } else if (b64variant.usesPaddingChar(ch)) {
                base = "Unexpected padding character ('"+b64variant.getPaddingChar()+"') as character #"+(bindex+1)+" of 4-char base64 unit: padding only legal as 3rd or 4th character";
            } else if (!Character.isDefined(ch) || Character.isISOControl(ch)) {
                // Not sure if we can really get here... ? (most illegal xml chars are caught at lower level)
                base = "Illegal character (code 0x"+Integer.toHexString(ch)+") in base64 content";
            } else {
                base = "Illegal character '"+ch+"' (code 0x"+Integer.toHexString(ch)+") in base64 content";
            }
            if (msg != null) {
                base = base + ": " + msg;
            }
            throw _constructError(base);
        }

        protected void _reportBase64EOF() throws JsonParseException {
            throw _constructError("Unexpected end-of-String in base64 content");
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

        // // // Linking
        
        protected Segment _next;
        
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

        public Segment next() { return _next; }
        
        // // // Mutators

        public Segment append(int index, JsonToken tokenType)
        {
            if (index < TOKENS_PER_SEGMENT) {
                set(index, tokenType);
                return null;
            }
            _next = new Segment();
            _next.set(0, tokenType);
            return _next;
        }

        public Segment append(int index, JsonToken tokenType, Object value)
        {
            if (index < TOKENS_PER_SEGMENT) {
                set(index, tokenType, value);
                return null;
            }
            _next = new Segment();
            _next.set(0, tokenType, value);
            return _next;
        }
        
        public void set(int index, JsonToken tokenType)
        {
            long typeCode = tokenType.ordinal();
            /* Assumption here is that there are no overwrites, just appends;
             * and so no masking is needed
             */
            if (index > 0) {
                typeCode <<= (index << 2);
            }
            _tokenTypes |= typeCode;
        }

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
