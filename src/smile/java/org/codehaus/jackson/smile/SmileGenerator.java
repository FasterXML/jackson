package org.codehaus.jackson.smile;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
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
     * To simplify certain operations, we require output buffer length
     * to allow outputting of contiguous 256 character UTF-8 encoded String
     * value. Length of the longest UTF-8 codepoint (from Java char) is 3 bytes,
     * and we need both initial token byte and single-byte length indicators
     * so we get following value.
     */
    private final static int MIN_BUFFER_LENGTH = (3 * 256) + 2;

    private static long MIN_INT_AS_LONG = (long) Integer.MIN_VALUE;
    private static long MAX_INT_AS_LONG = (long) Integer.MAX_VALUE;

    private final static byte NULL_BYTE = 0;

    private final static byte TOKEN_BYTE_LONG_STRING =
        (byte) (TOKEN_PREFIX_VAR_LENGTH_MISC | TOKEN_MISC_SUBTYPE_LONG_TEXT);
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected IOContext _ioContext;

    final protected OutputStream _out;
    
    /*
    /**********************************************************
    /* Output buffering
    /**********************************************************
     */

    /**
     * Intermediate buffer in which contents are buffered before
     * being written using {@link #_writer}.
     */
    protected byte[] _outputBuffer;

    /**
     * Pointer to the next available byte in {@link #_outputBuffer}
     */
    protected int _outputTail = 0;

    /**
     * Offset to index after the last valid index in {@link _outputBuffer}.
     * Typically same as length of the buffer.
     */
    protected final int _outputEnd;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public SmileGenerator(IOContext ctxt, int features, ObjectCodec codec,
            OutputStream out)
    {
        super(features, codec);
        _ioContext = ctxt;
        _out = out;
        _outputBuffer = ctxt.allocWriteEncodingBuffer();
        _outputEnd = _outputBuffer.length;
        // let's just sanity check to prevent nasty odd errors
        if (_outputEnd < MIN_BUFFER_LENGTH) {
            throw new IllegalStateException("Internal encoding buffer length ("+_outputEnd
                    +") too short, must be at least "+MIN_BUFFER_LENGTH);
        }
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

    /*
    public final static int TOKEN_LITERAL_START_OBJECT = 0x20;
    // NOTE: NO END_OBJECT, since that is contextual! Slot reserved however.
    public final static int TOKEN_LITERAL_TRUE = 0x24;
    public final static int TOKEN_LITERAL_FALSE = 0x25;
    public final static int TOKEN_LITERAL_NULL = 0x26;
    public final static int TOKEN_LITERAL_EMPTY_STRING = 0x27;
     */
    
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
        // TODO Auto-generated method stub
        
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException
    {
        int len = text.length();
        if (len == 0) {
            _writeByte(TOKEN_LITERAL_EMPTY_STRING);
            return;
        }
        if (len <= MAX_SHORT_STRING_BYTES) { // possibly short strings (not necessarily)
            // !!! TODO: check for shared Strings
            // first: ensure we have enough space
            if ((_outputTail + MIN_BUFFER_FOR_POSSIBLE_SHORT_STRING) >= _outputEnd) {
                _flushBuffer();
            }
            int byteLen = _fastUTF8Encode(text, 0, len);
            int origOffset = _outputTail;
            byte typeToken;
            if (byteLen <= MAX_SHORT_STRING_BYTES) { // yes, is short indeed
                if (byteLen == len) { // and all ASCII
                    typeToken = (byte) (TOKEN_PREFIX_TINY_ASCII | byteLen);
                } else { // not just ASCII
                    typeToken = (byte) (TOKEN_PREFIX_TINY_UNICODE | byteLen);
                }
            } else { // nope, longer non-ascii Strings
                typeToken = TOKEN_BYTE_LONG_STRING;
                // and we will need null byte as end marker
                _outputBuffer[_outputTail++] = NULL_BYTE;
            }
            // and then sneak in type token now that know the details
            _outputBuffer[origOffset] = typeToken;
        } else { // "long" String, never shared
            _writeByte(TOKEN_BYTE_LONG_STRING);
            // but might still fit within buffer?
            int maxLen = len + len + len + 1;
            if (maxLen <= _outputBuffer.length) { // yes indeed
                if ((_outputTail + maxLen) >= _outputEnd) {
                    _flushBuffer();
                }
                _fastUTF8Encode(text, 0, len);
                _outputBuffer[_outputTail++] = NULL_BYTE;                
            } else {
                _slowUTF8Encode(text, 0, len);
                _writeByte(NULL_BYTE);
            }
        }
    }
    
    /**
     * Helper method used to encode Strings that are short enough that UTF-8
     * encoded version is known to fit in the buffer
     */
    private int _fastUTF8Encode(String str, int ptr, int end)
    {
        return 0;
    }

    private void _slowUTF8Encode(String str, int ptr, int end)
    {
    }
    
    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException
    {
        if (len == 0) {
            _writeByte(TOKEN_LITERAL_EMPTY_STRING);
            return;
        }
        // !!! TBI
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
        int lenlen = _determineLengthIndicator(len);
        // !!! TODO: handle compression
        int compType = TOKEN_COMP_TYPE_NONE;
        byte tokenType = (byte) (TOKEN_PREFIX_VAR_LENGTH_MISC | TOKEN_MISC_BIT_BINARY | (lenlen << 2) | compType);
        switch (lenlen) {
        case TOKEN_LENGTH_IND_8B:
            _writeBytes(tokenType, (byte) len);
            break;
        case TOKEN_LENGTH_IND_16B:
            _writeTaggedShort(tokenType, (short) len);
            break;
        default:
            _writeTaggedInt(tokenType, len);
            break;
        }
        _writeBytes(data, offset, len);
    }

    /*
    /**********************************************************
    /* Output method implementations, primitive
    /**********************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException
    {
        if (state) {
            _writeByte(TOKEN_LITERAL_TRUE);            
        } else {
            _writeByte(TOKEN_LITERAL_FALSE);             
        }
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException {
        _writeByte(TOKEN_LITERAL_NULL);
    }

    @Override
    public void writeNumber(int i) throws IOException, JsonGenerationException
    {
        if (i > 0xF) { // not small, at least 8-bit
            if ((i >> 7) == 0) { // 8 bits is enough
                _writeBytes(TOKEN_NUMBER_BYTE, (byte) i);
            } else if ((i >> 15) == 0) { // 16 bits is enough
                _writeTaggedShort(TOKEN_NUMBER_SHORT, i);
            } else  { // need full 32 bits
                _writeTaggedInt(TOKEN_NUMBER_INT, i);
            }
            return;
        }
        // small or negative
        if (i >= -16) { // small int
            _writeByte((byte) (TOKEN_PREFIX_SMALL_INT + i));
        } else if ((i >> 7) == -1) {
            _writeBytes(TOKEN_NUMBER_BYTE, (byte) i);
        } else if ((i >> 15) == -1) { // 16 bit?
            _writeTaggedShort(TOKEN_NUMBER_SHORT, i);
        } else { // need full 32 bits
            _writeTaggedInt(TOKEN_NUMBER_INT, i);            
        }
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException
    {
        // First: maybe 32 bits is enough?
        long hi = (l >> 32);
        if (hi == 0L || hi == -1L) {
            writeNumber((int) l);
            return;
        }
        _writeTaggedInt(TOKEN_NUMBER_LONG, (int) hi);
        _writeInt((int) l);
    }

    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException
    {
        /* 17-Apr-2010, tatu: could also use 'doubleToIntBits', but it seems more accurate to use
         * exact representation; and possibly faster. However, if there are cases
         * where collapsing of NaN was needed (for non-Java clients), this can
         * be changed
         */
        long l = Double.doubleToRawLongBits(d);
        _writeTaggedInt(TOKEN_NUMBER_FLOAT, (int) (l >> 32));
        _writeInt((int) l);
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException
    {
        /* 17-Apr-2010, tatu: could also use 'floatToIntBits', but it seems more accurate to use
         * exact representation; and possibly faster. However, if there are cases
         * where collapsing of NaN was needed (for non-Java clients), this can
         * be changed
         */
        _writeTaggedInt(TOKEN_NUMBER_FLOAT, Float.floatToRawIntBits(f));
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException, JsonGenerationException
    {
        if (dec == null) {
            writeNull();
            return;
        }
        int scale = dec.scale();
        BigInteger unscaled = dec.unscaledValue();
        byte[] data = unscaled.toByteArray();
        int len = data.length;
        // only have a single length-indicator; need to be big enough for both scale and array length
        int lenlen = _determineLengthIndicator(Math.max(scale, len));
        byte tokenType = (byte) (TOKEN_PREFIX_VAR_LENGTH_MISC | TOKEN_MISC_SUBTYPE_BIG_DECIMAL | lenlen);
 
        // Ok then scale (byte/short/int) and BigInteger serialization (byte/short/int for length; n bytes)
        switch (lenlen) {
        case TOKEN_LENGTH_IND_8B:
            _writeBytes(tokenType, (byte) scale);
            _writeByte((byte) len);
            break;
        case TOKEN_LENGTH_IND_16B:
            _writeTaggedShort(tokenType, (short) scale);
            _writeShort(len);
            break;
        default:
            _writeTaggedInt(tokenType, scale);
            _writeInt(len);
            break;
        }
        _writeBytes(data);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException
    {
        if (v == null) {
            writeNull();
            return;
        }
        byte[] data = v.toByteArray();
        int len = data.length;
        int lenlen = _determineLengthIndicator(len);
        byte tokenType = (byte) (TOKEN_PREFIX_VAR_LENGTH_MISC | TOKEN_MISC_SUBTYPE_BIG_INTEGER | lenlen);
        switch (lenlen) {
        case TOKEN_LENGTH_IND_8B:
            _writeBytes(tokenType, (byte) len);
            break;
        case TOKEN_LENGTH_IND_16B:
            _writeTaggedShort(tokenType, (short) len);
            break;
        default:
            _writeTaggedInt(tokenType, len);
            break;
        }
        _writeBytes(data);
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
    public void close()
        throws IOException
    {
        super.close();

        /* 05-Dec-2008, tatu: To add [JACKSON-27], need to close open
         *   scopes.
         */
        // First: let's see that we still have buffers...
        if (_outputBuffer != null
            && isEnabled(Feature.AUTO_CLOSE_JSON_CONTENT)) {
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
        _flushBuffer();

        if (_ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_TARGET)) {
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
    /* Internal methods, writing bytes
    /**********************************************************
    */

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

    private final void _writeBytes(byte[] data) throws IOException
    {
        _writeBytes(data, 0, data.length);
    }

    private final void _writeBytes(byte[] data, int offset, int len) throws IOException
    {
        if (len > 0) {
            while (true) {
                int currLen = Math.min(len, (_outputTail - _outputEnd));
                System.arraycopy(data, offset, _outputBuffer, _outputTail, currLen);
                if ((len -= currLen) == 0) {
                    break;
                }
                _flushBuffer();
                offset += currLen;
            }
        }
    }

    private final void _writeShort(int i) throws IOException
    {
        if ((_outputTail + 2) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = (byte) (i >> 8);
        _outputBuffer[_outputTail++] = (byte) i;
    }
    
    private final void _writeTaggedShort(byte b, int i) throws IOException
    {
        if ((_outputTail + 3) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b;
        _outputBuffer[_outputTail++] = (byte) (i >> 8);
        _outputBuffer[_outputTail++] = (byte) i;
    }

    private final void _writeInt(int i) throws IOException
    {
        if ((_outputTail + 4) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = (byte) (i >> 24);
        _outputBuffer[_outputTail++] = (byte) (i >> 16);
        _outputBuffer[_outputTail++] = (byte) (i >> 8);
        _outputBuffer[_outputTail++] = (byte) i;
    }
    
    private final void _writeTaggedInt(byte b, int i) throws IOException
    {
        if ((_outputTail + 5) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b;
        _outputBuffer[_outputTail++] = (byte) (i >> 24);
        _outputBuffer[_outputTail++] = (byte) (i >> 16);
        _outputBuffer[_outputTail++] = (byte) (i >> 8);
        _outputBuffer[_outputTail++] = (byte) i;
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
    }

    protected final void _flushBuffer() throws IOException
    {
        if (_outputTail > 0) {
            _out.write(_outputBuffer, 0, _outputTail);
            _outputTail = 0;
        }
    }

    /*
    /**********************************************************
    /* Internal methods, calculating lengths
    /**********************************************************
    */
    
    /**
     * Method used to figure out how many bytes are needed to represent given
     * length; either 1, 2 or 4 bytes (since argument itself is a 32-bit int)
     */
    protected int _determineLengthIndicator(int length)
    {
        if ((length >> 8) == 0) {
            return TOKEN_LENGTH_IND_8B;
        }
        if ((length >> 16) == 0) {
            return TOKEN_LENGTH_IND_16B;
        }
        // with int, can not go past 32 bits
        return TOKEN_LENGTH_IND_32B;
    }
    
    
    /*
    /**********************************************************
    /* Internal methods, error reporting
    /**********************************************************
    */
    
    protected UnsupportedOperationException _notSupported() {
        return new UnsupportedOperationException();
    }    
}