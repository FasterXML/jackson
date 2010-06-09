package org.codehaus.jackson.xml;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.Base64Variant;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonStreamContext;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.impl.JsonGeneratorBase;
import org.codehaus.jackson.impl.JsonWriteContext;
import org.codehaus.jackson.io.IOContext;

/**
 * 
 * 
 * @since 1.6
 */
public class ToXmlGenerator
    extends JsonGeneratorBase
{
    /**
     * Enumeration that defines all togglable extra XML-specific features
     */
    public enum Feature {
    	DUMMY(false)
        ;

        final boolean _defaultState;
        final int _mask;
        
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

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link org.codehaus.jackson.smile.SmileGenerator.Feature}s
     * are enabled.
     */
    protected int _features;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public ToXmlGenerator(IOContext ctxt, int features, ObjectCodec codec)
    {
        super(features, codec);
        _ioContext = ctxt;
    }

    /*
    /**********************************************************
    /* Extended API, configuration
    /**********************************************************
     */

    public ToXmlGenerator enable(Feature f) {
        _features |= f.getMask();
        return this;
    }

    public ToXmlGenerator disable(Feature f) {
        _features &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_features & f.getMask()) != 0;
    }

    public ToXmlGenerator configure(Feature f, boolean state) {
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
    }
    
    @Override
    protected void _writeEndArray()
        throws IOException, JsonGenerationException
    {
    }

    @Override
    protected void _writeStartObject()
        throws IOException, JsonGenerationException
    {
    }

    @Override
    protected void _writeEndObject()
        throws IOException, JsonGenerationException
    {
    }

    @Override
    protected void _writeFieldName(String name, boolean commaBefore) throws IOException, JsonGenerationException
    {
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException
    {
    }    
    
    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException
    {
    }
    
    /*
    /**********************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException, JsonGenerationException {
        // !!! TBI
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException, JsonGenerationException {
        // !!! TBI
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        // !!! TBI
    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        // !!! TBI
    }

    @Override
    public void writeRawValue(String text) throws IOException, JsonGenerationException {
        // !!! TBI
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException, JsonGenerationException {
        // !!! TBI
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        // !!! TBI
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
        // !!! TBI
    }
    
    /*
    /**********************************************************
    /* Output method implementations, primitive
    /**********************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException
    {
        // !!! TBI
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException {
        // !!! TBI
    }

    @Override
    public void writeNumber(int i) throws IOException, JsonGenerationException
    {
        // !!! TBI
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException
    {
        // !!! TBI
    }

    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException
    {
        // !!! TBI
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException
    {
        // !!! TBI
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException, JsonGenerationException
    {
        if (dec == null) {
            writeNull();
            return;
        }
        // !!! TBI
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException
    {
        if (v == null) {
            writeNull();
            return;
        }
        // !!! TBI
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException,JsonGenerationException, UnsupportedOperationException
    {
//        throw _notSupported();
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
//        _out.flush();
    }

    @Override
    public void close()
        throws IOException
    {
//        boolean wasClosed = _closed;
        super.close();

        /* 05-Dec-2008, tatu: To add [JACKSON-27], need to close open
         *   scopes.
         */
        // First: let's see that we still have buffers...
        if (isEnabled(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)) {
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

        if (_ioContext.isResourceManaged() || isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET)) {
        	// !!! TBI
//            _out.close();
        } else {
            // If we can't close it, we should at least flush
        	// !!! TBI
//            _out.flush();
        }
        // Internal buffer(s) generator has can now be released as well
        _releaseBuffers();
    }

    /*
    /**********************************************************
    /* Internal methods, buffer handling
    /**********************************************************
    */
    
    @Override
    protected void _releaseBuffers()
    {
    	/*
        byte[] buf = _outputBuffer;
        if (buf != null) {
            _outputBuffer = null;
            _ioContext.releaseWriteEncodingBuffer(buf);
        }
        */
    }

    protected final void _flushBuffer() throws IOException
    {
    	/*
        if (_outputTail > 0) {
            _out.write(_outputBuffer, 0, _outputTail);
            _outputTail = 0;
        }
        */
    }
    
}
