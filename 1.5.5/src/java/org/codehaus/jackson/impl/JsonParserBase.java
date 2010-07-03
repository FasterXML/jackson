package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.util.ByteArrayBuilder;
import org.codehaus.jackson.util.TextBuffer;

/**
 * Intermediate base class used by all Jackson {@link JsonParser}
 * implementations. Contains most common things that are independent
 * of actual underlying input source
 *
 * @author Tatu Saloranta
 */
public abstract class JsonParserBase
    extends JsonParser
{
    // Control chars:
    final static int INT_TAB = '\t';
    final static int INT_LF = '\n';
    final static int INT_CR = '\r';
    final static int INT_SPACE = 0x0020;

    // Markup
    final static int INT_LBRACKET = '[';
    final static int INT_RBRACKET = ']';
    final static int INT_LCURLY = '{';
    final static int INT_RCURLY = '}';
    final static int INT_QUOTE = '"';
    final static int INT_BACKSLASH = '\\';
    final static int INT_SLASH = '/';
    final static int INT_COLON = ':';
    final static int INT_COMMA = ',';
    final static int INT_ASTERISK = '*';
    final static int INT_APOSTROPHE = '\'';

    // Letters we need
    final static int INT_b = 'b';
    final static int INT_f = 'f';
    final static int INT_n = 'n';
    final static int INT_r = 'r';
    final static int INT_t = 't';
    final static int INT_u = 'u';

    /*
    ////////////////////////////////////////////////////
    // Generic I/O state
    ////////////////////////////////////////////////////
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

    /*
    ////////////////////////////////////////////////////
    // Current input data
    ////////////////////////////////////////////////////
     */

    // Note: type of actual buffer depends on sub-class, can't include

    /**
     * Pointer to next available character in buffer
     */
    protected int _inputPtr = 0;

    /**
     * Index of character after last available one in the buffer.
     */
    protected int _inputEnd = 0;

    /*
    ////////////////////////////////////////////////////
    // Current input location information
    ////////////////////////////////////////////////////
     */

    /**
     * Number of characters that were contained in previous blocks
     * (blocks that were already processed prior to the current buffer).
     */
    protected long _currInputProcessed = 0L;

    /**
     * Current row location of current point in input buffer, starting
     * from 1
     */
    protected int _currInputRow = 1;

    /**
     * Current index of the first character of the current row in input
     * buffer. Needed to calculate column position, if necessary; benefit
     * of not having column itself is that this only has to be updated
     * once per line.
     */
    protected int _currInputRowStart = 0;

    /*
    ////////////////////////////////////////////////////
    // Information about starting location of event
    // Reader is pointing to; updated on-demand
    ////////////////////////////////////////////////////
     */

    // // // Location info at point when current token was started

    /**
     * Total number of characters read before start of current token.
     * For big (gigabyte-sized) sizes are possible, needs to be long,
     * unlike pointers and sizes related to in-memory buffers.
     */
    protected long _tokenInputTotal = 0; 

    /**
     * Input row on which current token starts, 1-based
     */
    protected int _tokenInputRow = 1;

    /**
     * Column on input row that current token starts; 0-based (although
     * in the end it'll be converted to 1-based)
     */
    protected int _tokenInputCol = 0;

    /*
    ////////////////////////////////////////////////////
    // Parsing state
    ////////////////////////////////////////////////////
     */

    /**
     * Information about parser context, context in which
     * the next token is to be parsed (root, array, object).
     */
    protected JsonReadContext _parsingContext;

    /**
     * Secondary token related to the current token: used when
     * the current token is <code>FIELD_NAME</code> but the
     * actual value token is also known.
     */
    protected JsonToken _nextToken;

    /**
     * Flag that indicates that the current token has not yet
     * been fully processed, and needs to be finished for
     * some access (or skipped to obtain the next token)
     */
    protected boolean _tokenIncomplete = false;

    /*
    ////////////////////////////////////////////////////
    // Buffer(s) for local name(s) and text content
    ////////////////////////////////////////////////////
     */

    /**
     * Buffer that contains contents of String values, including
     * field names if necessary (name split across boundary,
     * contains escape sequence, or access needed to char array)
     */
    protected final TextBuffer _textBuffer;

    /**
     * Temporary buffer that is needed if field name is accessed
     * using {@link #getTextCharacters} method (instead of String
     * returning alternatives)
     */
    protected char[] _nameCopyBuffer = null;

    /**
     * Flag set to indicate whether the field name is available
     * from the name copy buffer or not (in addition to its String
     * representation  being available via read context)
     */
    protected boolean _nameCopied = false;

    /**
     * ByteArrayBuilder is needed if 'getBinaryValue' is called. If so,
     * we better reuse it for remainder of content.
     */
    ByteArrayBuilder _byteArrayBuilder = null;

    /**
     * We will hold on to decoded binary data, for duration of
     * current event, so that multiple calls to
     * {@link #getBinaryValue} will not need to decode data more
     * than once.
     */
    protected byte[] _binaryValue;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected JsonParserBase(IOContext ctxt, int features)
    {
        _ioContext = ctxt;
        _features = features;
        _textBuffer = ctxt.constructTextBuffer();
        _parsingContext = JsonReadContext.createRootContext(_tokenInputRow, _tokenInputCol);
    }

    /*
    ////////////////////////////////////////////////////
    // Configuration overrides if any
    ////////////////////////////////////////////////////
     */

    // from base class:

    //public void enableFeature(Feature f)
    //public void disableFeature(Feature f)
    //public void setFeature(Feature f, boolean state)
    //public boolean isFeatureEnabled(Feature f)

    /*
    ////////////////////////////////////////////////////
    // Abstract methods needed from sub-classes
    ////////////////////////////////////////////////////
     */

    protected abstract void _finishString() throws IOException, JsonParseException;

    /*
    ////////////////////////////////////////////////////
    // JsonParser impl
    ////////////////////////////////////////////////////
     */

    public abstract JsonToken nextToken()
        throws IOException, JsonParseException;

    //public final JsonToken nextValue()

    public JsonParser skipChildren()
        throws IOException, JsonParseException
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

    //public JsonToken getCurrentToken()

    //public boolean hasCurrentToken()

    /**
     * Method that can be called to get the name associated with
     * the current event.
     */
    public String getCurrentName()
        throws IOException, JsonParseException
    {
        return _parsingContext.getCurrentName();
    }

    public void close() throws IOException
    {
        _closed = true;
        _closeInput();
        // Also, internal buffer(s) can now be released as well
        _releaseBuffers();
    }

    public boolean isClosed() { return _closed; }

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
        return new JsonLocation(_ioContext.getSourceReference(),
                                getTokenCharacterOffset(),
                                getTokenLineNr(),
                                getTokenColumnNr());
    }

    /**
     * Method that returns location of the last processed character;
     * usually for error reporting purposes
     */
    public JsonLocation getCurrentLocation()
    {
        int col = _inputPtr - _currInputRowStart + 1; // 1-based
        return new JsonLocation(_ioContext.getSourceReference(),
                                _currInputProcessed + _inputPtr - 1,
                                _currInputRow, col);
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, access to token information, text
    ////////////////////////////////////////////////////
     */

    /**
     * Method for accessing textual representation of the current event;
     * if no current event (before first call to {@link #nextToken}, or
     * after encountering end-of-input), returns null.
     * Method can be called for any event.
     */
    public String getText()
        throws IOException, JsonParseException
    {
        if (_currToken != null) { // null only before/after document
            switch (_currToken) {
            case FIELD_NAME:
                return _parsingContext.getCurrentName();

            case VALUE_STRING:
                if (_tokenIncomplete) {
                    _tokenIncomplete = false;
                    _finishString(); // only strings can be incomplete
                }
                // fall through
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return _textBuffer.contentsAsString();
                
            default:
                return _currToken.asString();
            }
        }
        return null;
    }

    public char[] getTextCharacters()
        throws IOException, JsonParseException
    {
        if (_currToken != null) { // null only before/after document
            switch (_currToken) {
                
            case FIELD_NAME:
                if (!_nameCopied) {
                    String name = _parsingContext.getCurrentName();
                    int nameLen = name.length();
                    if (_nameCopyBuffer == null) {
                        _nameCopyBuffer = _ioContext.allocNameCopyBuffer(nameLen);
                    } else if (_nameCopyBuffer.length < nameLen) {
                        _nameCopyBuffer = new char[nameLen];
                    }
                    name.getChars(0, nameLen, _nameCopyBuffer, 0);
                    _nameCopied = true;
                }
                return _nameCopyBuffer;

            case VALUE_STRING:
                if (_tokenIncomplete) {
                    _tokenIncomplete = false;
                    _finishString(); // only strings can be incomplete
                }
                // fall through
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return _textBuffer.getTextBuffer();
                
            default:
                return _currToken.asCharArray();
            }
        }
        return null;
    }

    public int getTextLength()
        throws IOException, JsonParseException
    {
        if (_currToken != null) { // null only before/after document
            switch (_currToken) {
                
            case FIELD_NAME:
                return _parsingContext.getCurrentName().length();
            case VALUE_STRING:
                if (_tokenIncomplete) {
                    _tokenIncomplete = false;
                    _finishString(); // only strings can be incomplete
                }
                // fall through
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return _textBuffer.size();
                
            default:
                return _currToken.asCharArray().length;
            }
        }
        return 0;
    }

    public int getTextOffset()
        throws IOException, JsonParseException
    {
        // Most have offset of 0, only some may have other values:
        if (_currToken != null) {
            switch (_currToken) {
            case FIELD_NAME:
                return 0;
            case VALUE_STRING:
                if (_tokenIncomplete) {
                    _tokenIncomplete = false;
                    _finishString(); // only strings can be incomplete
                }
                // fall through
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return _textBuffer.getTextOffset();
            }
        }
        return 0;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, access to token information, binary
    ////////////////////////////////////////////////////
     */

    public final byte[] getBinaryValue(Base64Variant b64variant)
        throws IOException, JsonParseException
    {
        if (_currToken != JsonToken.VALUE_STRING) {
            _reportError("Current token ("+_currToken+") not VALUE_STRING, can not access as binary");
        }
        /* To ensure that we won't see inconsistent data, better clear up
         * state...
         */
        if (_tokenIncomplete) {
            try {
                _binaryValue = _decodeBase64(b64variant);
            } catch (IllegalArgumentException iae) {
                throw _constructError("Failed to decode VALUE_STRING as base64 ("+b64variant+"): "+iae.getMessage());
            }
            /* let's clear incomplete only now; allows for accessing other
             * textual content in error cases
             */
            _tokenIncomplete = false;
        }
        return _binaryValue;
    }

    protected abstract byte[] _decodeBase64(Base64Variant b64variant)
        throws IOException, JsonParseException;

    /*
    ////////////////////////////////////////////////////
    // Public low-level accessors
    ////////////////////////////////////////////////////
     */

    public final long getTokenCharacterOffset() { return _tokenInputTotal; }
    public final int getTokenLineNr() { return _tokenInputRow; }
    public final int getTokenColumnNr() { return _tokenInputCol+1; }

    /*
    ////////////////////////////////////////////////////
    // Low-level reading, other
    ////////////////////////////////////////////////////
     */

    protected abstract boolean loadMore() throws IOException;

    protected final void loadMoreGuaranteed()
        throws IOException
    {
        if (!loadMore()) {
            _reportInvalidEOF();
        }
    }

    protected abstract void _closeInput()
        throws IOException;

    /**
     * Method called to release internal buffers owned by the base
     * reader. This may be called along with {@link #_closeInput} (for
     * example, when explicitly closing this reader instance), or
     * separately (if need be).
     */
    protected void _releaseBuffers() throws IOException
    {
        _textBuffer.releaseBuffers();
        char[] buf = _nameCopyBuffer;
        if (buf != null) {
            _nameCopyBuffer = null;
            _ioContext.releaseNameCopyBuffer(buf);
        }
    }

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

    /*
    ////////////////////////////////////////////////////
    // Error reporting
    ////////////////////////////////////////////////////
     */

    protected void _reportUnexpectedChar(int ch, String comment)
        throws JsonParseException
    {
        String msg = "Unexpected character ("+_getCharDesc(ch)+")";
        if (comment != null) {
            msg += ": "+comment;
        }
        _reportError(msg);
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

    protected void _throwInvalidSpace(int i)
        throws JsonParseException
    {
        char c = (char) i;
        String msg = "Illegal character ("+_getCharDesc(c)+"): only regular white space (\\r, \\n, \\t) is allowed between tokens";
        _reportError(msg);
    }

    /**
     * Method called to report a problem with unquoted control character.
     * Note: starting with version 1.4, it is possible to suppress
     * exception by enabling {@link Feature.ALLOW_UNQUOTED_CONTROL_CHARS}.
     */
    protected void _throwUnquotedSpace(int i, String ctxtDesc)
        throws JsonParseException
    {
        // JACKSON-208; possible to allow unquoted control chars:
        if (!isEnabled(Feature.ALLOW_UNQUOTED_CONTROL_CHARS) || i >= INT_SPACE) {
            char c = (char) i;
            String msg = "Illegal unquoted character ("+_getCharDesc(c)+"): has to be escaped using backslash to be included in "+ctxtDesc;
            _reportError(msg);
        }
    }

    protected void _reportMismatchedEndMarker(int actCh, char expCh)
        throws JsonParseException
    {
        String startDesc = ""+_parsingContext.getStartLocation(_ioContext.getSourceReference());
        _reportError("Unexpected close marker '"+((char) actCh)+"': expected '"+expCh+"' (for "+_parsingContext.getTypeDesc()+" starting at "+startDesc+")");
    }

    /*
    ////////////////////////////////////////////////////
    // Error reporting, generic
    ////////////////////////////////////////////////////
     */

    protected final static String _getCharDesc(int ch)
    {
        char c = (char) ch;
        if (Character.isISOControl(c)) {
            return "(CTRL-CHAR, code "+ch+")";
        }
        if (ch > 255) {
            return "'"+c+"' (code "+ch+" / 0x"+Integer.toHexString(ch)+")";
        }
        return "'"+c+"' (code "+ch+")";
    }

    protected final void _reportError(String msg)
        throws JsonParseException
    {
        throw _constructError(msg);
    }

    protected final void _wrapError(String msg, Throwable t)
        throws JsonParseException
    {
        throw _constructError(msg, t);
    }

    protected final void _throwInternal()
    {
        throw new RuntimeException("Internal error: this code path should never get executed");
    }

    protected final JsonParseException _constructError(String msg, Throwable t)
    {
        return new JsonParseException(msg, getCurrentLocation(), t);
    }

    /*
    ////////////////////////////////////////////////////
    // Other helper methods for sub-classes
    ////////////////////////////////////////////////////
     */

    public ByteArrayBuilder _getByteArrayBuilder()
    {
        if (_byteArrayBuilder == null) {
            _byteArrayBuilder = new ByteArrayBuilder();
        } else {
            _byteArrayBuilder.reset();
        }
        return _byteArrayBuilder;
    }
}
