package org.codehaus.jackson.smile;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.io.SerializedString;
import org.codehaus.jackson.impl.JsonGeneratorBase;
import org.codehaus.jackson.impl.JsonWriteContext;

import static org.codehaus.jackson.smile.SmileConstants.*;

/**
 * {@link JsonGenerator} implementation for the experimental "Binary JSON Infoset".
 * 
 * @author tatu
 */
public class SmileGenerator
    extends JsonGeneratorBase
{
    /**
     * Enumeration that defines all togglable features for Smile generators.
     */
    public enum Feature {
        /**
         * Whether to write 4-byte header sequence when starting output or not.
         * If disabled, no header is written; this may be useful in embedded cases
         * where context is enough to know that content is encoded using this format.
         * Note, however, that omitting header means that default settings for
         * shared names/string values can not be changed.
         */
        WRITE_HEADER(true)

        /**
         * Whether write byte marker that signifies end of logical content segment
         * ({@link SmileConstants#BYTE_MARKER_END_OF_CONTENT}) when
         * {@link #close} is called or not. This can be useful when outputting
         * multiple adjacent logical content segments (documents) into single
         * physical output unit (file).
         *<p>
         * Default setting is false meaning that such marker is not written.
         */
        ,WRITE_END_MARKER(false)
        
        /**
         * Whether to use simple 7-bit per byte encoding for binary content when output.
         * This is necessary ensure that byte 0xFF will never be included in content output.
         * For other data types this limitation is handled automatically; but since overhead
         * for binary data (14% size expansion, processing overhead) is non-negligible,
         * it is not enabled by default. If no binary data is output, feature has no effect.
         *<p>
         * Default setting is true, indicating that binary data is quoted as 7-bit bytes
         * instead of written raw.
         */
        ,ENCODE_BINARY_AS_7BIT(true)

        /**
         * Whether generator should check if it can "share" field names during generating
         * content or not. If enabled, can replace repeating field names with back references,
         * which are more compact and should faster to decode. Downside is that there is some
         * overhead for writing (need to track existing values, check), as well as decoding.
         *<p>
         * Since field names tend to repeat quite often, this setting is enabled by default.
         */
        ,CHECK_SHARED_NAMES(true)

        /**
         * Whether generator should check if it can "share" short (at most 64 bytes encoded)
         * String value during generating
         * content or not. If enabled, can replace repeating Short String values with back references,
         * which are more compact and should faster to decode. Downside is that there is some
         * overhead for writing (need to track existing values, check), as well as decoding.
         *<p>
         * Since efficiency of this option depends a lot on type of content being produced,
         * this option is disabled by default, and should only be enabled if it is likely that
         * same values repeat relatively often.
         */
        ,CHECK_SHARED_STRING_VALUES(false)
        ;

        protected final boolean _defaultState;
        protected final int _mask;
        
        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
            _mask = (1 << ordinal());
        }
        
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    }

    /**
     * Helper class used for keeping track of possibly shareable String
     * references (for field names and/or short String values)
     */
    protected final static class SharedStringNode
    {
        public final String value;
        public final int index;
        public SharedStringNode next;
        
        public SharedStringNode(String value, int index, SharedStringNode next)
        {
            this.value = value;
            this.index = index;
            this.next = next;
        }
    }
    
    /**
     * To simplify certain operations, we require output buffer length
     * to allow outputting of contiguous 256 character UTF-8 encoded String
     * value. Length of the longest UTF-8 code point (from Java char) is 3 bytes,
     * and we need both initial token byte and single-byte end marker
     * so we get following value.
     *<p>
     * Note: actually we could live with shorter one; absolute minimum would
     * be for encoding 64-character Strings.
     */
    private final static int MIN_BUFFER_LENGTH = (3 * 256) + 2;

    protected final static byte TOKEN_BYTE_LONG_STRING_ASCII = (byte) TOKEN_MISC_LONG_TEXT_ASCII;
    protected final static byte TOKEN_BYTE_LONG_STRING_UNICODE = (byte) TOKEN_MISC_LONG_TEXT_UNICODE;

    protected final static byte TOKEN_BYTE_INT_32 =  (byte) (TOKEN_MISC_INTEGER | TOKEN_MISC_INTEGER_32);
    protected final static byte TOKEN_BYTE_INT_64 =  (byte) (TOKEN_MISC_INTEGER | TOKEN_MISC_INTEGER_64);
    protected final static byte TOKEN_BYTE_BIG_INTEGER =  (byte) (TOKEN_MISC_INTEGER | TOKEN_MISC_INTEGER_BIG);

    protected final static byte TOKEN_BYTE_FLOAT_32 =  (byte) (TOKEN_MISC_FP | TOKEN_MISC_FLOAT_32);
    protected final static byte TOKEN_BYTE_FLOAT_64 =  (byte) (TOKEN_MISC_FP | TOKEN_MISC_FLOAT_64);
    protected final static byte TOKEN_BYTE_BIG_DECIMAL =  (byte) (TOKEN_MISC_FP | TOKEN_MISC_FLOAT_BIG);
    
    protected final static int SURR1_FIRST = 0xD800;
    protected final static int SURR1_LAST = 0xDBFF;
    protected final static int SURR2_FIRST = 0xDC00;
    protected final static int SURR2_LAST = 0xDFFF;

    protected final static long MIN_INT_AS_LONG = (long) Integer.MIN_VALUE;
    protected final static long MAX_INT_AS_LONG = (long) Integer.MAX_VALUE;
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected IOContext _ioContext;

    final protected OutputStream _out;

    /**
     * Bit flag composed of bits that indicate which
     * {@link org.codehaus.jackson.smile.SmileGenerator.Feature}s
     * are enabled.
     */
    protected int _smileFeatures;
    
    /*
    /**********************************************************
    /* Output buffering
    /**********************************************************
     */

    /**
     * Intermediate buffer in which contents are buffered before
     * being written using {@link #_out}.
     */
    protected byte[] _outputBuffer;

    /**
     * Pointer to the next available byte in {@link #_outputBuffer}
     */
    protected int _outputTail = 0;

    /**
     * Offset to index after the last valid index in {@link #_outputBuffer}.
     * Typically same as length of the buffer.
     */
    protected final int _outputEnd;

    /**
     * Intermediate buffer in which characters of a String are copied
     * before being encoded.
     */
    protected char[] _charBuffer;
    
    /**
     * Let's keep track of how many bytes have been output, may prove useful
     * when debugging. This does <b>not</b> include bytes buffered in
     * the output buffer, just bytes that have been written using underlying
     * stream writer.
     */
    protected int _bytesWritten;
    
    /*
    /**********************************************************
    /* Shared String detection
    /**********************************************************
     */

    /**
     * Raw data structure used for checking whether field name to
     * write can be output using back reference or not.
     */
    protected SharedStringNode[] _seenNames;
    
    /**
     * Number of entries in {@link #_seenNames}; -1 if no shared name
     * detection is enabled
     */
    protected int _seenNameCount;

    /**
     * Raw data structure used for checking whether String value to
     * write can be output using back reference or not.
     */
    protected SharedStringNode[] _seenStringValues;
    
    /**
     * Number of entries in {@link #_seenStringValues}; -1 if no shared text value
     * detection is enabled
     */
    protected int _seenStringValueCount;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public SmileGenerator(IOContext ctxt, int jsonFeatures, int smileFeatures,
    		ObjectCodec codec, OutputStream out)
    {
        super(jsonFeatures, codec);
        _smileFeatures = smileFeatures;
        _ioContext = ctxt;
        _out = out;
        _outputBuffer = ctxt.allocWriteEncodingBuffer();
        _outputEnd = _outputBuffer.length;
        _charBuffer = ctxt.allocConcatBuffer();
        // let's just sanity check to prevent nasty odd errors
        if (_outputEnd < MIN_BUFFER_LENGTH) {
            throw new IllegalStateException("Internal encoding buffer length ("+_outputEnd
                    +") too short, must be at least "+MIN_BUFFER_LENGTH);
        }
        if ((smileFeatures & Feature.CHECK_SHARED_NAMES.getMask()) == 0) {
            _seenNames = null;
            _seenNameCount = -1;
        } else {
            _seenNames = new SharedStringNode[64];
            _seenNameCount = 0;
        }

        if ((smileFeatures & Feature.CHECK_SHARED_STRING_VALUES.getMask()) == 0) {
            _seenStringValues = null;
            _seenStringValueCount = -1;
        } else {
            _seenStringValues = new SharedStringNode[64];
            _seenStringValueCount = 0;
        }
}

    /**
     * Method that can be called to explicitly write Smile document header.
     * Note that usually you do not need to call this for first document to output, 
     * but rather only if you intend to write multiple root-level documents
     * with same generator (and even in that case this is optional thing to do).
     * As a result usually only {@link SmileFactory} calls this method.
     */
    public void writeHeader() throws IOException
    {
    	int last = HEADER_BYTE_4;
        if ((_smileFeatures & Feature.CHECK_SHARED_NAMES.getMask()) != 0) {
            last |= SmileConstants.HEADER_BIT_HAS_SHARED_NAMES;
        }
        if ((_smileFeatures & Feature.CHECK_SHARED_STRING_VALUES.getMask()) != 0) {
            last |= SmileConstants.HEADER_BIT_HAS_SHARED_STRING_VALUES;
        }
        if ((_smileFeatures & Feature.ENCODE_BINARY_AS_7BIT.getMask()) == 0) {
            last |= SmileConstants.HEADER_BIT_HAS_RAW_BINARY;
        }
        _writeBytes(HEADER_BYTE_1, HEADER_BYTE_2, HEADER_BYTE_3, (byte) last);
    }
    
    /*
    /**********************************************************
    /* Extended API, configuration
    /**********************************************************
     */

    public SmileGenerator enable(Feature f) {
        _smileFeatures |= f.getMask();
        return this;
    }

    public SmileGenerator disable(Feature f) {
        _smileFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_smileFeatures & f.getMask()) != 0;
    }

    public SmileGenerator configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }
    
    /*
    /**********************************************************
    /* Output method implementations, structural
    /**********************************************************
     */

    @Override
    protected final void _writeStartArray() throws IOException, JsonGenerationException
    {
        _writeByte(TOKEN_LITERAL_START_ARRAY);
    }
    
    @Override
    protected void _writeEndArray()
        throws IOException, JsonGenerationException
    {
        _writeByte(TOKEN_LITERAL_END_ARRAY);
    }

    @Override
    protected void _writeStartObject()
        throws IOException, JsonGenerationException
    {
        _writeByte(TOKEN_LITERAL_START_OBJECT);
    }

    @Override
    protected void _writeEndObject()
        throws IOException, JsonGenerationException
    {
        _writeByte(TOKEN_LITERAL_END_OBJECT);
    }

    @Override
    protected void _writeFieldName(String name, boolean commaBefore) throws IOException, JsonGenerationException
    {
        int len = name.length();
        if (len == 0) {
            _writeByte(TOKEN_KEY_EMPTY_STRING);
            return;
        }
        // First: is it something we can share?
        if (_seenNameCount >= 0) {
            int ix = _findSeenName(name);
            if (ix >= 0) {
                if (ix < 64) {
                    _writeByte((byte) (TOKEN_PREFIX_KEY_SHARED_SHORT + ix));
                } else {
                    _writeBytes(((byte) (TOKEN_PREFIX_KEY_SHARED_LONG + (ix >> 8))), (byte) ix);
                }
                return;
            }
        }
        if (len <= MAX_SHORT_NAME_ASCII_BYTES) { // possibly short strings (not necessarily)
            // first: ensure we have enough space
            if ((_outputTail + MIN_BUFFER_FOR_POSSIBLE_SHORT_STRING) >= _outputEnd) {
                _flushBuffer();
            }
            // then let's copy String chars to char buffer, faster than using getChar (measured, profiled)
            name.getChars(0, len, _charBuffer, 0);
            int origOffset = _outputTail;
            ++_outputTail; // to reserve space for type token
            int byteLen = _shortUTF8Encode(_charBuffer, 0, len);
            byte typeToken;
            
            // Ascii?
            if (byteLen == len) {
                if (byteLen <= MAX_SHORT_NAME_ASCII_BYTES) { // yes, is short indeed
                    typeToken = (byte) ((TOKEN_PREFIX_KEY_ASCII - 1) + byteLen);
                } else { // longer albeit Ascii
                    typeToken = TOKEN_KEY_LONG_STRING;
                    // and we will need String end marker byte
                    _outputBuffer[_outputTail++] = BYTE_MARKER_END_OF_STRING;
                }
            } else { // not all ASCII
                if (byteLen <= MAX_SHORT_NAME_UNICODE_BYTES) { // yes, is short indeed
                    // note: since 2 is smaller allowed length, offset differs from one used for
                    typeToken = (byte) ((TOKEN_PREFIX_KEY_UNICODE - 2) + byteLen);
                } else { // nope, longer non-ASCII Strings
                    typeToken = TOKEN_KEY_LONG_STRING;
                    // and we will need String end marker byte
                    _outputBuffer[_outputTail++] = BYTE_MARKER_END_OF_STRING;
                }
            }
            // and then sneak in type token now that know the details
            _outputBuffer[origOffset] = typeToken;
        } else { // "long" String, never shared
            _writeByte(TOKEN_KEY_LONG_STRING);
            // but might still fit within buffer?
            int maxLen = len + len + len + 1;
            if (maxLen <= _outputBuffer.length) { // yes indeed
                if ((_outputTail + maxLen) >= _outputEnd) {
                    _flushBuffer();
                }
                // can we make a copy of chars?                                 
                if (_charBuffer.length >= len) {
                    name.getChars(0, len, _charBuffer, 0);
                     _shortUTF8Encode(_charBuffer, 0, len);
                } else {
                    _mediumUTF8Encode(name);
                }
                _outputBuffer[_outputTail++] = BYTE_MARKER_END_OF_STRING;                
            } else {
                _slowUTF8Encode(name);
                _writeByte(BYTE_MARKER_END_OF_STRING);
            }
        }
        // Also, keep track if we can use back-references (shared names)
        if (_seenNameCount >= 0) {
            _addSeenName(name);
        }
    }

    @Override
    protected void _writeFieldName(SerializedString name, boolean commaBefore) 
        throws IOException, JsonGenerationException
    {
        final int charLen = name.charLength();
        if (charLen == 0) {
            _writeByte(TOKEN_KEY_EMPTY_STRING);
            return;
        }
        // First: is it something we can share?
        if (_seenNameCount >= 0) {
            int ix = _findSeenName(name.getValue());
            if (ix >= 0) {
                if (ix < 64) {
                    _writeByte((byte) (TOKEN_PREFIX_KEY_SHARED_SHORT + ix));
                } else {
                    _writeBytes(((byte) (TOKEN_PREFIX_KEY_SHARED_LONG + (ix >> 8))), (byte) ix);
                } 
                return;
            }
        }
        byte[] bytes = name.asUnquotedUTF8();
        final int byteLen = bytes.length;

        byte typeToken;
        boolean needEndMarker;
        // ASCII?
        if (byteLen == charLen) {
            if (byteLen <= MAX_SHORT_NAME_ASCII_BYTES) {
                typeToken = (byte) ((TOKEN_PREFIX_KEY_ASCII - 1) + byteLen);
                needEndMarker = false;
            } else {
                typeToken = TOKEN_KEY_LONG_STRING;
                needEndMarker = true;
            }
        } else { // nope, Unicode char(s)
            if (byteLen <= MAX_SHORT_NAME_UNICODE_BYTES) {
                // note: since 2 is smaller allowed length, offset differs from one used for
                typeToken = (byte) ((TOKEN_PREFIX_KEY_UNICODE - 2) + byteLen);
                needEndMarker = false;
            } else {
                typeToken = TOKEN_KEY_LONG_STRING;
                needEndMarker = true;
            }            
        }
        // Ok. Enough room?
        if ((_outputTail + byteLen + 2) < _outputEnd) {
            _outputBuffer[_outputTail++] = typeToken;
            System.arraycopy(bytes, 0, _outputBuffer, _outputTail, byteLen);
            _outputTail += byteLen;
            if (needEndMarker) {
                _outputBuffer[_outputTail++] = BYTE_MARKER_END_OF_STRING;
            }
        } else {
            // quote either before or after flush...
            if ((_outputTail + MIN_BUFFER_FOR_POSSIBLE_SHORT_STRING) < _outputEnd) {
                _outputBuffer[_outputTail++] = typeToken;
                _flushBuffer();
            } else {
                _flushBuffer();
                _outputBuffer[_outputTail++] = typeToken;
            }
            // either way, do intermediate copy if name is relatively short
            // Need to copy?
            if (byteLen < MIN_BUFFER_LENGTH) {
                System.arraycopy(bytes, 0, _outputBuffer, _outputTail, byteLen);
                _outputTail += byteLen;
            } else {
                // otherwise, just write as is
                if (_outputTail > 0) {
                    _flushBuffer();
                }
                _out.write(bytes, 0, byteLen);
            }
            if (needEndMarker) {
                _outputBuffer[_outputTail++] = BYTE_MARKER_END_OF_STRING;
            }
        }
        // Also, keep track if we can use back-references (shared names)
        if (_seenNameCount >= 0) {
            _addSeenName(name.getValue());
        }
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException
    {
        _verifyValueWrite("write String value");
        int len = text.length();
        if (len == 0) {
            _writeByte(TOKEN_LITERAL_EMPTY_STRING);
            return;
        }
        // First: is it something we can share?
        if (len <= MAX_SHARED_STRING_LENGTH_BYTES && _seenStringValueCount >= 0) {
            int ix = _findSeenStringValue(text);
            if (ix >= 0) {
                if (ix < 31) { // add 1, as byte 0 is omitted
                    _writeByte((byte) (TOKEN_PREFIX_SHARED_STRING_SHORT + 1 + ix));
                } else {
                    _writeBytes(((byte) (TOKEN_MISC_SHARED_STRING_LONG + (ix >> 8))), (byte) ix);
                }
                return;
            }
        }
        if (len <= MAX_SHORT_VALUE_STRING_BYTES) { // possibly short strings (not necessarily)
            // first: ensure we have enough space
            if ((_outputTail + MIN_BUFFER_FOR_POSSIBLE_SHORT_STRING) >= _outputEnd) {
                _flushBuffer();
            }
            // then let's copy String chars to char buffer, faster than using getChar (measured, profiled)
            text.getChars(0, len, _charBuffer, 0);
            int origOffset = _outputTail;
            ++_outputTail; // to leave room for type token
            int byteLen = _shortUTF8Encode(_charBuffer, 0, len);
            byte typeToken;
            if (byteLen <= MAX_SHORT_VALUE_STRING_BYTES) { // yes, is short indeed
                if (byteLen == len) { // and all ASCII
                    typeToken = (byte) ((TOKEN_PREFIX_TINY_ASCII - 1) + byteLen);
                } else { // not just ASCII
                    // note: since length 1 can not be used here, value range is offset by 2, not 1
                    typeToken = (byte) ((TOKEN_PREFIX_TINY_UNICODE - 2) +  byteLen);
                }
                // plus keep reference, if it could be shared:
                if (_seenStringValueCount >= 0) {
                    _addSeenStringValue(text);
                }
            } else { // nope, longer String 
                typeToken = (byteLen == len) ? TOKEN_BYTE_LONG_STRING_ASCII : TOKEN_BYTE_LONG_STRING_UNICODE;
                // and we will need String end marker byte
                _outputBuffer[_outputTail++] = BYTE_MARKER_END_OF_STRING;
            }
            // and then sneak in type token now that know the details
            _outputBuffer[origOffset] = typeToken;            
        } else { // "long" String, never shared
            // but might still fit within buffer?
            int maxLen = len + len + len + 2;
            if (maxLen <= _outputBuffer.length) { // yes indeed
                if ((_outputTail + maxLen) >= _outputEnd) {
                    _flushBuffer();
                }
                int origOffset = _outputTail;
                // can't say for sure if it's Ascii or Unicode, so:
                _writeByte(TOKEN_BYTE_LONG_STRING_UNICODE);
                int byteLen;
                if (len < _charBuffer.length) {
                    text.getChars(0, len, _charBuffer, 0);
                    byteLen = _shortUTF8Encode(_charBuffer, 0, len);
                } else {
                    byteLen = _mediumUTF8Encode(text);
                }
                // if it's ASCII, let's revise our type determination (to help decoder optimize)
                if (byteLen == len) {
                    _outputBuffer[origOffset] = TOKEN_BYTE_LONG_STRING_ASCII;
                }
                _outputBuffer[_outputTail++] = BYTE_MARKER_END_OF_STRING;                
            } else { // won't fit; can't efficiently rewrite Ascii/unicode marker, so:
                _writeByte(TOKEN_BYTE_LONG_STRING_UNICODE);
                _slowUTF8Encode(text);
                _writeByte(BYTE_MARKER_END_OF_STRING);
            }
        }
    }    
    
    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException
    {
        // Shared strings are tricky; easiest to just construct String, call the other method
        if (len <= MAX_SHARED_STRING_LENGTH_BYTES && _seenStringValueCount >= 0 && len > 0) {
            writeString(new String(text, offset, len));
            return;
        }
        _verifyValueWrite("write String value");
        if (len == 0) {
            _writeByte(TOKEN_LITERAL_EMPTY_STRING);
            return;
        }
        if (len <= MAX_SHORT_VALUE_STRING_BYTES) { // possibly short strings (not necessarily)
            // !!! TODO: check for shared Strings
            // first: ensure we have enough space
            if ((_outputTail + MIN_BUFFER_FOR_POSSIBLE_SHORT_STRING) >= _outputEnd) {
                _flushBuffer();
            }
            int origOffset = _outputTail;
            ++_outputTail; // to leave room for type token
            int byteLen = _shortUTF8Encode(text, offset, offset+len);
            byte typeToken;
            if (byteLen <= MAX_SHORT_VALUE_STRING_BYTES) { // yes, is short indeed
                if (byteLen == len) { // and all ASCII
                    typeToken = (byte) ((TOKEN_PREFIX_TINY_ASCII - 1) + byteLen);
                } else { // not just ASCII
                    typeToken = (byte) ((TOKEN_PREFIX_TINY_UNICODE - 2) + byteLen);
                }
            } else { // nope, longer non-ASCII Strings
                typeToken = TOKEN_BYTE_LONG_STRING_UNICODE;
                // and we will need String end marker byte
                _outputBuffer[_outputTail++] = BYTE_MARKER_END_OF_STRING;
            }
            // and then sneak in type token now that know the details
            _outputBuffer[origOffset] = typeToken;
        } else { // "long" String, never shared
            // but might still fit within buffer?
            int maxLen = len + len + len + 2;
            if (maxLen <= _outputBuffer.length) { // yes indeed
                if ((_outputTail + maxLen) >= _outputEnd) {
                    _flushBuffer();
                }
                int origOffset = _outputTail;
                _writeByte(TOKEN_BYTE_LONG_STRING_UNICODE);
                int byteLen = _shortUTF8Encode(text, offset, offset+len);
                // if it's ASCII, let's revise our type determination (to help decoder optimize)
                if (byteLen == len) {
                    _outputBuffer[origOffset] = TOKEN_BYTE_LONG_STRING_ASCII;
                }
                _outputBuffer[_outputTail++] = BYTE_MARKER_END_OF_STRING;
            } else {
                _writeByte(TOKEN_BYTE_LONG_STRING_UNICODE);
                _slowUTF8Encode(text, offset, offset+len);
                _writeByte(BYTE_MARKER_END_OF_STRING);
            }
        }
    }
    
    /*
    /**********************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException, JsonGenerationException {
        throw _notSupported();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException, JsonGenerationException {
        throw _notSupported();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        throw _notSupported();
    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        throw _notSupported();
    }

    @Override
    public void writeRawValue(String text) throws IOException, JsonGenerationException {
        throw _notSupported();
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException, JsonGenerationException {
        throw _notSupported();
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        throw _notSupported();
    }
    
    /*
    /**********************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException
    {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        if (this.isEnabled(Feature.ENCODE_BINARY_AS_7BIT)) {
            _writeByte((byte) TOKEN_MISC_BINARY_7BIT);
            _write7BitBinaryWithLength(data, offset, len);
        } else {
            _writeByte((byte) TOKEN_MISC_BINARY_RAW );
            _writePositiveVInt(len);
            // raw is dead simple of course:
            _writeBytes(data, offset, len);
        }
    }
    
    /*
    /**********************************************************
    /* Output method implementations, primitive
    /**********************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write boolean value");
        if (state) {
            _writeByte(TOKEN_LITERAL_TRUE);
        } else {
            _writeByte(TOKEN_LITERAL_FALSE);             
        }
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write null value");
        _writeByte(TOKEN_LITERAL_NULL);
    }

    @Override
    public void writeNumber(int i) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
    	// First things first: let's zigzag encode number
        i = SmileUtil.zigzagEncode(i);
        // tiny (single byte) or small (type + 6-bit value) number?
        if (i <= 0x3F && i >= 0) {
            if (i <= 0x1F) { // tiny 
                _writeByte((byte) (TOKEN_PREFIX_SMALL_INT + i));
                return;
            }
            // nope, just small, 2 bytes (type, 1-byte zigzag value) for 6 bit value
            _writeBytes(TOKEN_BYTE_INT_32, (byte) (0x80 + i));
            return;
        }
        // Ok: let's find minimal representation then
        byte b0 = (byte) (0x80 + (i & 0x3F));
        i >>>= 6;
        if (i <= 0x7F) { // 13 bits is enough (== 3 byte total encoding)
            _writeBytes(TOKEN_BYTE_INT_32, (byte) i, b0);
            return;
        }
        byte b1 = (byte) (i & 0x7F);
        i >>= 7;
        if (i <= 0x7F) {
            _writeBytes(TOKEN_BYTE_INT_32, (byte) i, b1, b0);
            return;
        }
        byte b2 = (byte) (i & 0x7F);
        i >>= 7;
        if (i <= 0x7F) {
            _writeBytes(TOKEN_BYTE_INT_32, (byte) i, b2, b1, b0);
            return;
        }
        // no, need all 5 bytes
        byte b3 = (byte) (i & 0x7F);
        _writeBytes(TOKEN_BYTE_INT_32, (byte) (i >> 7), b3, b2, b1, b0);
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException
    {
        // First: maybe 32 bits is enough?
    	if (l <= MAX_INT_AS_LONG && l >= MIN_INT_AS_LONG) {
            writeNumber((int) l);
            return;
        }
        _verifyValueWrite("write number");
        // Then let's zigzag encode it
        
        l = SmileUtil.zigzagEncode(l);
        // Ok, well, we do know that 5 lowest-significant bytes are needed
        int i = (int) l;
        // 4 can be extracted from lower int
        byte b0 = (byte) (0x80 + (i & 0x3F)); // sign bit set in the last byte
        byte b1 = (byte) ((i >> 6) & 0x7F);
        byte b2 = (byte) ((i >> 13) & 0x7F);
        byte b3 = (byte) ((i >> 20) & 0x7F);
        // fifth one is split between ints:
        l >>>= 27;
        byte b4 = (byte) (((int) l) & 0x7F);

        // which may be enough?
        i = (int) (l >> 7);
        if (i == 0) {
            _writeBytes(TOKEN_BYTE_INT_64, b4, b3, b2, b1, b0);
            return;
        }

        if (i <= 0x7F) {
            _writeBytes(TOKEN_BYTE_INT_64, (byte) i);
            _writeBytes(b4, b3, b2, b1, b0);
            return;
        }
        byte b5 = (byte) (i & 0x7F);
        i >>= 7;
        if (i <= 0x7F) {
            _writeBytes(TOKEN_BYTE_INT_64, (byte) i);
            _writeBytes(b5, b4, b3, b2, b1, b0);
            return;
        }
        byte b6 = (byte) (i & 0x7F);
        i >>= 7;
        if (i <= 0x7F) {
            _writeBytes(TOKEN_BYTE_INT_64, (byte) i, b6);
            _writeBytes(b5, b4, b3, b2, b1, b0);
            return;
        }
        byte b7 = (byte) (i & 0x7F);
        i >>= 7;
        if (i <= 0x7F) {
            _writeBytes(TOKEN_BYTE_INT_64, (byte) i, b7, b6);
            _writeBytes(b5, b4, b3, b2, b1, b0);
            return;
        }
        byte b8 = (byte) (i & 0x7F);
        i >>= 7;
        // must be done, with 10 bytes! (9 * 7 + 6 == 69 bits; only need 63)
        _writeBytes(TOKEN_BYTE_INT_64, (byte) i, b8, b7, b6);
        _writeBytes(b5, b4, b3, b2, b1, b0);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        // quite simple: type, and then VInt-len prefixed 7-bit encoded binary data:
        _writeByte(TOKEN_BYTE_BIG_INTEGER);
        byte[] data = v.toByteArray();
        _write7BitBinaryWithLength(data, 0, data.length);
    }
    
    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException
    {
        // Ok, now, we needed token type byte plus 10 data bytes (7 bits each)
        _ensureRoomForOutput(11);
        _verifyValueWrite("write number");
        /* 17-Apr-2010, tatu: could also use 'doubleToIntBits', but it seems more accurate to use
         * exact representation; and possibly faster. However, if there are cases
         * where collapsing of NaN was needed (for non-Java clients), this can
         * be changed
         */
        long l = Double.doubleToRawLongBits(d);
        _outputBuffer[_outputTail++] = TOKEN_BYTE_FLOAT_64;
        // Handle first 29 bits (single bit first, then 4 x 7 bits)
        int hi5 = (int) (l >>> 35);
        _outputBuffer[_outputTail+4] = (byte) (hi5 & 0x7F);
        hi5 >>= 7;
        _outputBuffer[_outputTail+3] = (byte) (hi5 & 0x7F);
        hi5 >>= 7;
        _outputBuffer[_outputTail+2] = (byte) (hi5 & 0x7F);
        hi5 >>= 7;
        _outputBuffer[_outputTail+1] = (byte) (hi5 & 0x7F);
        hi5 >>= 7;
        _outputBuffer[_outputTail] = (byte) hi5;
        _outputTail += 5;
        // Then split byte (one that crosses lo/hi int boundary), 7 bits
        {
            int mid = (int) (l >> 28);
            _outputBuffer[_outputTail++] = (byte) (mid & 0x7F);
        }
        // and then last 4 bytes (28 bits)
        int lo4 = (int) l;
        _outputBuffer[_outputTail+3] = (byte) (lo4 & 0x7F);
        lo4 >>= 7;
        _outputBuffer[_outputTail+2] = (byte) (lo4 & 0x7F);
        lo4 >>= 7;
        _outputBuffer[_outputTail+1] = (byte) (lo4 & 0x7F);
        lo4 >>= 7;
        _outputBuffer[_outputTail] = (byte) (lo4 & 0x7F);
        _outputTail += 4;
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException
    {
        // Ok, now, we needed token type byte plus 5 data bytes (7 bits each)
        _ensureRoomForOutput(6);
        _verifyValueWrite("write number");
        
        /* 17-Apr-2010, tatu: could also use 'floatToIntBits', but it seems more accurate to use
         * exact representation; and possibly faster. However, if there are cases
         * where collapsing of NaN was needed (for non-Java clients), this can
         * be changed
         */
        int i = Float.floatToRawIntBits(f);
        _outputBuffer[_outputTail++] = TOKEN_BYTE_FLOAT_32;
        _outputBuffer[_outputTail+4] = (byte) (i & 0x7F);
        i >>= 7;
        _outputBuffer[_outputTail+3] = (byte) (i & 0x7F);
        i >>= 7;
        _outputBuffer[_outputTail+2] = (byte) (i & 0x7F);
        i >>= 7;
        _outputBuffer[_outputTail+1] = (byte) (i & 0x7F);
        i >>= 7;
        _outputBuffer[_outputTail] = (byte) (i & 0x7F);
        _outputTail += 5;
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException, JsonGenerationException
    {
        if (dec == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeByte(TOKEN_BYTE_BIG_DECIMAL);
        int scale = dec.scale();
        // Ok, first output scale as VInt
        _writeSignedVInt(scale);
        BigInteger unscaled = dec.unscaledValue();
        byte[] data = unscaled.toByteArray();
        // And then binary data in "safe" mode (7-bit values)
        _write7BitBinaryWithLength(data, 0, data.length);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException,JsonGenerationException, UnsupportedOperationException
    {
        /* 17-Apr-2010, tatu: Could try parsing etc; but for now let's not bother, it could
         *   just be some non-standard representation that caller wants to pass
         */
        throw _notSupported();
    }

    /*
    /**********************************************************
    /* Implementations for other methods
    /**********************************************************
     */
    
    @Override
    protected final void _verifyValueWrite(String typeMsg)
        throws IOException, JsonGenerationException
    {
        int status = _writeContext.writeValue();
        if (status == JsonWriteContext.STATUS_EXPECT_NAME) {
            _reportError("Can not "+typeMsg+", expecting field name");
        }
    }
    
    /*
    /**********************************************************
    /* Low-level output handling
    /**********************************************************
     */

    @Override
    public final void flush() throws IOException
    {
        _flushBuffer();
        _out.flush();
    }

    @Override
    public void close() throws IOException
    {
        boolean wasClosed = _closed;
        
        super.close();

        /* 05-Dec-2008, tatu: To add [JACKSON-27], need to close open
         *   scopes.
         */
        // First: let's see that we still have buffers...
        if (_outputBuffer != null
            && isEnabled(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)) {
            while (true) {
                JsonStreamContext ctxt = getOutputContext();
                if (ctxt.inArray()) {
                    writeEndArray();
                } else if (ctxt.inObject()) {
                    writeEndObject();
                } else {
                    break;
                }
            }
        }
        if (!wasClosed && isEnabled(Feature.WRITE_END_MARKER)) {
            _writeByte(BYTE_MARKER_END_OF_CONTENT);
        }
        _flushBuffer();

        if (_ioContext.isResourceManaged() || isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET)) {
            _out.close();
        } else {
            // If we can't close it, we should at least flush
            _out.flush();
        }
        // Internal buffer(s) generator has can now be released as well
        _releaseBuffers();
    }

    /*
    /**********************************************************
    /* Internal methods, UTF-8 encoding
    /**********************************************************
    */

    private final int _shortUTF8Encode(char[] str, int i, int end)
    {
        // First: let's see if it's all ASCII: that's rather fast
        int ptr = _outputTail;
        final byte[] outBuf = _outputBuffer;
        do {
            int c = str[i];
            if (c > 0x7F) {
                break;
            }
            outBuf[ptr++] = (byte) c;
        } while (++i < end);
        int codedLen = ptr - _outputTail;
        _outputTail = ptr;
        if (i < end) { // offline not-all-ASCII case
            ptr = _outputTail;
            _shortUTF8Encode2(str, i, end);
            codedLen += (_outputTail - ptr);
        }
        return codedLen;
    }

    
    /**
     * Helper method used to encode Strings that are short enough that UTF-8
     * encoded version is known to fit in the buffer
     */
    private final int _mediumUTF8Encode(String str)
    {
        int i = 0;
        final int len = str.length();
        int ptr = _outputTail;

        // First: let's see if it's all ASCII: that's rather fast
        final byte[] outBuf = _outputBuffer;
        do {
            int c = str.charAt(i);
            if (c > 0x7F) {
                break;
            }
            outBuf[ptr++] = (byte) c;
        } while (++i < len);
        int codedLen = ptr - _outputTail;
        _outputTail = ptr;
        if (i < len) { // offline not-all-ASCII case
            ptr = _outputTail;            
            _shortUTF8Encode2(str, i);
            codedLen += (_outputTail - ptr);
        }
        return codedLen;
    }

    
    /**
     * Second part, slightly slower, which needs to deal with
     * multi-byte aspects of UTF-8 encoding
     */
    private final void _shortUTF8Encode2(String str, int i)
    {
        final int len = str.length();
        do {
            int c = str.charAt(i++);
            if (c <= 0x7F) {
                _outputBuffer[_outputTail++] = (byte) c;
                continue;
            }
            // Nope, multi-byte:
            if (c < 0x800) { // 2-byte
                _outputBuffer[_outputTail++] = (byte) (0xc0 | (c >> 6));
                _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
                continue;
            }
            // 3 or 4 bytes (surrogate)
            // Surrogates?
            if (c < SURR1_FIRST || c > SURR2_LAST) { // nope, regular 3-byte character
                _outputBuffer[_outputTail++] = (byte) (0xe0 | (c >> 12));
                _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
                continue;
            }
            // Yup, a surrogate pair
            if (c > SURR1_LAST) { // must be from first range; second won't do
                _throwIllegalSurrogate(c);
            }
            // ... meaning it must have a pair
            if (i >= len) {
                _throwIllegalSurrogate(c);
            }
            c = _convertSurrogate(c, str.charAt(i++));
            if (c > 0x10FFFF) { // illegal in JSON as well as in XML
                _throwIllegalSurrogate(c);
            }
            _outputBuffer[_outputTail++] = (byte) (0xf0 | (c >> 18));
            _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 12) & 0x3f));
            _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 6) & 0x3f));
            _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
        } while (i < len);
    }

    private final void _shortUTF8Encode2(char[] str, int i, int end)
    {
        do {
            int c = str[i++];
            if (c <= 0x7F) {
                _outputBuffer[_outputTail++] = (byte) c;
                continue;
            }
            // Nope, multi-byte:
            if (c < 0x800) { // 2-byte
                _outputBuffer[_outputTail++] = (byte) (0xc0 | (c >> 6));
                _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
                continue;
            }
            // 3 or 4 bytes (surrogate)
            // Surrogates?
            if (c < SURR1_FIRST || c > SURR2_LAST) { // nope, regular 3-byte character
                _outputBuffer[_outputTail++] = (byte) (0xe0 | (c >> 12));
                _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
                continue;
            }
            // Yup, a surrogate pair
            if (c > SURR1_LAST) { // must be from first range; second won't do
                _throwIllegalSurrogate(c);
            }
            // ... meaning it must have a pair
            if (i >= end) {
                _throwIllegalSurrogate(c);
            }
            c = _convertSurrogate(c, str[i++]);
            if (c > 0x10FFFF) { // illegal in JSON as well as in XML
                _throwIllegalSurrogate(c);
            }
            _outputBuffer[_outputTail++] = (byte) (0xf0 | (c >> 18));
            _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 12) & 0x3f));
            _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 6) & 0x3f));
            _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
        } while (i < end);
    }
    
    private void _slowUTF8Encode(String str) throws IOException
    {
        final int len = str.length();
        int inputPtr = 0;
        final int bufferEnd = _outputEnd - 4;
        
        output_loop:
        for (; inputPtr < len; ) {
            /* First, let's ensure we can output at least 4 bytes
             * (longest UTF-8 encoded codepoint):
             */
            if (_outputTail >= bufferEnd) {
                _flushBuffer();
            }
            int c = str.charAt(inputPtr++);
            // And then see if we have an ASCII char:
            if (c <= 0x7F) { // If so, can do a tight inner loop:
                _outputBuffer[_outputTail++] = (byte)c;
                // Let's calc how many ASCII chars we can copy at most:
                int maxInCount = (len - inputPtr);
                int maxOutCount = (_outputEnd - _outputTail);

                if (maxInCount > maxOutCount) {
                    maxInCount = maxOutCount;
                }
                maxInCount += inputPtr;
                ascii_loop:
                while (true) {
                    if (inputPtr >= maxInCount) { // done with max. ascii seq
                        continue output_loop;
                    }
                    c = str.charAt(inputPtr++);
                    if (c > 0x7F) {
                        break ascii_loop;
                    }
                    _outputBuffer[_outputTail++] = (byte) c;
                }
            }

            // Nope, multi-byte:
            if (c < 0x800) { // 2-byte
                _outputBuffer[_outputTail++] = (byte) (0xc0 | (c >> 6));
                _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
            } else { // 3 or 4 bytes
                // Surrogates?
                if (c < SURR1_FIRST || c > SURR2_LAST) {
                    _outputBuffer[_outputTail++] = (byte) (0xe0 | (c >> 12));
                    _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                    _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
                    continue;
                }
                // Yup, a surrogate:
                if (c > SURR1_LAST) { // must be from first range
                    _throwIllegalSurrogate(c);
                }
                // and if so, followed by another from next range
                if (inputPtr >= len) {
                    _throwIllegalSurrogate(c);
                }
                c = _convertSurrogate(c, str.charAt(inputPtr++));
                if (c > 0x10FFFF) { // illegal, as per RFC 4627
                    _throwIllegalSurrogate(c);
                }
                _outputBuffer[_outputTail++] = (byte) (0xf0 | (c >> 18));
                _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 12) & 0x3f));
                _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
            }
        }
    }

    private void _slowUTF8Encode(char[] str, int inputPtr, int inputEnd) throws IOException
    {
        final int bufferEnd = _outputEnd - 4;
        
        output_loop:
        while (inputPtr < inputEnd) {
            /* First, let's ensure we can output at least 4 bytes
             * (longest UTF-8 encoded codepoint):
             */
            if (_outputTail >= bufferEnd) {
                _flushBuffer();
            }
            int c = str[inputPtr++];
            // And then see if we have an ASCII char:
            if (c <= 0x7F) { // If so, can do a tight inner loop:
                _outputBuffer[_outputTail++] = (byte)c;
                // Let's calc how many ASCII chars we can copy at most:
                int maxInCount = (inputEnd - inputPtr);
                int maxOutCount = (_outputEnd - _outputTail);

                if (maxInCount > maxOutCount) {
                    maxInCount = maxOutCount;
                }
                maxInCount += inputPtr;
                ascii_loop:
                while (true) {
                    if (inputPtr >= maxInCount) { // done with max. ascii seq
                        continue output_loop;
                    }
                    c = str[inputPtr++];
                    if (c > 0x7F) {
                        break ascii_loop;
                    }
                    _outputBuffer[_outputTail++] = (byte) c;
                }
            }

            // Nope, multi-byte:
            if (c < 0x800) { // 2-byte
                _outputBuffer[_outputTail++] = (byte) (0xc0 | (c >> 6));
                _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
            } else { // 3 or 4 bytes
                // Surrogates?
                if (c < SURR1_FIRST || c > SURR2_LAST) {
                    _outputBuffer[_outputTail++] = (byte) (0xe0 | (c >> 12));
                    _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                    _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
                    continue;
                }
                // Yup, a surrogate:
                if (c > SURR1_LAST) { // must be from first range
                    _throwIllegalSurrogate(c);
                }
                // and if so, followed by another from next range
                if (inputPtr >= inputEnd) {
                    _throwIllegalSurrogate(c);
                }
                c = _convertSurrogate(c, str[inputPtr++]);
                if (c > 0x10FFFF) { // illegal, as per RFC 4627
                    _throwIllegalSurrogate(c);
                }
                _outputBuffer[_outputTail++] = (byte) (0xf0 | (c >> 18));
                _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 12) & 0x3f));
                _outputBuffer[_outputTail++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                _outputBuffer[_outputTail++] = (byte) (0x80 | (c & 0x3f));
            }
        }
    }
    
    /**
     * Method called to calculate UTF codepoint, from a surrogate pair.
     */
    private int _convertSurrogate(int firstPart, int secondPart)
    {
        // Ok, then, is the second part valid?
        if (secondPart < SURR2_FIRST || secondPart > SURR2_LAST) {
            throw new IllegalArgumentException("Broken surrogate pair: first char 0x"+Integer.toHexString(firstPart)+", second 0x"+Integer.toHexString(secondPart)+"; illegal combination");
        }
        return 0x10000 + ((firstPart - SURR1_FIRST) << 10) + (secondPart - SURR2_FIRST);
    }

    private void _throwIllegalSurrogate(int code)
    {
        if (code > 0x10FFFF) { // over max?
            throw new IllegalArgumentException("Illegal character point (0x"+Integer.toHexString(code)+") to output; max is 0x10FFFF as per RFC 4627");
        }
        if (code >= SURR1_FIRST) {
            if (code <= SURR1_LAST) { // Unmatched first part (closing without second part?)
                throw new IllegalArgumentException("Unmatched first part of surrogate pair (0x"+Integer.toHexString(code)+")");
            }
            throw new IllegalArgumentException("Unmatched second part of surrogate pair (0x"+Integer.toHexString(code)+")");
        }
        // should we ever get this?
        throw new IllegalArgumentException("Illegal character point (0x"+Integer.toHexString(code)+") to output");
    }

    /*
    /**********************************************************
    /* Internal methods, writing bytes
    /**********************************************************
    */

    private final void _ensureRoomForOutput(int needed) throws IOException
    {
        if ((_outputTail + needed) >= _outputEnd) {
            _flushBuffer();
        }        
    }
    
    private final void _writeByte(byte b) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b;
    }

    private final void _writeBytes(byte b1, byte b2) throws IOException
    {
        if ((_outputTail + 1) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b1;
        _outputBuffer[_outputTail++] = b2;
    }

    private final void _writeBytes(byte b1, byte b2, byte b3) throws IOException
    {
        if ((_outputTail + 2) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b1;
        _outputBuffer[_outputTail++] = b2;
        _outputBuffer[_outputTail++] = b3;
    }

    private final void _writeBytes(byte b1, byte b2, byte b3, byte b4) throws IOException
    {
        if ((_outputTail + 3) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b1;
        _outputBuffer[_outputTail++] = b2;
        _outputBuffer[_outputTail++] = b3;
        _outputBuffer[_outputTail++] = b4;
    }

    private final void _writeBytes(byte b1, byte b2, byte b3, byte b4, byte b5) throws IOException
    {
        if ((_outputTail + 4) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b1;
        _outputBuffer[_outputTail++] = b2;
        _outputBuffer[_outputTail++] = b3;
        _outputBuffer[_outputTail++] = b4;
        _outputBuffer[_outputTail++] = b5;
    }

    private final void _writeBytes(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6) throws IOException
    {
        if ((_outputTail + 5) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b1;
        _outputBuffer[_outputTail++] = b2;
        _outputBuffer[_outputTail++] = b3;
        _outputBuffer[_outputTail++] = b4;
        _outputBuffer[_outputTail++] = b5;
        _outputBuffer[_outputTail++] = b6;
    }

    private final void _writeBytes(byte[] data, int offset, int len) throws IOException
    {
        if (len > 0) {
            while (true) {
                int currLen = Math.min(len, (_outputEnd - _outputTail));
                System.arraycopy(data, offset, _outputBuffer, _outputTail, currLen);
                _outputTail += currLen;
                if ((len -= currLen) == 0) {
                    break;
                }
                _flushBuffer();
                offset += currLen;
            }
        }
    }

    /**
     * Helper method for writing a 32-bit positive (really 31-bit then) value.
     * Value is NOT zigzag encoded (since there is no sign bit to worry about)
     */
    private void _writePositiveVInt(int i) throws IOException
    {
        // At most 5 bytes (4 * 7 + 6 bits == 34 bits)
        _ensureRoomForOutput(5);
        byte b0 = (byte) (0x80 + (i & 0x3F));
        i >>= 6;
        if (i <= 0x7F) { // 6 or 13 bits is enough (== 2 or 3 byte total encoding)
            if (i > 0) {
                _outputBuffer[_outputTail++] = (byte) i;
            }
            _outputBuffer[_outputTail++] = b0;
            return;
        }
        byte b1 = (byte) (i & 0x7F);
        i >>= 7;
        if (i <= 0x7F) {
            _outputBuffer[_outputTail++] = (byte) i;
            _outputBuffer[_outputTail++] = b1;
            _outputBuffer[_outputTail++] = b0;            
        } else {
            byte b2 = (byte) (i & 0x7F);
            i >>= 7;
            if (i <= 0x7F) {
                _outputBuffer[_outputTail++] = (byte) i;
                _outputBuffer[_outputTail++] = b2;
                _outputBuffer[_outputTail++] = b1;
                _outputBuffer[_outputTail++] = b0;            
            } else {
                byte b3 = (byte) (i & 0x7F);
                _outputBuffer[_outputTail++] = (byte) (i >> 7);
                _outputBuffer[_outputTail++] = b3;
                _outputBuffer[_outputTail++] = b2;
                _outputBuffer[_outputTail++] = b1;
                _outputBuffer[_outputTail++] = b0;            
            }
        }
    }

    /**
     * Helper method for writing 32-bit signed value, using
     * "zig zag encoding" (see protocol buffers for explanation -- basically,
     * sign bit is moved as LSB, rest of value shifted left by one)
     * coupled with basic variable length encoding
     */
    private void _writeSignedVInt(int input) throws IOException
    {
        _writePositiveVInt(SmileUtil.zigzagEncode(input));
    }

    protected void _write7BitBinaryWithLength(byte[] data, int offset, int len) throws IOException
    {
        _writePositiveVInt(len);
        // first, let's handle full 7-byte chunks
        while (len >= 7) {
            if ((_outputTail + 8) >= _outputEnd) {
                _flushBuffer();
            }
            int i = data[offset++]; // 1st byte
            _outputBuffer[_outputTail++] = (byte) ((i >> 1) & 0x7F);
            i = (i << 8) | (data[offset++] & 0xFF); // 2nd
            _outputBuffer[_outputTail++] = (byte) ((i >> 2) & 0x7F);
            i = (i << 8) | (data[offset++] & 0xFF); // 3rd
            _outputBuffer[_outputTail++] = (byte) ((i >> 3) & 0x7F);
            i = (i << 8) | (data[offset++] & 0xFF); // 4th
            _outputBuffer[_outputTail++] = (byte) ((i >> 4) & 0x7F);
            i = (i << 8) | (data[offset++] & 0xFF); // 5th
            _outputBuffer[_outputTail++] = (byte) ((i >> 5) & 0x7F);
            i = (i << 8) | (data[offset++] & 0xFF); // 6th
            _outputBuffer[_outputTail++] = (byte) ((i >> 6) & 0x7F);
            i = (i << 8) | (data[offset++] & 0xFF); // 7th
            _outputBuffer[_outputTail++] = (byte) ((i >> 7) & 0x7F);
            _outputBuffer[_outputTail++] = (byte) (i & 0x7F);
            len -= 7;
        }
        // and then partial piece, if any
        if (len > 0) {
            // up to 6 bytes to output, resulting in at most 7 bytes (which can encode 49 bits)
            if ((_outputTail + 7) >= _outputEnd) {
                _flushBuffer();
            }
            int i = data[offset++];
            _outputBuffer[_outputTail++] = (byte) ((i >> 1) & 0x7F);
            if (len > 1) {
                i = ((i & 0x01) << 8) | (data[offset++] & 0xFF); // 2nd
                _outputBuffer[_outputTail++] = (byte) ((i >> 2) & 0x7F);
                if (len > 2) {
                    i = ((i & 0x03) << 8) | (data[offset++] & 0xFF); // 3rd
                    _outputBuffer[_outputTail++] = (byte) ((i >> 3) & 0x7F);
                    if (len > 3) {
                        i = ((i & 0x07) << 8) | (data[offset++] & 0xFF); // 4th
                        _outputBuffer[_outputTail++] = (byte) ((i >> 4) & 0x7F);
                        if (len > 4) {
                            i = ((i & 0x0F) << 8) | (data[offset++] & 0xFF); // 5th
                            _outputBuffer[_outputTail++] = (byte) ((i >> 5) & 0x7F);
                            if (len > 5) {
                                i = ((i & 0x1F) << 8) | (data[offset++] & 0xFF); // 6th
                                _outputBuffer[_outputTail++] = (byte) ((i >> 6) & 0x7F);
                                _outputBuffer[_outputTail++] = (byte) (i & 0x3F); // last 6 bits
                            } else {
                                _outputBuffer[_outputTail++] = (byte) (i & 0x1F); // last 5 bits                                
                            }
                        } else {
                            _outputBuffer[_outputTail++] = (byte) (i & 0x0F); // last 4 bits
                        }
                    } else {
                        _outputBuffer[_outputTail++] = (byte) (i & 0x07); // last 3 bits                        
                    }
                } else {
                    _outputBuffer[_outputTail++] = (byte) (i & 0x03); // last 2 bits                    
                }
            } else {
                _outputBuffer[_outputTail++] = (byte) (i & 0x01); // last bit
            }
        }
    }
    
    /*
    /**********************************************************
    /* Internal methods, buffer handling
    /**********************************************************
     */
    
    @Override
    protected void _releaseBuffers()
    {
        byte[] buf = _outputBuffer;
        if (buf != null) {
            _outputBuffer = null;
            _ioContext.releaseWriteEncodingBuffer(buf);
        }
        char[] cbuf = _charBuffer;
        if (cbuf != null) {
            _charBuffer = null;
            _ioContext.releaseConcatBuffer(cbuf);
        }
    }

    protected final void _flushBuffer() throws IOException
    {
        if (_outputTail > 0) {
            _bytesWritten += _outputTail;
            _out.write(_outputBuffer, 0, _outputTail);
            _outputTail = 0;
        }
    }

    /*
    /**********************************************************
    /* Internal methods, handling shared string "maps"
    /**********************************************************
     */

    private final int _findSeenName(String name)
    {
        int hash = name.hashCode();
        SharedStringNode head = _seenNames[hash & (_seenNames.length-1)];
        if (head != null) {
            SharedStringNode node = head;
            // first, identity match; assuming most of the time we get intern()ed String
            do {
                if (node.value == name) {
                    return node.index;
                }
                node = node.next;
            } while (node != null);
            // and then comparison, if no match yet
            node = head;
            do {
                String value = node.value;
                if (value.hashCode() == hash && value.equals(name)) {
                    return node.index;
                }
                node = node.next;
            } while (node != null);
        }
        return -1;
    }
    
    private final void _addSeenName(String name)
    {
        // first: do we need to expand?
        if (_seenNameCount == _seenNames.length) {
            if (_seenNameCount == MAX_SHARED_NAMES) { // we are too full, restart from empty
                Arrays.fill(_seenNames, null);
                _seenNameCount = 0;
            } else { // we always start with modest default size (like 64), so expand to full
                SharedStringNode[] old = _seenNames;
                _seenNames = new SharedStringNode[MAX_SHARED_NAMES];
                final int mask = MAX_SHARED_NAMES-1;
                for (SharedStringNode node : old) {
                    for (; node != null; node = node.next) {
                        int ix = node.value.hashCode() & mask;
                        node.next = _seenNames[ix];
                        _seenNames[ix] = node;
                    }
                }
            }
        }
        // other than that, just slap it there
        int ix = name.hashCode() & (_seenNames.length-1);
        _seenNames[ix] = new SharedStringNode(name, _seenNameCount, _seenNames[ix]);
        ++_seenNameCount;
    }

    private final int _findSeenStringValue(String text)
    {
        int hash = text.hashCode();
        SharedStringNode head = _seenStringValues[hash & (_seenStringValues.length-1)];
        if (head != null) {
            SharedStringNode node = head;
            // first, identity match; assuming most of the time we get intern()ed String
            do {
                if (node.value == text) {
                    return node.index;
                }
                node = node.next;
            } while (node != null);
            // and then comparison, if no match yet
            node = head;
            do {
                String value = node.value;
                if (value.hashCode() == hash && value.equals(text)) {
                    return node.index;
                }
                node = node.next;
            } while (node != null);
        }
        return -1;
    }

    private final void _addSeenStringValue(String text)
    {
        // first: do we need to expand?
        if (_seenStringValueCount == _seenStringValues.length) {
            if (_seenStringValueCount == MAX_SHARED_STRING_VALUES) { // we are too full, restart from empty
                Arrays.fill(_seenStringValues, null);
                _seenStringValueCount = 0;
            } else { // we always start with modest default size (like 64), so expand to full
                SharedStringNode[] old = _seenStringValues;
                _seenStringValues = new SharedStringNode[MAX_SHARED_STRING_VALUES];
                final int mask = MAX_SHARED_STRING_VALUES-1;
                for (SharedStringNode node : old) {
                    for (; node != null; node = node.next) {
                        int ix = node.value.hashCode() & mask;
                        node.next = _seenStringValues[ix];
                        _seenStringValues[ix] = node;
                    }
                }
            }
        }
        // other than that, just slap it there
        int ix = text.hashCode() & (_seenStringValues.length-1);
        _seenStringValues[ix] = new SharedStringNode(text, _seenStringValueCount, _seenStringValues[ix]);
        ++_seenStringValueCount;
    }
    
    /*
    /**********************************************************
    /* Internal methods, error reporting
    /**********************************************************
     */

    /**
     * Method for accessing offset of the next byte within the whole output
     * stream that this generator has produced.
     */
    protected long outputOffset() {
        return _bytesWritten + _outputTail;
    }
    
    protected UnsupportedOperationException _notSupported() {
        return new UnsupportedOperationException();
    }    
}