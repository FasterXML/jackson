package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.util.*;
import static org.codehaus.jackson.JsonReadContext.*;

/**
 * This is a concrete implementation of {@link JsonParser}, which is
 * based on a {@link java.io.Reader} to handle low-level character
 * conversion tasks.
 */
public final class ReaderBasedParser
    extends ReaderBasedNumericParser
{

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    final protected SymbolTable mSymbols;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    public ReaderBasedParser(IOContext ioCtxt, Reader r, SymbolTable st)
    {
        super(ioCtxt, r);
        mSymbols = st;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, traversal
    ////////////////////////////////////////////////////
     */

    /**
     * @return Next token from the stream, if any found, or null
     *   to indicate end-of-input
     */
    public JsonToken nextToken()
        throws IOException, JsonParseException
    {
        if (_tokenIncomplete) {
            _tokenIncomplete = false;
            skipString(); // only strings can be partial
        }

        int i;

        // Space to skip?
        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    handleEOF();
                    return (_currToken = null);
                }
            }
            i = (int) mInputBuffer[_inputPtr++];
            if (i > INT_SPACE) {
                break;
            }
            if (i != INT_SPACE) {
                if (i == INT_LF) {
                    skipLF();
                } else if (i == INT_CR) {
                    skipCR();
                } else if (i != INT_TAB) {
                    throwInvalidSpace(i);
                }
            }
        }

        /* First, need to ensure we know the starting location of token
         * after skipping leading white space
         */
        _tokenInputTotal = _currInputProcessed + _inputPtr - 1;
        _tokenInputRow = _currInputRow;
        _tokenInputCol = _inputPtr - _currInputRowStart - 1;

        // Closing scope?
        if (i == INT_RBRACKET) {
            if (!_parsingContext.isArray()) {
                reportMismatchedEndMarker(i, ']');
            }
            _parsingContext = _parsingContext.getParentImpl();
            return (_currToken = JsonToken.END_ARRAY);
        }
        if (i == INT_RCURLY) {
            if (!_parsingContext.isObject()) {
                reportMismatchedEndMarker(i, '}');
            }
            _parsingContext = _parsingContext.getParentImpl();
            return (_currToken = JsonToken.END_OBJECT);
        }

        // Nope. Have and/or need a separator?
        int sep = _parsingContext.handleSeparator(i);

        switch (sep) {
        case HANDLED_EXPECT_NAME:
        case HANDLED_EXPECT_VALUE:
            // Need to skip space, find next char
            while (true) {
                if (_inputPtr >= _inputEnd) {
                    if (!loadMore()) {
                        reportError("Unexpected end-of-input within/between "+_parsingContext.getTypeDesc()+" entries");
                    }
                }
                i = (int) mInputBuffer[_inputPtr++];
                if (i > INT_SPACE) {
                    break;
                }
                if (i != INT_SPACE) {
                    if (i == INT_LF) {
                        skipLF();
                    } else if (i == INT_CR) {
                        skipCR();
                    } else if (i != INT_TAB) {
                        throwInvalidSpace(i);
                    }
                }
            }
            // And if we expect a name, must be quote
            if (sep == HANDLED_EXPECT_NAME) {
                return handleFieldName(i);
            }
            break;
        case MISSING_COMMA:
            reportUnexpectedChar(i, "was expecting comma to separate "+_parsingContext.getTypeDesc()+" entries");
        case MISSING_COLON:
            reportUnexpectedChar(i, "was expecting colon to separate field name and value");
        case NOT_EXP_SEPARATOR_NEED_VALUE:
            break;
        case NOT_EXP_SEPARATOR_NEED_NAME:
            return handleFieldName(i);
        }

        // We now have the first char: what did we get?
        switch (i) {
        case INT_QUOTE:
            return startString();
        case INT_LBRACKET:
            //_parsingContext = _parsingContext.createChildArrayContext(this);
            _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
            return (_currToken = JsonToken.START_ARRAY);
        case INT_LCURLY:
            //_parsingContext = _parsingContext.createChildObjectContext(this);
            _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
            return (_currToken = JsonToken.START_OBJECT);
        case INT_RBRACKET:
        case INT_RCURLY:
            // Error: neither is valid at this point; valid closers have
            // been handled earlier
            reportUnexpectedChar(i, "expected a value");
        case INT_t:
            return matchToken(JsonToken.VALUE_TRUE);
        case INT_f:
            return matchToken(JsonToken.VALUE_FALSE);
        case INT_n:
            return matchToken(JsonToken.VALUE_NULL);

        case INT_MINUS:
            /* Should we have separate handling for plus? Although
             * it is not allowed per se, it may be erroneously used,
             * and could be indicate by a more specific error message.
             */
        case INT_0:
        case INT_1:
        case INT_2:
        case INT_3:
        case INT_4:
        case INT_5:
        case INT_6:
        case INT_7:
        case INT_8:
        case INT_9:
            return parseNumberText(i);
        }

        reportUnexpectedChar(i, "expected a valid value (number, String, array, object, 'true', 'false' or 'null')");
        return null; // never gets here
    }

    @Override
    public void close()
        throws IOException
    {
        super.close();
        mSymbols.release();
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, binary access
    ////////////////////////////////////////////////////
     */

    @Override
    public int readBinaryValue(OutputStream results)
        throws IOException, JsonParseException
    {
        // !!! TBI: implemented base64 decoding
        return -1;
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, secondary parsing
    ////////////////////////////////////////////////////
     */

    protected JsonToken handleFieldName(int i)
        throws IOException, JsonParseException
    {
        if (i != INT_QUOTE) {
            reportUnexpectedChar(i, "was expecting double-quote to start field name");
        }
        _fieldInBuffer = false; // by default let's expect it won't get there

        /* First: let's try to see if we have a simple name: one that does
         * not cross input buffer boundary, and does not contain escape
         * sequences.
         */
        int ptr = _inputPtr;
        int hash = 0;
        final int inputLen = _inputEnd;

        if (ptr < inputLen) {
            final int[] codes = CharTypes.getInputCode();
            final int maxCode = codes.length;

            do {
                int ch = mInputBuffer[ptr];
                if (ch < maxCode && codes[ch] != 0) {
                    if (ch == '"') {
                        int start = _inputPtr;
                        _inputPtr = ptr+1; // to skip the quote
                        String name = mSymbols.findSymbol(mInputBuffer, start, ptr - start, hash);
                        _parsingContext.setCurrentName(name);
                        return (_currToken = JsonToken.FIELD_NAME);
                    }
                    break;
                }
                hash = (hash * 31) + ch;
                ++ptr;
            } while (ptr < inputLen);
        }

        int start = _inputPtr;
        _inputPtr = ptr;
        return handleFieldName2(start, hash);
    }

    private JsonToken handleFieldName2(int startPtr, int hash)
        throws IOException, JsonParseException
    {
        _textBuffer.resetWithShared(mInputBuffer, startPtr, (_inputPtr - startPtr));

        /* Output pointers; calls will also ensure that the buffer is
         * not shared and has room for at least one more char.
         */
        char[] outBuf = _textBuffer.getCurrentSegment();
        int outPtr = _textBuffer.getCurrentSegmentSize();

        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    reportInvalidEOF(": was expecting closing quote for name");
                }
            }
            char c = mInputBuffer[_inputPtr++];
            int i = (int) c;
            if (i <= INT_BACKSLASH) {
                if (i == INT_BACKSLASH) {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    c = decodeEscaped();
                } else if (i <= INT_QUOTE) {
                    if (i == INT_QUOTE) {
                        break;
                    }
                    if (i < INT_SPACE) {
                        throwUnquotedSpace(i, "name");
                    }
                }
            }
            hash = (hash * 31) + i;
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;

            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
        }
        _textBuffer.setCurrentLength(outPtr);
        {
            _fieldInBuffer = true; // yep, is now stored in text buffer
            TextBuffer tb = _textBuffer;
            char[] buf = tb.getTextBuffer();
            int start = tb.getTextOffset();
            int len = tb.size();

            _parsingContext.setCurrentName(mSymbols.findSymbol(buf, start, len, hash));
        }
        return (_currToken = JsonToken.FIELD_NAME);
    }

    protected JsonToken startString()
        throws IOException, JsonParseException
    {
        /* First: let's try to see if we have simple String value: one
         * that does not cross input buffer boundary, and does not
         * contain escape sequences.
         */
        int ptr = _inputPtr;
        final int inputLen = _inputEnd;

        if (ptr < inputLen) {
            final int[] codes = CharTypes.getInputCode();
            final int maxCode = codes.length;

            do {
                int ch = mInputBuffer[ptr];
                if (ch < maxCode && codes[ch] != 0) {
                    if (ch == '"') {
                        _textBuffer.resetWithShared(mInputBuffer, _inputPtr, (ptr-_inputPtr));
                        _inputPtr = ptr+1;
                        return (_currToken = JsonToken.VALUE_STRING);
                    }
                    break;
                }
                ++ptr;
            } while (ptr < inputLen);
        }

        /* Nope: either ran out of input, or bumped into an escape
         * sequence. Either way, let's defer further parsing to ensure
         * String value is actually needed.
         */
        //int start = mInputPtr;
        _textBuffer.resetWithShared(mInputBuffer, _inputPtr, (ptr-_inputPtr));
        _inputPtr = ptr;
        _tokenIncomplete = true;
        return (_currToken = JsonToken.VALUE_STRING);
    }

    protected void finishString()
        throws IOException, JsonParseException
    {
        /* Output pointers; calls will also ensure that the buffer is
         * not shared and has room for at least one more char.
         */
        char[] outBuf = _textBuffer.getCurrentSegment();
        int outPtr = _textBuffer.getCurrentSegmentSize();

        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    reportInvalidEOF(": was expecting closing quote for a string value");
                }
            }
            char c = mInputBuffer[_inputPtr++];
            int i = (int) c;
            if (i <= INT_BACKSLASH) {
                if (i == INT_BACKSLASH) {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    c = decodeEscaped();
                } else if (i <= INT_QUOTE) {
                    if (i == INT_QUOTE) {
                        break;
                    }
                    if (i < INT_SPACE) {
                        throwUnquotedSpace(i, "string value");
                    }
                }
            }
            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;
        }
        _textBuffer.setCurrentLength(outPtr);
    }

    /**
     * Method called to skim through rest of unparsed String value,
     * if it is not needed. This can be done bit faster if contents
     * need not be stored for future access.
     */
    protected void skipString()
        throws IOException, JsonParseException
    {
        int inputPtr = _inputPtr;
        int inputLen = _inputEnd;
        char[] inputBuffer = mInputBuffer;

        while (true) {
            if (inputPtr >= inputLen) {
                _inputPtr = inputPtr;
                if (!loadMore()) {
                    reportInvalidEOF(": was expecting closing quote for a string value");
                }
                inputPtr = _inputPtr;
                inputLen = _inputEnd;
            }
            char c = inputBuffer[inputPtr++];
            int i = (int) c;
            if (i <= INT_BACKSLASH) {
                if (i == INT_BACKSLASH) {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    _inputPtr = inputPtr;
                    c = decodeEscaped();
                    inputPtr = _inputPtr;
                    inputLen = _inputEnd;
                } else if (i <= INT_QUOTE) {
                    if (i == INT_QUOTE) {
                        _inputPtr = inputPtr;
                        break;
                    }
                    if (i < INT_SPACE) {
                        _inputPtr = inputPtr;
                        throwUnquotedSpace(i, "string value");
                    }
                }
            }
        }
    }

    protected JsonToken matchToken(JsonToken token)
        throws IOException, JsonParseException
    {
        // First char is already matched, need to check the rest
        String matchStr = token.asString();
        int i = 1;

        for (int len = matchStr.length(); i < len; ++i) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    reportInvalidEOF(" in a value");
                }
            }
            char c = mInputBuffer[_inputPtr];
            if (c != matchStr.charAt(i)) {
                reportInvalidToken(matchStr.substring(0, i));
            }
            ++_inputPtr;
        }
        /* Ok, fine; let's not bother checking anything beyond keyword.
         * If there's something wrong there, it'll cause a parsing
         * error later on.
         */
        return (_currToken = token);
    }

    private void reportInvalidToken(String matchedPart)
        throws IOException, JsonParseException
    {
        StringBuilder sb = new StringBuilder(matchedPart);
        /* Let's just try to find what appears to be the token, using
         * regular Java identifier character rules. It's just a heuristic,
         * nothing fancy here.
         */
        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    break;
                }
            }
            char c = mInputBuffer[_inputPtr];
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            ++_inputPtr;
            sb.append(c);
        }

        reportError("Unrecognized token '"+sb.toString()+"': was expecting 'null', 'true' or 'false'");
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, other parsing
    ////////////////////////////////////////////////////
     */

    protected final char decodeEscaped()
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                reportInvalidEOF(" in character escape sequence");
            }
        }
        char c = mInputBuffer[_inputPtr++];

        switch ((int) c) {
            // First, ones that are mapped
        case INT_b:
            return '\b';
        case INT_t:
            return '\t';
        case INT_n:
            return '\n';
        case INT_f:
            return '\f';
        case INT_r:
            return '\r';

            // And these are to be returned as they are
        case INT_QUOTE:
        case INT_SLASH:
        case INT_BACKSLASH:
            return c;

        case INT_u: // and finally hex-escaped
            break;

        default:
            reportError("Unrecognized character escape "+getCharDesc(c));
        }

        // Ok, a hex escape. Need 4 characters
        int value = 0;
        for (int i = 0; i < 4; ++i) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    reportInvalidEOF(" in character escape sequence");
                }
            }
            int ch = (int) mInputBuffer[_inputPtr++];
            int digit = CharTypes.charToHex(ch);
            if (digit < 0) {
                reportUnexpectedChar(ch, "expected a hex-digit for character escape sequence");
            }
            value = (value << 4) | digit;
        }
        return (char) value;
    }
}
