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
        _writeByte(TOKEN_NAME_LITERAL_END_OBJECT);
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
        if (text.length() == 0) {
            _writeByte(TOKEN_LITERAL_EMPTY_STRING);
            return;
        }
        // !!! TBI
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
        // TODO Auto-generated method stub
        
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
                _writeBytes(TOKEN_NUMBER_8B_INT, (byte) i);
            } else if ((i >> 15) == 0) { // 16 bits is enough
                _writeTaggedShort(TOKEN_NUMBER_16B_INT, i);
            } else  { // need full 32 bits
                _writeTaggedInt(TOKEN_NUMBER_32B_INT, i);
            }
            return;
        }
        // small or negative
        if (i >= -16) { // small int
            _writeByte((byte) (TOKEN_PREFIX_SMALL_INT + i));
        } else if ((i >> 7) == -1) {
            _writeBytes(TOKEN_NUMBER_8B_INT, (byte) i);
        } else if ((i >> 15) == -1) { // 16 bit?
            _writeTaggedShort(TOKEN_NUMBER_16B_INT, i);
        } else { // need full 32 bits
            _writeTaggedInt(TOKEN_NUMBER_32B_INT, i);            
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
        _writeTaggedInt(TOKEN_NUMBER_64B_INT, (int) hi);
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
        // TODO Auto-generated method stub
        
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub
        
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
        if (_outputTail >= _outputBuffer.length) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b;
    }

    private final void _writeBytes(byte b1, byte b2) throws IOException
    {
        if ((_outputTail + 1) >= _outputBuffer.length) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b1;
        _outputBuffer[_outputTail++] = b2;
    }

    private final void _writeTaggedShort(byte b, int i) throws IOException
    {
        if ((_outputTail + 3) >= _outputBuffer.length) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b;
        _outputBuffer[_outputTail++] = (byte) (i >> 8);
        _outputBuffer[_outputTail++] = (byte) i;
    }

    private final void _writeInt(int i) throws IOException
    {
        if ((_outputTail + 4) >= _outputBuffer.length) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = (byte) (i >> 24);
        _outputBuffer[_outputTail++] = (byte) (i >> 16);
        _outputBuffer[_outputTail++] = (byte) (i >> 8);
        _outputBuffer[_outputTail++] = (byte) i;
    }
    
    private final void _writeTaggedInt(byte b, int i) throws IOException
    {
        if ((_outputTail + 5) >= _outputBuffer.length) {
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
    /* Internal methods, error reporting
    /**********************************************************
    */
    
    protected UnsupportedOperationException _notSupported() {
        return new UnsupportedOperationException();
    }    
}