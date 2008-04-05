package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
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

    // Letters we need
    final static int INT_b = 'b';
    final static int INT_f = 'f';
    final static int INT_n = 'n';
    final static int INT_r = 'r';
    final static int INT_t = 't';
    final static int INT_u = 'u';

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    /**
     * I/O context for this reader. It handles buffer allocation
     * for the reader.
     */
    final protected IOContext mIOContext;

    /*
    ////////////////////////////////////////////////////
    // Current input data
    ////////////////////////////////////////////////////
     */

    protected JsonToken mCurrToken;

    // Note: type of actual buffer depends on sub-class, can't include

    /**
     * Pointer to next available character in buffer
     */
    protected int mInputPtr = 0;

    /**
     * Index of character after last available one in the buffer.
     */
    protected int mInputLast = 0;

    /*
    ////////////////////////////////////////////////////
    // Current input location information
    ////////////////////////////////////////////////////
     */

    /**
     * Number of characters that were contained in previous blocks
     * (blocks that were already processed prior to the current buffer).
     */
    protected long mCurrInputProcessed = 0L;

    /**
     * Current row location of current point in input buffer, starting
     * from 1
     */
    protected int mCurrInputRow = 1;

    /**
     * Current index of the first character of the current row in input
     * buffer. Needed to calculate column position, if necessary; benefit
     * of not having column itself is that this only has to be updated
     * once per line.
     */
    protected int mCurrInputRowStart = 0;

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
    protected long mTokenInputTotal = 0; 

    /**
     * Input row on which current token starts, 1-based
     */
    protected int mTokenInputRow = 1;

    /**
     * Column on input row that current token starts; 0-based (although
     * in the end it'll be converted to 1-based)
     */
    protected int mTokenInputCol = 0;

    /*
    ////////////////////////////////////////////////////
    // Parsing state
    ////////////////////////////////////////////////////
     */

    protected JsonReadContext mParsingContext;

    /**
     * Flag that indicates that the current token has not yet
     * been fully processed, and needs to be finished for
     * some access (or skipped to obtain the next token)
     */
    protected boolean mTokenIncomplete = false;

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
    protected final TextBuffer mTextBuffer;

    /**
     * Flag set to indicate whether field name parsed is available
     * from the text buffer or not.
     */
    protected boolean mFieldInBuffer = false;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected JsonParserBase(IOContext ctxt)
    {
        mIOContext = ctxt;
        mTextBuffer = ctxt.constructTextBuffer();
    }

    /*
    ////////////////////////////////////////////////////
    // Abstract methods needed from sub-classes
    ////////////////////////////////////////////////////
     */

    protected abstract void finishToken()
        throws IOException, JsonParseException;

    /*
    ////////////////////////////////////////////////////
    // JsonParser impl
    ////////////////////////////////////////////////////
     */

    public abstract JsonToken nextToken()
        throws IOException, JsonParseException;


    /**
     * @return Type of the token this parser currently points to,
     *   if any: null both before any tokens have been read, and
     *   after end-of-input has been encountered.
     */
    public JsonToken getCurrentToken()
    {
        return mCurrToken;
    }

    public boolean hasCurrentToken()
    {
        return mCurrToken != null;
    }

    /**
     * Method that can be called to get the name associated with
     * the current event. Will return null for all token types
     * except for {@link JsonToken#FIELD_NAME}.
     */
    public String getCurrentName()
        throws IOException, JsonParseException
    {
        return (mCurrToken == JsonToken.FIELD_NAME) ? mParsingContext.getCurrentName() : null;
    }

    public void close()
        throws IOException
    {
        closeInput();
        // Also, internal buffer(s) can now be released as well
        releaseBuffers();
    }

    public JsonReadContext getParsingContext()
    {
        return mParsingContext;
    }


    /**
     * Method that return the <b>starting</b> location of the current
     * token; that is, position of the first character from input
     * that starts the current token.
     */
    public JsonLocation getTokenLocation()
    {
        return new JsonLocation(mIOContext.getSourceReference(),
                                mTokenInputTotal,
                                mTokenInputRow, mTokenInputCol + 1);
    }

    /**
     * Method that returns location of the last processed character;
     * usually for error reporting purposes
     */
    public JsonLocation getCurrentLocation()
    {
        return new JsonLocation(mIOContext.getSourceReference(),
                                mCurrInputProcessed + mInputPtr - 1,
                                mCurrInputRow, mInputPtr - mCurrInputRowStart);
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
        if (mTokenIncomplete) {
            finishToken();
        }
        if (mCurrToken != null) { // null only before/after document
            switch (mCurrToken) {
            case FIELD_NAME:
                return mParsingContext.getCurrentName();

            case VALUE_STRING:
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return mTextBuffer.contentsAsString();
                
            default:
                return mCurrToken.asString();
            }
        }
        return null;
    }

    public char[] getTextCharacters()
        throws IOException, JsonParseException
    {
        if (mTokenIncomplete) {
            finishToken();
        }
        if (mCurrToken != null) { // null only before/after document
            switch (mCurrToken) {
                
            case FIELD_NAME:
                if (!mFieldInBuffer) {
                    mTextBuffer.resetWithString(mParsingContext.getCurrentName());
                    mFieldInBuffer = true;
                }
                return mTextBuffer.getTextBuffer();

            case VALUE_STRING:
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return mTextBuffer.getTextBuffer();
                
            default:
                return mCurrToken.asCharArray();
            }
        }
        return null;
    }

    public int getTextLength()
        throws IOException, JsonParseException
    {
        if (mTokenIncomplete) {
            finishToken();
        }
        if (mCurrToken != null) { // null only before/after document
            switch (mCurrToken) {
                
            case FIELD_NAME:
                return mParsingContext.getCurrentName().length();
            case VALUE_STRING:
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return mTextBuffer.size();
                
            default:
                return mCurrToken.asCharArray().length;
            }
        }
        return 0;
    }

    public int getTextOffset()
        throws IOException, JsonParseException
    {
        if (mTokenIncomplete) {
            finishToken();
        }

        // Most have offset of 0, only some may have other values:
        if (mCurrToken != null) {
            switch (mCurrToken) {
            case FIELD_NAME:
                return 0;
            case VALUE_STRING:
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return mTextBuffer.getTextOffset();
            }
        }
        return 0;
    }

    /*
    ////////////////////////////////////////////////////
    // Public low-level accessors
    ////////////////////////////////////////////////////
     */

    public final long getTokenCharacterOffset() { return mTokenInputTotal; }
    public final int getTokenLineNr() { return mTokenInputRow; }
    public final int getTokenColumnNr() { return mTokenInputCol; }

    /*
    ////////////////////////////////////////////////////
    // Low-level reading, linefeed handling
    ////////////////////////////////////////////////////
     */

    protected final void skipCR()
        throws IOException
    {
        if (mInputPtr < mInputLast || loadMore()) {
            ++mInputPtr;
        }
        ++mCurrInputRow;
        mCurrInputRowStart = mInputPtr;
    }

    protected final void skipLF()
        throws IOException
    {
        ++mCurrInputRow;
        mCurrInputRowStart = mInputPtr;
    }

    protected final void markLF() {
        ++mCurrInputRow;
        mCurrInputRowStart = mInputPtr;
    }

    protected final void markLF(int inputPtr) {
        ++mCurrInputRow;
        mCurrInputRowStart = inputPtr;
    }

    /*
    ////////////////////////////////////////////////////
    // Low-level reading, other
    ////////////////////////////////////////////////////
     */

    protected abstract boolean loadMore()
        throws IOException;

    protected abstract char getNextChar(String eofMsg)
        throws IOException, JsonParseException;

    protected abstract void closeInput()
        throws IOException;

    /**
     * Method called to release internal buffers owned by the base
     * reader. This may be called along with {@link #closeInput} (for
     * example, when explicitly closing this reader instance), or
     * separately (if need be).
     */
    protected void releaseBuffers()
        throws IOException
    {
        mTextBuffer.releaseBuffers();
    }

    /**
     * Method called when an EOF is encountered between tokens.
     * If so, it may be a legitimate EOF, but only iff there
     * is no open non-root context.
     */
    protected void handleEOF()
        throws JsonParseException
    {
        if (!mParsingContext.isRoot()) {
            reportInvalidEOF(": expected close marker for "+mParsingContext.getTypeDesc()+" (from "+mParsingContext.getStartLocation(mIOContext.getSourceReference())+")");
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Error reporting
    ////////////////////////////////////////////////////
     */

    protected void reportUnexpectedChar(int ch, String comment)
        throws JsonParseException
    {
        String msg = "Unexpected character ("+getCharDesc(ch)+")";
        if (comment != null) {
            msg += ": "+comment;
        }
        reportError(msg);
    }

    protected void reportInvalidEOF(String msg)
        throws JsonParseException
    {
        reportError("Unexpected end-of-input"+msg);
    }

    protected void throwInvalidSpace(int i)
        throws JsonParseException
    {
        char c = (char) i;
        String msg = "Illegal character ("+getCharDesc(c)+"): only regular white space (\\r, \\n, \\t) is allowed between tokens";
        reportError(msg);
    }

    protected void throwUnquotedSpace(int i, String ctxtDesc)
        throws JsonParseException
    {
        char c = (char) i;
        String msg = "Illegal unquoted character ("+getCharDesc(c)+"): has to be escaped using backslash to be included in "+ctxtDesc;
        reportError(msg);
    }

    protected void reportMismatchedEndMarker(int actCh, char expCh)
        throws JsonParseException
    {
        String startDesc = ""+mParsingContext.getStartLocation(mIOContext.getSourceReference());
        reportError("Unexpected close marker '"+((char) actCh)+"': expected '"+expCh+"' (for "+mParsingContext.getTypeDesc()+" starting at "+startDesc+")");
    }

    /*
    ////////////////////////////////////////////////////
    // Error reporting, generic
    ////////////////////////////////////////////////////
     */

    protected static String getCharDesc(int ch)
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

    protected void reportError(String msg)
        throws JsonParseException
    {
        throw new JsonParseException(msg, getCurrentLocation());
    }

    protected void wrapError(String msg, Throwable t)
        throws JsonParseException
    {
        throw new JsonParseException(msg, getCurrentLocation(), t);
    }

    protected void throwInternal()
    {
        throw new RuntimeException("Internal error: this code path should never get executed");
    }

}
