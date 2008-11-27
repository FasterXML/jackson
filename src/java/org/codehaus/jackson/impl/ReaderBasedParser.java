package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.sym.SymbolTable;
import org.codehaus.jackson.util.*;

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

    final protected SymbolTable _symbols;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    public ReaderBasedParser(IOContext ioCtxt, int features, Reader r, SymbolTable st)
    {
        super(ioCtxt, features, r);
        _symbols = st;
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
        /* First: field names are special -- we will always tokenize
         * (part of) value along with field name to simplify
         * state handling. If so, can and need to use secondary token:
         */
        if (_currToken == JsonToken.FIELD_NAME) {
            return _nextAfterName();
        }
        if (_tokenIncomplete) {
            _skipString(); // only strings can be partial
        }

        int i = _skipWSOrEnd();
        if (i < 0) { // end-of-input
            return (_currToken = null);
        }

        /* First, need to ensure we know the starting location of token
         * after skipping leading white space
         */
        _tokenInputTotal = _currInputProcessed + _inputPtr - 1;
        _tokenInputRow = _currInputRow;
        _tokenInputCol = _inputPtr - _currInputRowStart - 1;

        // Closing scope?
        if (i == INT_RBRACKET) {
            if (!_parsingContext.inArray()) {
                _reportMismatchedEndMarker(i, ']');
            }
            _parsingContext = _parsingContext.getParentImpl();
            return (_currToken = JsonToken.END_ARRAY);
        }
        if (i == INT_RCURLY) {
            if (!_parsingContext.inObject()) {
                _reportMismatchedEndMarker(i, '}');
            }
            _parsingContext = _parsingContext.getParentImpl();
            return (_currToken = JsonToken.END_OBJECT);
        }

        // Nope: do we then expect a comma?
        if (_parsingContext.expectComma()) {
            if (i != INT_COMMA) {
                _reportUnexpectedChar(i, "was expecting comma to separate "+_parsingContext.getTypeDesc()+" entries");
            }
            i = _skipWS();
        }

        /* And should we now have a name? Always true for
         * Object contexts, since the intermediate 'expect-value'
         * state is never retained.
         */
        boolean inObject = _parsingContext.inObject();
        if (inObject) {
            _handleFieldName(i);
            _currToken = JsonToken.FIELD_NAME;
            i = _skipWS();
            if (i != INT_COLON) {
                _reportUnexpectedChar(i, "was expecting a colon to separate field name and value");
            }
            i = _skipWS();
        }

        // Ok: we must have a value... what is it?

        JsonToken t;

        switch (i) {
        case INT_QUOTE:
            _tokenIncomplete = true;
            t = JsonToken.VALUE_STRING;
            break;
        case INT_LBRACKET:
            if (!inObject) {
                _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
            }
            t = JsonToken.START_ARRAY;
            break;
        case INT_LCURLY:
            if (!inObject) {
                _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
            }
            t = JsonToken.START_OBJECT;
            break;
        case INT_RBRACKET:
        case INT_RCURLY:
            // Error: neither is valid at this point; valid closers have
            // been handled earlier
            _reportUnexpectedChar(i, "expected a value");
        case INT_t:
            _matchToken(JsonToken.VALUE_TRUE);
            t = JsonToken.VALUE_TRUE;
            break;
        case INT_f:
            _matchToken(JsonToken.VALUE_FALSE);
            t = JsonToken.VALUE_FALSE;
            break;
        case INT_n:
            _matchToken(JsonToken.VALUE_NULL);
            t = JsonToken.VALUE_NULL;
            break;

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
            t = parseNumberText(i);
            break;
        default:
            _reportUnexpectedChar(i, "expected a valid value (number, String, array, object, 'true', 'false' or 'null')");
            t = null; // never gets here
        }

        if (inObject) {
            _nextToken = t;
            return _currToken;
        }
        _currToken = t;
        return t;
    }

    private final JsonToken _nextAfterName()
    {
        _nameCopied = false; // need to invalidate if it was copied
        JsonToken t = _nextToken;
        _nextToken = null;
        // Also: may need to start new context?
        if (t == JsonToken.START_ARRAY) {
            _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
        } else if (t == JsonToken.START_OBJECT) {
            _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
        }
        return (_currToken = t);
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        _symbols.release();
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, binary access
    ////////////////////////////////////////////////////
     */

    /*
    @Override
    public InputStream readBinaryValue(Base64Variant b64v)
        throws IOException, JsonParseException
    {
        // !!! TBI: implemented base64 decoding
        return null;
    }
    */

    /*
    ////////////////////////////////////////////////////
    // Internal methods, secondary parsing
    ////////////////////////////////////////////////////
     */

    protected void _handleFieldName(int i)
        throws IOException, JsonParseException
    {
        if (i != INT_QUOTE) {
            _reportUnexpectedChar(i, "was expecting double-quote to start field name");
        }
        /* First: let's try to see if we have a simple name: one that does
         * not cross input buffer boundary, and does not contain escape
         * sequences.
         */
        int ptr = _inputPtr;
        int hash = 0;
        final int inputLen = _inputEnd;

        if (ptr < inputLen) {
            final int[] codes = CharTypes.getInputCodeLatin1();
            final int maxCode = codes.length;

            do {
                int ch = _inputBuffer[ptr];
                if (ch < maxCode && codes[ch] != 0) {
                    if (ch == '"') {
                        int start = _inputPtr;
                        _inputPtr = ptr+1; // to skip the quote
                        String name = _symbols.findSymbol(_inputBuffer, start, ptr - start, hash);
                        _parsingContext.setCurrentName(name);
                        return;
                    }
                    break;
                }
                hash = (hash * 31) + ch;
                ++ptr;
            } while (ptr < inputLen);
        }

        int start = _inputPtr;
        _inputPtr = ptr;
        handleFieldName2(start, hash);
    }

    private void handleFieldName2(int startPtr, int hash)
        throws IOException, JsonParseException
    {
        _textBuffer.resetWithShared(_inputBuffer, startPtr, (_inputPtr - startPtr));

        /* Output pointers; calls will also ensure that the buffer is
         * not shared and has room for at least one more char.
         */
        char[] outBuf = _textBuffer.getCurrentSegment();
        int outPtr = _textBuffer.getCurrentSegmentSize();

        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(": was expecting closing quote for name");
                }
            }
            char c = _inputBuffer[_inputPtr++];
            int i = (int) c;
            if (i <= INT_BACKSLASH) {
                if (i == INT_BACKSLASH) {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    c = _decodeEscaped();
                } else if (i <= INT_QUOTE) {
                    if (i == INT_QUOTE) {
                        break;
                    }
                    if (i < INT_SPACE) {
                        _throwUnquotedSpace(i, "name");
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
            TextBuffer tb = _textBuffer;
            char[] buf = tb.getTextBuffer();
            int start = tb.getTextOffset();
            int len = tb.size();

            _parsingContext.setCurrentName(_symbols.findSymbol(buf, start, len, hash));
        }
    }

    protected void finishString()
        throws IOException, JsonParseException
    {
        /* First: let's try to see if we have simple String value: one
         * that does not cross input buffer boundary, and does not
         * contain escape sequences.
         */
        int ptr = _inputPtr;
        final int inputLen = _inputEnd;

        if (ptr < inputLen) {
            final int[] codes = CharTypes.getInputCodeLatin1();
            final int maxCode = codes.length;

            do {
                int ch = _inputBuffer[ptr];
                if (ch < maxCode && codes[ch] != 0) {
                    if (ch == '"') {
                        _textBuffer.resetWithShared(_inputBuffer, _inputPtr, (ptr-_inputPtr));
                        _inputPtr = ptr+1;
                        // Yes, we got it all
                        return;
                    }
                    break;
                }
                ++ptr;
            } while (ptr < inputLen);
        }

        /* Either ran out of input, or bumped into an escape
         * sequence...
         */
        _textBuffer.resetWithCopy(_inputBuffer, _inputPtr, (ptr-_inputPtr));
        _inputPtr = ptr;
        finishString2();
    }

    protected void finishString2()
        throws IOException, JsonParseException
    {
        char[] outBuf = _textBuffer.getCurrentSegment();
        int outPtr = _textBuffer.getCurrentSegmentSize();

        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(": was expecting closing quote for a string value");
                }
            }
            char c = _inputBuffer[_inputPtr++];
            int i = (int) c;
            if (i <= INT_BACKSLASH) {
                if (i == INT_BACKSLASH) {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    c = _decodeEscaped();
                } else if (i <= INT_QUOTE) {
                    if (i == INT_QUOTE) {
                        break;
                    }
                    if (i < INT_SPACE) {
                        _throwUnquotedSpace(i, "string value");
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
    protected void _skipString()
        throws IOException, JsonParseException
    {
        _tokenIncomplete = false;

        int inputPtr = _inputPtr;
        int inputLen = _inputEnd;
        char[] inputBuffer = _inputBuffer;

        while (true) {
            if (inputPtr >= inputLen) {
                _inputPtr = inputPtr;
                if (!loadMore()) {
                    _reportInvalidEOF(": was expecting closing quote for a string value");
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
                    c = _decodeEscaped();
                    inputPtr = _inputPtr;
                    inputLen = _inputEnd;
                } else if (i <= INT_QUOTE) {
                    if (i == INT_QUOTE) {
                        _inputPtr = inputPtr;
                        break;
                    }
                    if (i < INT_SPACE) {
                        _inputPtr = inputPtr;
                        _throwUnquotedSpace(i, "string value");
                    }
                }
            }
        }
    }

    /**
     * Method called to much one of literal tokens we may expect
     */
    protected void _matchToken(JsonToken token)
        throws IOException, JsonParseException
    {
        // First char is already matched, need to check the rest
        String matchStr = token.asString();
        int i = 1;

        for (int len = matchStr.length(); i < len; ++i) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(" in a value");
                }
            }
            char c = _inputBuffer[_inputPtr];
            if (c != matchStr.charAt(i)) {
                _reportInvalidToken(matchStr.substring(0, i));
            }
            ++_inputPtr;
        }
        /* Ok, fine; let's not bother checking anything beyond keyword.
         * If there's something wrong there, it'll cause a parsing
         * error later on.
         */
        return;
    }

    private void _reportInvalidToken(String matchedPart)
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
            char c = _inputBuffer[_inputPtr];
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            ++_inputPtr;
            sb.append(c);
        }

        _reportError("Unrecognized token '"+sb.toString()+"': was expecting 'null', 'true' or 'false'");
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, other parsing
    ////////////////////////////////////////////////////
     */
    
    /**
     * We actually need to check the character value here
     * (to see if we have \n following \r).
     */
    protected final void _skipCR() throws IOException
    {
        if (_inputPtr < _inputEnd || loadMore()) {
            if (_inputBuffer[_inputPtr] == '\n') {
                ++_inputPtr;
            }
        }
        ++_currInputRow;
        _currInputRowStart = _inputPtr;
    }

    protected final void _skipLF() throws IOException
    {
        ++_currInputRow;
        _currInputRowStart = _inputPtr;
    }

    private final int _skipWS()
        throws IOException, JsonParseException
    {
        while (_inputPtr < _inputEnd || loadMore()) {
            int i = (int) _inputBuffer[_inputPtr++];
            if (i > INT_SPACE) {
                if (i != INT_SLASH) {
                    return i;
                }
                _skipComment();
            } else if (i != INT_SPACE) {
                if (i == INT_LF) {
                    _skipLF();
                } else if (i == INT_CR) {
                    _skipCR();
                } else if (i != INT_TAB) {
                    _throwInvalidSpace(i);
                }
            }
        }
        throw _constructError("Unexpected end-of-input within/between "+_parsingContext.getTypeDesc()+" entries");
    }

    private final int _skipWSOrEnd()
        throws IOException, JsonParseException
    {
        while ((_inputPtr < _inputEnd) || loadMore()) {
            int i = (int) _inputBuffer[_inputPtr++];
            if (i > INT_SPACE) {
                 if (i != INT_SLASH) {
                    return i;
                }
                _skipComment();
            } else if (i != INT_SPACE) {
                if (i == INT_LF) {
                    _skipLF();
                } else if (i == INT_CR) {
                    _skipCR();
                } else if (i != INT_TAB) {
                    _throwInvalidSpace(i);
                }
            }
        }
        // We ran out of input...
        _handleEOF();
        return -1;
    }

    private final void _skipComment()
        throws IOException, JsonParseException
    {
        if (!isFeatureEnabled(Feature.ALLOW_COMMENTS)) {
            _reportUnexpectedChar('/', "maybe a (non-standard) comment? (not recognized as one since Feature 'ALLOW_COMMENTS' not enabled for parser)");
        }
        // First: check which comment (if either) it is:
        if (_inputPtr >= _inputEnd && !loadMore()) {
            _reportInvalidEOF(" in a comment");
        }
        char c = _inputBuffer[_inputPtr++];
        if (c == '/') {
            _skipCppComment();
        } else if (c == '*') {
            _skipCComment();
        } else {
            _reportUnexpectedChar(c, "was expecting either '*' or '/' for a comment");
        }
    }

    private final void _skipCComment()
        throws IOException, JsonParseException
    {
        // Ok: need the matching '*/'
        main_loop:
        while ((_inputPtr < _inputEnd) || loadMore()) {
            int i = (int) _inputBuffer[_inputPtr++];
            if (i <= INT_ASTERISK) {
                if (i == INT_ASTERISK) { // end?
                    if ((_inputPtr >= _inputEnd) && !loadMore()) {
                        break main_loop;
                    }
                    if (_inputBuffer[_inputPtr] == INT_SLASH) {
                        ++_inputPtr;
                        return;
                    }
                    continue;
                }
                if (i < INT_SPACE) {
                    if (i == INT_LF) {
                        _skipLF();
                    } else if (i == INT_CR) {
                        _skipCR();
                    } else if (i != INT_TAB) {
                        _throwInvalidSpace(i);
                    }
                }
            }
        }
        _reportInvalidEOF(" in a comment");
    }

    private final void _skipCppComment()
        throws IOException, JsonParseException
    {
        // Ok: need to find EOF or linefeed
        while ((_inputPtr < _inputEnd) || loadMore()) {
            int i = (int) _inputBuffer[_inputPtr++];
            if (i < INT_SPACE) {
                if (i == INT_LF) {
                    _skipLF();
                    break;
                } else if (i == INT_CR) {
                    _skipCR();
                    break;
                } else if (i != INT_TAB) {
                    _throwInvalidSpace(i);
                }
            }
        }
    }

    protected final char _decodeEscaped()
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                _reportInvalidEOF(" in character escape sequence");
            }
        }
        char c = _inputBuffer[_inputPtr++];

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
            _reportError("Unrecognized character escape "+_getCharDesc(c));
        }

        // Ok, a hex escape. Need 4 characters
        int value = 0;
        for (int i = 0; i < 4; ++i) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(" in character escape sequence");
                }
            }
            int ch = (int) _inputBuffer[_inputPtr++];
            int digit = CharTypes.charToHex(ch);
            if (digit < 0) {
                _reportUnexpectedChar(ch, "expected a hex-digit for character escape sequence");
            }
            value = (value << 4) | digit;
        }
        return (char) value;
    }
}
