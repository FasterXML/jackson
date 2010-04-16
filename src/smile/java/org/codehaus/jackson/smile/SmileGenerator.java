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
    public void writeNumber(int i) throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void writeNumber(double d) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void writeNumber(float f) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException,
            JsonGenerationException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException,
            JsonGenerationException, UnsupportedOperationException {
        // TODO Auto-generated method stub
        
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
    /* Internal methods
    /**********************************************************
    */

    private final void _writeByte(byte b) throws IOException
    {
        if (_outputTail >= _outputBuffer.length) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = b;
    }
    
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

    protected UnsupportedOperationException _notSupported() {
        return new UnsupportedOperationException();
    }    
}