package org.codehaus.jackson.smile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.impl.JsonReadContext;
import org.codehaus.jackson.io.IOContext;

public class SmileParser extends JsonParser
{
    /*
    /**********************************************************
    /* Generic I/O state
    /**********************************************************
     */

    /**
     * I/O context for this reader. It handles buffer allocation
     * for the reader.
     */
    final protected IOContext _ioContext;

    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;

    /**
     * Input stream that can be used for reading more content, if one
     * in use. May be null, if input comes just as a full buffer,
     * or if the stream has been closed.
     */
    protected InputStream _inputStream;

    /*
    /**********************************************************
    /* Current input data
    /**********************************************************
     */

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source, but in some cases pre-loaded buffer
     * is handed to the parser.
     */
    protected byte[] _inputBuffer;

    /**
     * Flag that indicates whether the input buffer is recycable (and
     * needs to be returned to recycler once we are done) or not.
     */
    protected boolean _bufferRecyclable;

    /**
     * Pointer to next available character in buffer
     */
    protected int _inputPtr = 0;

    /**
     * Index of character after last available one in the buffer.
     */
    protected int _inputEnd = 0;

    /*
    /**********************************************************
    /* Other configuration
    /**********************************************************
     */
    
    protected ObjectCodec _objectCodec;
    
    /*
    /**********************************************************
    /* Parsing state
    /**********************************************************
     */

    /**
     * Information about parser context, context in which
     * the next token is to be parsed (root, array, object).
     */
    protected JsonReadContext _parsingContext;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public SmileParser(IOContext ctxt) {
        _ioContext = ctxt;
    }

    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }
    
    @Override
    public void close() throws IOException
    {
        _closed = true;
        // _closeInput()
        if (_inputStream != null) {
            if (_ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_SOURCE)) {
                _inputStream.close();
            }
            _inputStream = null;
        }
        //_releaseBuffers();
        if (_bufferRecyclable) {
            byte[] buf = _inputBuffer;
            if (buf != null) {
                _inputBuffer = null;
                _ioContext.releaseReadIOBuffer(buf);
            }
        }
    }
    
    public boolean isClosed() { return _closed; }
    
    /*
    /**********************************************************
    /* JsonParser impl
    /**********************************************************
     */

    @Override
    public JsonToken nextToken() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonParser skipChildren() throws IOException, JsonParseException
    {
        if (_currToken != JsonToken.START_OBJECT
                && _currToken != JsonToken.START_ARRAY) {
                return this;
            }
            int open = 1;

            /* Since proper matching of start/end markers is handled
             * by nextToken(), we'll just count nesting levels here
             */
            while (true) {
                JsonToken t = nextToken();
                if (t == null) {
                    _handleEOF();
                    /* given constraints, above should never return;
                     * however, FindBugs doesn't know about it and
                     * complains... so let's add dummy break here
                     */
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

    public String getCurrentName() throws IOException, JsonParseException
    {
        return _parsingContext.getCurrentName();
    }    
    
    public JsonReadContext getParsingContext()
    {
        return _parsingContext;
    }
    
    /**
     * Method that return the <b>starting</b> location of the current
     * token; that is, position of the first character from input
     * that starts the current token.
     */
    public JsonLocation getTokenLocation()
    {
        return JsonLocation.NA;
    }
    
    /**
     * Method that returns location of the last processed character;
     * usually for error reporting purposes
     */
    public JsonLocation getCurrentLocation()
    {
        return JsonLocation.NA;
    }
        
    /*
    /**********************************************************
    /* Public API, access to token information, text
    /**********************************************************
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
    /**********************************************************
    /* Public API, access to token information, binary
    /**********************************************************
     */

    @Override
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException,
            JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
    /**********************************************************
    /* Numeric accessors
    /**********************************************************
     */

    @Override
    public BigInteger getBigIntegerValue() throws IOException,
            JsonParseException {
        // TODO Auto-generated method stub
        return null;
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


    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    /**
     * Method called when an EOF is encountered between tokens.
     * If so, it may be a legitimate EOF, but only iff there
     * is no open non-root context.
     */
    protected void _handleEOF()
        throws JsonParseException
    {
        if (!_parsingContext.inRoot()) {
            _reportInvalidEOF(": expected close marker for "+_parsingContext.getTypeDesc()+" (from "+_parsingContext.getStartLocation(_ioContext.getSourceReference())+")");
        }
    }

    protected void _reportInvalidEOF()
        throws JsonParseException
    {
        _reportInvalidEOF(" in "+_currToken);
    }
    
    protected void _reportInvalidEOF(String msg)
        throws JsonParseException
    {
        _reportError("Unexpected end-of-input"+msg);
    }    

    protected final void _reportError(String msg)
        throws JsonParseException
    {
        throw _constructError(msg);
    }
    
    protected final void _throwInternal()
    {
        throw new RuntimeException("Internal error: this code path should never get executed");
    }
    
    protected final JsonParseException _constructError(String msg, Throwable t)
    {
        return new JsonParseException(msg, getCurrentLocation(), t);
    }

}
