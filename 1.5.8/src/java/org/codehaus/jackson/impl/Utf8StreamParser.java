package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.sym.*;
import org.codehaus.jackson.util.*;

/**
 * This is a concrete implementation of {@link JsonParser}, which is
 * based on a {@link java.io.InputStream} as the input source.
 */
public final class Utf8StreamParser
    extends Utf8NumericParser
{
    final static byte BYTE_LF = (byte) '\n';

    /*
    /***************************************************
    /* Configuration, state
    /***************************************************
    */

    /**
     * Codec used for data binding when (if) requested.
     */
    protected ObjectCodec _objectCodec;

    /**
     * Symbol table that contains field names encountered so far
     */
    final protected BytesToNameCanonicalizer _symbols;

    /**
     * Temporary buffer used for name parsing.
     */
    protected int[] _quadBuffer = new int[32];

    /*
    /***************************************************
    /* Life-cycle
    /***************************************************
     */

    public Utf8StreamParser(IOContext ctxt, int features, InputStream in,
                            ObjectCodec codec,
                            BytesToNameCanonicalizer sym,
                            byte[] inputBuffer, int start, int end,
                            boolean bufferRecyclable)
    {
        super(ctxt, features, in, inputBuffer, start, end, bufferRecyclable);
        _objectCodec = codec;
        _symbols = sym;
        // 12-Mar-2010, tatus: Sanity check, related to [JACKSON-259]:
        if (!JsonParser.Feature.CANONICALIZE_FIELD_NAMES.enabledIn(features)) {
            // should never construct non-canonical utf8/byte parser (instead, use Reader)
            _throwInternal();
        }
    }

    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

    /*
    /***************************************************
    /* Public API, traversal
    /***************************************************
     */

    /**
     * @return Next token from the stream, if any found, or null
     *   to indicate end-of-input
     */
    @Override
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
            /* 19-Feb-2009, tatu: Should actually close/release things
             *    like input source, symbol table and recyclable buffers now.
             */
            close();
            return (_currToken = null);
        }

        /* First, need to ensure we know the starting location of token
         * after skipping leading white space
         */
        _tokenInputTotal = _currInputProcessed + _inputPtr - 1;
        _tokenInputRow = _currInputRow;
        _tokenInputCol = _inputPtr - _currInputRowStart - 1;

        // finally: clear any data retained so far
        _binaryValue = null;

        // Closing scope?
        if (i == INT_RBRACKET) {
            if (!_parsingContext.inArray()) {
                _reportMismatchedEndMarker(i, '}');
            }
            _parsingContext = _parsingContext.getParent();
            return (_currToken = JsonToken.END_ARRAY);
        }
        if (i == INT_RCURLY) {
            if (!_parsingContext.inObject()) {
                _reportMismatchedEndMarker(i, ']');
            }
            _parsingContext = _parsingContext.getParent();
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
            // First, field name itself:
            Name n = _parseFieldName(i);
            _parsingContext.setCurrentName(n.getName());
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
            t = _handleUnexpectedValue(i);
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
        // Merge found symbols, if any:
        _symbols.release();
    }

    /*
    /***************************************************
    /* Internal methods, secondary parsing
    /***************************************************
     */

    protected final Name _parseFieldName(int i)
        throws IOException, JsonParseException
    {
        if (i != INT_QUOTE) {
            return _handleUnusualFieldName(i);
        }
        // First: can we optimize out bounds checks?
        if ((_inputEnd - _inputPtr) < 9) { // Need 8 chars, plus one trailing (quote)
            return slowParseFieldName();
        }

        // If so, can also unroll loops nicely
        /* 25-Nov-2008, tatu: This may seem weird, but here we do
         *   NOT want to worry about UTF-8 decoding. Rather, we'll
         *   assume that part is ok (if not it will get caught
         *   later on), and just handle quotes and backslashes here.
         */
        final int[] codes = CharTypes.getInputCodeLatin1();

        int q = _inputBuffer[_inputPtr++] & 0xFF;
        if (codes[q] != 0) {
            if (q == INT_QUOTE) { // special case, ""
                return BytesToNameCanonicalizer.getEmptyName();
            }
            return parseFieldName(0, q, 0); // quoting or invalid char
        }

        i = _inputBuffer[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // one byte/char case or broken
                return findName(q, 1);
            }
            return parseFieldName(q, i, 1);
        }
        q = (q << 8) | i;
        i = _inputBuffer[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // two byte name or broken
                return findName(q, 2);
            }
            return parseFieldName(q, i, 2);
        }
        q = (q << 8) | i;
        i = _inputBuffer[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // three byte name or broken
                return findName(q, 3);
            }
            return parseFieldName(q, i, 3);
        }
        q = (q << 8) | i;
        i = _inputBuffer[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // four byte name or broken
                return findName(q, 4);
            }
            return parseFieldName(q, i, 4);
        }
        return parseMediumFieldName(q, i);
    }

    protected Name parseMediumFieldName(int q1, int q2)
        throws IOException, JsonParseException
    {
        // As mentioned earlier, we do ignore UTF-8 aspects at this point
        final int[] codes = CharTypes.getInputCodeLatin1();

        // Ok, got 5 name bytes so far
        int i = _inputBuffer[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 5 bytes
                return findName(q1, q2, 1);
            }
            return parseFieldName(q1, q2, i, 1); // quoting or invalid char
        }
        q2 = (q2 << 8) | i;
        i = _inputBuffer[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 6 bytes
                return findName(q1, q2, 2);
            }
            return parseFieldName(q1, q2, i, 2);
        }
        q2 = (q2 << 8) | i;
        i = _inputBuffer[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 7 bytes
                return findName(q1, q2, 3);
            }
            return parseFieldName(q1, q2, i, 3);
        }
        q2 = (q2 << 8) | i;
        i = _inputBuffer[_inputPtr++] & 0xFF;
        if (codes[i] != 0) {
            if (i == INT_QUOTE) { // 8 bytes
                return findName(q1, q2, 4);
            }
            return parseFieldName(q1, q2, i, 4);
        }
        _quadBuffer[0] = q1;
        _quadBuffer[1] = q2;
        return parseLongFieldName(i);
    }

    protected Name parseLongFieldName(int q)
        throws IOException, JsonParseException
    {
        // As explained above, will ignore utf-8 encoding at this point
        final int[] codes = CharTypes.getInputCodeLatin1();
        int qlen = 2;

        while (true) {
            /* Let's offline if we hit buffer boundary (otherwise would
             * need to [try to] align input, which is bit complicated
             * and may not always be possible)
             */
            if ((_inputEnd - _inputPtr) < 4) {
                return parseEscapedFieldName(_quadBuffer, qlen, 0, q, 0);
            }
            // Otherwise can skip boundary checks for 4 bytes in loop

            int i = _inputBuffer[_inputPtr++] & 0xFF;
            if (codes[i] != 0) {
                if (i == INT_QUOTE) {
                    return findName(_quadBuffer, qlen, q, 1);
                }
                return parseEscapedFieldName(_quadBuffer, qlen, q, i, 1);
            }

            q = (q << 8) | i;
            i = _inputBuffer[_inputPtr++] & 0xFF;
            if (codes[i] != 0) {
                if (i == INT_QUOTE) {
                    return findName(_quadBuffer, qlen, q, 2);
                }
                return parseEscapedFieldName(_quadBuffer, qlen, q, i, 2);
            }

            q = (q << 8) | i;
            i = _inputBuffer[_inputPtr++] & 0xFF;
            if (codes[i] != 0) {
                if (i == INT_QUOTE) {
                    return findName(_quadBuffer, qlen, q, 3);
                }
                return parseEscapedFieldName(_quadBuffer, qlen, q, i, 3);
            }

            q = (q << 8) | i;
            i = _inputBuffer[_inputPtr++] & 0xFF;
            if (codes[i] != 0) {
                if (i == INT_QUOTE) {
                    return findName(_quadBuffer, qlen, q, 4);
                }
                return parseEscapedFieldName(_quadBuffer, qlen, q, i, 4);
            }

            // Nope, no end in sight. Need to grow quad array etc
            if (qlen >= _quadBuffer.length) {
                _quadBuffer = growArrayBy(_quadBuffer, qlen);
            }
            _quadBuffer[qlen++] = q;
            q = i;
        }
    }

    /**
     * Method called when not even first 8 bytes are guaranteed
     * to come consequtively. Happens rarely, so this is offlined;
     * plus we'll also do full checks for escaping etc.
     */
    protected Name slowParseFieldName()
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                _reportInvalidEOF(": was expecting closing '\"' for name");
            }
        }
        int i = _inputBuffer[_inputPtr++] & 0xFF;
        if (i == INT_QUOTE) { // special case, ""
            return BytesToNameCanonicalizer.getEmptyName();
        }
        return parseEscapedFieldName(_quadBuffer, 0, 0, i, 0);
    }

    private final Name parseFieldName(int q1, int ch, int lastQuadBytes)
        throws IOException, JsonParseException
    {
        return parseEscapedFieldName(_quadBuffer, 0, q1, ch, lastQuadBytes);
    }

    private final Name parseFieldName(int q1, int q2, int ch, int lastQuadBytes)
        throws IOException, JsonParseException
    {
        _quadBuffer[0] = q1;
        return parseEscapedFieldName(_quadBuffer, 1, q2, ch, lastQuadBytes);
    }

    /**
     * Slower parsing method which is generally branched to when
     * an escape sequence is detected (or alternatively for long
     * names, or ones crossing input buffer boundary). In any case,
     * needs to be able to handle more exceptional cases, gets
     * slower, and hance is offlined to a separate method.
     */
    protected Name parseEscapedFieldName(int[] quads, int qlen, int currQuad, int ch,
                                         int currQuadBytes)
        throws IOException, JsonParseException
    {
        /* 25-Nov-2008, tatu: This may seem weird, but here we do
         *   NOT want to worry about UTF-8 decoding. Rather, we'll
         *   assume that part is ok (if not it will get caught
         *   later on), and just handle quotes and backslashes here.
         */
        final int[] codes = CharTypes.getInputCodeLatin1();

        while (true) {
            if (codes[ch] != 0) {
                if (ch == INT_QUOTE) { // we are done
                    break;
                }
                // Unquoted white space?
                if (ch != INT_BACKSLASH) {
                    // As per [JACKSON-208], call can now return:
                    _throwUnquotedSpace(ch, "name");
                } else {
                    // Nope, escape sequence
                    ch = _decodeEscaped();
                }
                /* Oh crap. May need to UTF-8 (re-)encode it, if it's
                 * beyond 7-bit ascii. Gets pretty messy.
                 * If this happens often, may want to use different name
                 * canonicalization to avoid these hits.
                 */
                if (ch > 127) {
                    // Ok, we'll need room for first byte right away
                    if (currQuadBytes >= 4) {
                        if (qlen >= quads.length) {
                            _quadBuffer = quads = growArrayBy(quads, quads.length);
                        }
                        quads[qlen++] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                    }
                    if (ch < 0x800) { // 2-byte
                        currQuad = (currQuad << 8) | (0xc0 | (ch >> 6));
                        ++currQuadBytes;
                        // Second byte gets output below:
                    } else { // 3 bytes; no need to worry about surrogates here
                        currQuad = (currQuad << 8) | (0xe0 | (ch >> 12));
                        ++currQuadBytes;
                        // need room for middle byte?
                        if (currQuadBytes >= 4) {
                            if (qlen >= quads.length) {
                                _quadBuffer = quads = growArrayBy(quads, quads.length);
                            }
                            quads[qlen++] = currQuad;
                            currQuad = 0;
                            currQuadBytes = 0;
                        }
                        currQuad = (currQuad << 8) | (0x80 | ((ch >> 6) & 0x3f));
                        ++currQuadBytes;
                    }
                    // And same last byte in both cases, gets output below:
                    ch = 0x80 | (ch & 0x3f);
                }
            }
            // Ok, we have one more byte to add at any rate:
            if (currQuadBytes < 4) {
                ++currQuadBytes;
                currQuad = (currQuad << 8) | ch;
            } else {
                if (qlen >= quads.length) {
                    _quadBuffer = quads = growArrayBy(quads, quads.length);
                }
                quads[qlen++] = currQuad;
                currQuad = ch;
                currQuadBytes = 1;
            }
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(" in field name");
                }
            }
            ch = _inputBuffer[_inputPtr++] & 0xFF;
        }

        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                _quadBuffer = quads = growArrayBy(quads, quads.length);
            }
            quads[qlen++] = currQuad;
        }
        Name name = _symbols.findName(quads, qlen);
        if (name == null) {
            name = addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    /**
     * Method called when we see non-white space character other
     * than double quote, when expecting a field name.
     * In standard mode will just throw an expection; but
     * in non-standard modes may be able to parse name.
     */
    protected final Name _handleUnusualFieldName(int ch)
        throws IOException, JsonParseException
    {
        // [JACKSON-173]: allow single quotes
        if (ch == INT_APOSTROPHE && isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
            return _parseApostropheFieldName();
        }
        // [JACKSON-69]: allow unquoted names if feature enabled:
        if (!isEnabled(Feature.ALLOW_UNQUOTED_FIELD_NAMES)) {
            _reportUnexpectedChar(ch, "was expecting double-quote to start field name");
        }
        /* Also: note that although we use a different table here,
         * it does NOT handle UTF-8 decoding. It'll just pass those
         * high-bit codes as acceptable for later decoding.
         */
        final int[] codes = CharTypes.getInputCodeUtf8JsNames();
        // Also: must start with a valid character...
        if (codes[ch] != 0) {
            _reportUnexpectedChar(ch, "was expecting either valid name character (for unquoted name) or double-quote (for quoted) to start field name");
        }

        /* Ok, now; instead of ultra-optimizing parsing here (as with
         * regular JSON names), let's just use the generic "slow"
         * variant. Can measure its impact later on if need be
         */
        int[] quads = _quadBuffer;
        int qlen = 0;
        int currQuad = 0;
        int currQuadBytes = 0;

        while (true) {
            // Ok, we have one more byte to add at any rate:
            if (currQuadBytes < 4) {
                ++currQuadBytes;
                currQuad = (currQuad << 8) | ch;
            } else {
                if (qlen >= quads.length) {
                    _quadBuffer = quads = growArrayBy(quads, quads.length);
                }
                quads[qlen++] = currQuad;
                currQuad = ch;
                currQuadBytes = 1;
            }
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(" in field name");
                }
            }
            ch = _inputBuffer[_inputPtr] & 0xFF;
            if (codes[ch] != 0) {
                break;
            }
            ++_inputPtr;
        }

        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                _quadBuffer = quads = growArrayBy(quads, quads.length);
            }
            quads[qlen++] = currQuad;
        }
        Name name = _symbols.findName(quads, qlen);
        if (name == null) {
            name = addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    /* Parsing to support [JACKSON-173]. Plenty of duplicated code;
     * main reason being to try to avoid slowing down fast path
     * for valid JSON -- more alternatives, more code, generally
     * bit slower execution.
     */
    protected final Name _parseApostropheFieldName()
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                _reportInvalidEOF(": was expecting closing '\'' for name");
            }
        }
        int ch = _inputBuffer[_inputPtr++] & 0xFF;
        if (ch == INT_APOSTROPHE) { // special case, ''
            return BytesToNameCanonicalizer.getEmptyName();
        }
        int[] quads = _quadBuffer;
        int qlen = 0;
        int currQuad = 0;
        int currQuadBytes = 0;

        // Copied from parseEscapedFieldName, with minor mods:

        final int[] codes = CharTypes.getInputCodeLatin1();

        while (true) {
            if (ch == INT_APOSTROPHE) {
                break;
            }
            // additional check to skip handling of double-quotes
            if (ch != INT_QUOTE && codes[ch] != 0) {
                if (ch != INT_BACKSLASH) {
                    // Unquoted white space?
                    // As per [JACKSON-208], call can now return:
                    _throwUnquotedSpace(ch, "name");
                } else {
                    // Nope, escape sequence
                    ch = _decodeEscaped();
                }
                /* Oh crap. May need to UTF-8 (re-)encode it, if it's
                 * beyond 7-bit ascii. Gets pretty messy.
                 * If this happens often, may want to use different name
                 * canonicalization to avoid these hits.
                 */
                if (ch > 127) {
                    // Ok, we'll need room for first byte right away
                    if (currQuadBytes >= 4) {
                        if (qlen >= quads.length) {
                            _quadBuffer = quads = growArrayBy(quads, quads.length);
                        }
                        quads[qlen++] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                    }
                    if (ch < 0x800) { // 2-byte
                        currQuad = (currQuad << 8) | (0xc0 | (ch >> 6));
                        ++currQuadBytes;
                        // Second byte gets output below:
                    } else { // 3 bytes; no need to worry about surrogates here
                        currQuad = (currQuad << 8) | (0xe0 | (ch >> 12));
                        ++currQuadBytes;
                        // need room for middle byte?
                        if (currQuadBytes >= 4) {
                            if (qlen >= quads.length) {
                                _quadBuffer = quads = growArrayBy(quads, quads.length);
                            }
                            quads[qlen++] = currQuad;
                            currQuad = 0;
                            currQuadBytes = 0;
                        }
                        currQuad = (currQuad << 8) | (0x80 | ((ch >> 6) & 0x3f));
                        ++currQuadBytes;
                    }
                    // And same last byte in both cases, gets output below:
                    ch = 0x80 | (ch & 0x3f);
                }
            }
            // Ok, we have one more byte to add at any rate:
            if (currQuadBytes < 4) {
                ++currQuadBytes;
                currQuad = (currQuad << 8) | ch;
            } else {
                if (qlen >= quads.length) {
                    _quadBuffer = quads = growArrayBy(quads, quads.length);
                }
                quads[qlen++] = currQuad;
                currQuad = ch;
                currQuadBytes = 1;
            }
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(" in field name");
                }
            }
            ch = _inputBuffer[_inputPtr++] & 0xFF;
        }

        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                _quadBuffer = quads = growArrayBy(quads, quads.length);
            }
            quads[qlen++] = currQuad;
        }
        Name name = _symbols.findName(quads, qlen);
        if (name == null) {
            name = addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    /*
    /***************************************************
    /* Internal methods, symbol (name) handling
    /***************************************************
     */

    private final Name findName(int q1, int lastQuadBytes)
        throws JsonParseException
    {
        // Usually we'll find it from the canonical symbol table already
        Name name = _symbols.findName(q1);
        if (name != null) {
            return name;
        }
        // If not, more work. We'll need add stuff to buffer
        _quadBuffer[0] = q1;
        return addName(_quadBuffer, 1, lastQuadBytes);
    }

    private final Name findName(int q1, int q2, int lastQuadBytes)
        throws JsonParseException
    {
        // Usually we'll find it from the canonical symbol table already
        Name name = _symbols.findName(q1, q2);
        if (name != null) {
            return name;
        }
        // If not, more work. We'll need add stuff to buffer
        _quadBuffer[0] = q1;
        _quadBuffer[1] = q2;
        return addName(_quadBuffer, 2, lastQuadBytes);
    }

    private final Name findName(int[] quads, int qlen, int lastQuad, int lastQuadBytes)
        throws JsonParseException
    {
        if (qlen >= quads.length) {
            _quadBuffer = quads = growArrayBy(quads, quads.length);
        }
        quads[qlen++] = lastQuad;
        Name name = _symbols.findName(quads, qlen);
        if (name == null) {
            return addName(quads, qlen, lastQuadBytes);
        }
        return name;
    }

    /**
     * This is the main workhorse method used when we take a symbol
     * table miss. It needs to demultiplex individual bytes, decode
     * multi-byte chars (if any), and then construct Name instance
     * and add it to the symbol table.
     */
    private final Name addName(int[] quads, int qlen, int lastQuadBytes)
        throws JsonParseException
    {
        /* Ok: must decode UTF-8 chars. No other validation is
         * needed, since unescaping has been done earlier as necessary
         * (as well as error reporting for unescaped control chars)
         */
        // 4 bytes per quad, except last one maybe less
        int byteLen = (qlen << 2) - 4 + lastQuadBytes;

        /* And last one is not correctly aligned (leading zero bytes instead
         * need to shift a bit, instead of trailing). Only need to shift it
         * for UTF-8 decoding; need revert for storage (since key will not
         * be aligned, to optimize lookup speed)
         */
        int lastQuad;

        if (lastQuadBytes < 4) {
            lastQuad = quads[qlen-1];
            // 8/16/24 bit left shift
            quads[qlen-1] = (lastQuad << ((4 - lastQuadBytes) << 3));
        } else {
            lastQuad = 0;
        }

        // Need some working space, TextBuffer works well:
        char[] cbuf = _textBuffer.emptyAndGetCurrentSegment();
        int cix = 0;

        for (int ix = 0; ix < byteLen; ) {
            int ch = quads[ix >> 2]; // current quad, need to shift+mask
            int byteIx = (ix & 3);
            ch = (ch >> ((3 - byteIx) << 3)) & 0xFF;
            ++ix;

            if (ch > 127) { // multi-byte
                int needed;
                if ((ch & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
                    ch &= 0x1F;
                    needed = 1;
                } else if ((ch & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
                    ch &= 0x0F;
                    needed = 2;
                } else if ((ch & 0xF8) == 0xF0) { // 4 bytes; double-char with surrogates and all...
                    ch &= 0x07;
                    needed = 3;
                } else { // 5- and 6-byte chars not valid xml chars
                    _reportInvalidInitial(ch);
                    needed = ch = 1; // never really gets this far
                }
                if ((ix + needed) > byteLen) {
                    _reportInvalidEOF(" in field name");
                }
                
                // Ok, always need at least one more:
                int ch2 = quads[ix >> 2]; // current quad, need to shift+mask
                byteIx = (ix & 3);
                ch2 = (ch2 >> ((3 - byteIx) << 3));
                ++ix;
                
                if ((ch2 & 0xC0) != 0x080) {
                    _reportInvalidOther(ch2);
                }
                ch = (ch << 6) | (ch2 & 0x3F);
                if (needed > 1) {
                    ch2 = quads[ix >> 2];
                    byteIx = (ix & 3);
                    ch2 = (ch2 >> ((3 - byteIx) << 3));
                    ++ix;
                    
                    if ((ch2 & 0xC0) != 0x080) {
                        _reportInvalidOther(ch2);
                    }
                    ch = (ch << 6) | (ch2 & 0x3F);
                    if (needed > 2) { // 4 bytes? (need surrogates on output)
                        ch2 = quads[ix >> 2];
                        byteIx = (ix & 3);
                        ch2 = (ch2 >> ((3 - byteIx) << 3));
                        ++ix;
                        if ((ch2 & 0xC0) != 0x080) {
                            _reportInvalidOther(ch2 & 0xFF);
                        }
                        ch = (ch << 6) | (ch2 & 0x3F);
                    }
                }
                if (needed > 2) { // surrogate pair? once again, let's output one here, one later on
                    ch -= 0x10000; // to normalize it starting with 0x0
                    if (cix >= cbuf.length) {
                        cbuf = _textBuffer.expandCurrentSegment();
                    }
                    cbuf[cix++] = (char) (0xD800 + (ch >> 10));
                    ch = 0xDC00 | (ch & 0x03FF);
                }
            }
            if (cix >= cbuf.length) {
                cbuf = _textBuffer.expandCurrentSegment();
            }
            cbuf[cix++] = (char) ch;
        }

        /* Ok. Now we have the character array, and can construct the
         * String (as well as check proper composition of semicolons
         * for ns-aware mode...)
         */
        String baseName = new String(cbuf, 0, cix);
        // And finally, un-align if necessary
        if (lastQuadBytes < 4) {
            quads[qlen-1] = lastQuad;
        }
        return _symbols.addName(baseName, quads, qlen);
    }

    /*
    /***************************************************
    /* Internal methods, String value parsing
    /***************************************************
     */

    @Override
    protected void _finishString()
        throws IOException, JsonParseException
    {
        int outPtr = 0;
        int c;
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();

        // Here we do want to do full decoding, hence:
        final int[] codes = CharTypes.getInputCodeUtf8();
        final byte[] inputBuffer = _inputBuffer;

        main_loop:
        while (true) {
            // Then the tight ascii non-funny-char loop:
            ascii_loop:
            while (true) {
                int ptr = _inputPtr;
                if (ptr >= _inputEnd) {
                    loadMoreGuaranteed();
                    ptr = _inputPtr;
                }
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                int max = _inputEnd;
                {
                    int max2 = ptr + (outBuf.length - outPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (ptr < max) {
                    c = (int) inputBuffer[ptr++] & 0xFF;
                    if (codes[c] != 0) {
                        _inputPtr = ptr;
                        break ascii_loop;
                    }
                    outBuf[outPtr++] = (char) c;
                }
                _inputPtr = ptr;
            }
            // Ok: end marker, escape or multi-byte?
            if (c == INT_QUOTE) {
                break main_loop;
            }

            switch (codes[c]) {
            case 1: // backslash
                c = _decodeEscaped();
                break;
            case 2: // 2-byte UTF
                c = _decodeUtf8_2(c);
                break;
            case 3: // 3-byte UTF
                if ((_inputEnd - _inputPtr) >= 2) {
                    c = _decodeUtf8_3fast(c);
                } else {
                    c = _decodeUtf8_3(c);
                }
                break;
            case 4: // 4-byte UTF
                c = _decodeUtf8_4(c);
                // Let's add first part right away:
                outBuf[outPtr++] = (char) (0xD800 | (c >> 10));
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output down below
                break;
            default:
                if (c < INT_SPACE) {
                    // As per [JACKSON-208], call can now return:
                    _throwUnquotedSpace(c, "string value");
                } else {
                    // Is this good enough error message?
                    _reportInvalidChar(c);
                }
            }
            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = (char) c;
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

        // Need to be fully UTF-8 aware here:
        final int[] codes = CharTypes.getInputCodeUtf8();
        final byte[] inputBuffer = _inputBuffer;

        main_loop:
        while (true) {
            int c;

            ascii_loop:
            while (true) {
                int ptr = _inputPtr;
                int max = _inputEnd;
                if (ptr >= max) {
                    loadMoreGuaranteed();
                    ptr = _inputPtr;
                    max = _inputEnd;
                }
                while (ptr < max) {
                    c = (int) inputBuffer[ptr++] & 0xFF;
                    if (codes[c] != 0) {
                        _inputPtr = ptr;
                        break ascii_loop;
                    }
                }
                _inputPtr = ptr;
            }
            // Ok: end marker, escape or multi-byte?
            if (c == INT_QUOTE) {
                break main_loop;
            }

            switch (codes[c]) {
            case 1: // backslash
                _decodeEscaped();
                break;
            case 2: // 2-byte UTF
                _skipUtf8_2(c);
                break;
            case 3: // 3-byte UTF
                _skipUtf8_3(c);
                break;
            case 4: // 4-byte UTF
                _skipUtf8_4(c);
                break;
            default:
                if (c < INT_SPACE) {
                    // As per [JACKSON-208], call can now return:
                    _throwUnquotedSpace(c, "string value");
                } else {
                    // Is this good enough error message?
                    _reportInvalidChar(c);
                }
            }
        }
    }

    /**
     * Method for handling cases where first non-space character
     * of an expected value token is not legal for standard JSON content.
     *
     * @since 1.3
     */
    protected final JsonToken _handleUnexpectedValue(int c)
        throws IOException, JsonParseException
    {
        // Most likely an error, unless we are to allow single-quote-strings
        if (c != INT_APOSTROPHE || !isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
            _reportUnexpectedChar(c, "expected a valid value (number, String, array, object, 'true', 'false' or 'null')");
        }

        // Otherwise almost verbatim copy of _finishString()
        int outPtr = 0;
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();

        // Here we do want to do full decoding, hence:
        final int[] codes = CharTypes.getInputCodeUtf8();
        final byte[] inputBuffer = _inputBuffer;

        main_loop:
        while (true) {
            // Then the tight ascii non-funny-char loop:
            ascii_loop:
            while (true) {
                if (_inputPtr >= _inputEnd) {
                    loadMoreGuaranteed();
                }
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                int max = _inputEnd;
                {
                    int max2 = _inputPtr + (outBuf.length - outPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (_inputPtr < max) {
                    c = (int) inputBuffer[_inputPtr++] & 0xFF;
                    if (c == INT_APOSTROPHE || codes[c] != 0) {
                        break ascii_loop;
                    }
                    outBuf[outPtr++] = (char) c;
                }
            }

            // Ok: end marker, escape or multi-byte?
            if (c == INT_APOSTROPHE) {
                break main_loop;
            }

            switch (codes[c]) {
            case 1: // backslash
                if (c != INT_QUOTE) { // marked as special, isn't here
                    c = _decodeEscaped();
                }
                break;
            case 2: // 2-byte UTF
                c = _decodeUtf8_2(c);
                break;
            case 3: // 3-byte UTF
                if ((_inputEnd - _inputPtr) >= 2) {
                    c = _decodeUtf8_3fast(c);
                } else {
                    c = _decodeUtf8_3(c);
                }
                break;
            case 4: // 4-byte UTF
                c = _decodeUtf8_4(c);
                // Let's add first part right away:
                outBuf[outPtr++] = (char) (0xD800 | (c >> 10));
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output down below
                break;
            default:
                if (c < INT_SPACE) {
                    _throwUnquotedSpace(c, "string value");
                }
                // Is this good enough error message?
                _reportInvalidChar(c);
            }
            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = (char) c;
        }
        _textBuffer.setCurrentLength(outPtr);

        return JsonToken.VALUE_STRING;
    }

    /*
    /***************************************************
    /* Internal methods, other parsing helper methods
    /***************************************************
     */

    protected void _matchToken(JsonToken token)
        throws IOException, JsonParseException
    {
        // First char is already matched, need to check the rest
        byte[] matchBytes = token.asByteArray();
        int i = 1;

        for (int len = matchBytes.length; i < len; ++i) {
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            if (matchBytes[i] != _inputBuffer[_inputPtr]) {
                _reportInvalidToken(token.asString().substring(0, i));
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
         * nothing fancy here (nor fast).
         */
        while (true) {
            if (_inputPtr >= _inputEnd && !loadMore()) {
                break;
            }
            int i = (int) _inputBuffer[_inputPtr++];
            char c = (char) _decodeCharForError(i);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            ++_inputPtr;
            sb.append(c);
        }

        _reportError("Unrecognized token '"+sb.toString()+"': was expecting 'null', 'true' or 'false'");
    }

    /*
    /***************************************************
    /* Internal methods, ws skipping, escape/unescape
    /***************************************************
     */

    private final int _skipWS()
        throws IOException, JsonParseException
    {
        while (_inputPtr < _inputEnd || loadMore()) {
            int i = _inputBuffer[_inputPtr++] & 0xFF;
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
            int i = _inputBuffer[_inputPtr++] & 0xFF;
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
        if (!isEnabled(Feature.ALLOW_COMMENTS)) {
            _reportUnexpectedChar('/', "maybe a (non-standard) comment? (not recognized as one since Feature 'ALLOW_COMMENTS' not enabled for parser)");
        }
        // First: check which comment (if either) it is:
        if (_inputPtr >= _inputEnd && !loadMore()) {
            _reportInvalidEOF(" in a comment");
        }
        int c = _inputBuffer[_inputPtr++] & 0xFF;
        if (c == INT_SLASH) {
            _skipCppComment();
        } else if (c == INT_ASTERISK) {
            _skipCComment();
        } else {
            _reportUnexpectedChar(c, "was expecting either '*' or '/' for a comment");
        }
    }

    private final void _skipCComment()
        throws IOException, JsonParseException
    {
        // Need to be UTF-8 aware here to decode content (for skipping)
        final int[] codes = CharTypes.getInputCodeComment();

        // Ok: need the matching '*/'
        while ((_inputPtr < _inputEnd) || loadMore()) {
            int i = (int) _inputBuffer[_inputPtr++] & 0xFF;
            int code = codes[i];
            if (code != 0) {
                switch (code) {
                case INT_ASTERISK:
                    if (_inputBuffer[_inputPtr] == INT_SLASH) {
                        ++_inputPtr;
                        return;
                    }
                    break;
                case INT_LF:
                    _skipLF();
                    break;
                case INT_CR:
                    _skipCR();
                    break;
                default: // e.g. -1
                    // Is this good enough error message?
                    _reportInvalidChar(i);
                }
            }
        }
        _reportInvalidEOF(" in a comment");
    }

    private final void _skipCppComment()
        throws IOException, JsonParseException
    {
        // Ok: need to find EOF or linefeed
        final int[] codes = CharTypes.getInputCodeComment();
        while ((_inputPtr < _inputEnd) || loadMore()) {
            int i = (int) _inputBuffer[_inputPtr++] & 0xFF;
            int code = codes[i];
            if (code != 0) {
                switch (code) {
                case INT_LF:
                    _skipLF();
                    return;
                case INT_CR:
                    _skipCR();
                    return;
                case INT_ASTERISK: // nop for these comments
                    break;
                default: // e.g. -1
                    // Is this good enough error message?
                    _reportInvalidChar(i);
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
        int c = (int) _inputBuffer[_inputPtr++];

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
            return (char) c;

        case INT_u: // and finally hex-escaped
            break;

        default:
            _reportError("Unrecognized character escape (\\ followed by "+_getCharDesc(_decodeCharForError(c))+")");
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

    protected int _decodeCharForError(int firstByte)
        throws IOException, JsonParseException
    {
        int c = (int) firstByte;
        if (c < 0) { // if >= 0, is ascii and fine as is
            int needed;
            
            // Ok; if we end here, we got multi-byte combination
            if ((c & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
                c &= 0x1F;
                needed = 1;
            } else if ((c & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
                c &= 0x0F;
                needed = 2;
            } else if ((c & 0xF8) == 0xF0) {
                // 4 bytes; double-char with surrogates and all...
                c &= 0x07;
                needed = 3;
            } else {
                _reportInvalidInitial(c & 0xFF);
                needed = 1; // never gets here
            }

            int d = nextByte();
            if ((d & 0xC0) != 0x080) {
                _reportInvalidOther(d & 0xFF);
            }
            c = (c << 6) | (d & 0x3F);
            
            if (needed > 1) { // needed == 1 means 2 bytes total
                d = nextByte(); // 3rd byte
                if ((d & 0xC0) != 0x080) {
                    _reportInvalidOther(d & 0xFF);
                }
                c = (c << 6) | (d & 0x3F);
                if (needed > 2) { // 4 bytes? (need surrogates)
                    d = nextByte();
                    if ((d & 0xC0) != 0x080) {
                        _reportInvalidOther(d & 0xFF);
                    }
                    c = (c << 6) | (d & 0x3F);
                }
            }
        }
        return c;
    }

    /*
    /***************************************************
    /* Internal methods,UTF8 decoding
    /***************************************************
     */

    private final int _decodeUtf8_2(int c)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        return ((c & 0x1F) << 6) | (d & 0x3F);
    }

    private final int _decodeUtf8_3(int c1)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        c1 &= 0x0F;
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        int c = (c1 << 6) | (d & 0x3F);
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = (c << 6) | (d & 0x3F);
        return c;
    }

    private final int _decodeUtf8_3fast(int c1)
        throws IOException, JsonParseException
    {
        c1 &= 0x0F;
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        int c = (c1 << 6) | (d & 0x3F);
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = (c << 6) | (d & 0x3F);
        return c;
    }

    /**
     * @return Character value <b>minus 0x10000</c>; this so that caller
     *    can readily expand it to actual surrogates
     */
    private final int _decodeUtf8_4(int c)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = ((c & 0x07) << 6) | (d & 0x3F);

        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = (c << 6) | (d & 0x3F);
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }

        /* note: won't change it to negative here, since caller
         * already knows it'll need a surrogate
         */
        return ((c << 6) | (d & 0x3F)) - 0x10000;
    }

    private final void _skipUtf8_2(int c)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        c = (int) _inputBuffer[_inputPtr++];
        if ((c & 0xC0) != 0x080) {
            _reportInvalidOther(c & 0xFF, _inputPtr);
        }
    }

    /* Alas, can't heavily optimize skipping, since we still have to
     * do validity checks...
     */
    private final void _skipUtf8_3(int c)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        //c &= 0x0F;
        c = (int) _inputBuffer[_inputPtr++];
        if ((c & 0xC0) != 0x080) {
            _reportInvalidOther(c & 0xFF, _inputPtr);
        }
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        c = (int) _inputBuffer[_inputPtr++];
        if ((c & 0xC0) != 0x080) {
            _reportInvalidOther(c & 0xFF, _inputPtr);
        }
    }

    private final void _skipUtf8_4(int c)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
    }

    /*
    /***************************************************
    /* Internal methods, input loading
    /***************************************************
     */

    /**
     * We actually need to check the character value here
     * (to see if we have \n following \r).
     */
    protected final void _skipCR() throws IOException
    {
        if (_inputPtr < _inputEnd || loadMore()) {
            if (_inputBuffer[_inputPtr] == BYTE_LF) {
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

    private int nextByte()
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        return _inputBuffer[_inputPtr++] & 0xFF;
    }

    /*
    /***************************************************
    /* Internal methods, error reporting
    /***************************************************
     */

    protected void _reportInvalidChar(int c)
        throws JsonParseException
        {
            // Either invalid WS or illegal UTF-8 start char
            if (c < INT_SPACE) {
                _throwInvalidSpace(c);
            }
            _reportInvalidInitial(c);
        }

    protected void _reportInvalidInitial(int mask)
        throws JsonParseException
    {
        _reportError("Invalid UTF-8 start byte 0x"+Integer.toHexString(mask));
    }

    protected void _reportInvalidOther(int mask)
        throws JsonParseException
    {
        _reportError("Invalid UTF-8 middle byte 0x"+Integer.toHexString(mask));
    }

    protected void _reportInvalidOther(int mask, int ptr)
        throws JsonParseException
    {
        _inputPtr = ptr;
        _reportInvalidOther(mask);
    }

    public static int[] growArrayBy(int[] arr, int more)
    {
        if (arr == null) {
            return new int[more];
        }
        int[] old = arr;
        int len = arr.length;
        arr = new int[len + more];
        System.arraycopy(old, 0, arr, 0, len);
        return arr;
    }

    /*
    /***************************************************
    /* Binary access
    /***************************************************
     */

    @Override
    protected byte[] _decodeBase64(Base64Variant b64variant)
        throws IOException, JsonParseException
    {
        ByteArrayBuilder builder = _getByteArrayBuilder();

        /* !!! 23-Jan-2009, tatu: There are some potential problems
         *   with this:
         *
         * - Escaped chars are not handled. Should they?
         */

        //main_loop:
        while (true) {
            // first, we'll skip preceding white space, if any
            int ch;
            do {
                if (_inputPtr >= _inputEnd) {
                    loadMoreGuaranteed();
                }
                ch = (int) _inputBuffer[_inputPtr++] & 0xFF;
            } while (ch <= INT_SPACE);
            int bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) { // reached the end, fair and square?
                if (ch == INT_QUOTE) {
                    return builder.toByteArray();
                }
                throw reportInvalidChar(b64variant, ch, 0);
            }
            int decodedData = bits;
            
            // then second base64 char; can't get padding yet, nor ws
            
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            ch = _inputBuffer[_inputPtr++] & 0xFF;
            bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                throw reportInvalidChar(b64variant, ch, 1);
            }
            decodedData = (decodedData << 6) | bits;
            
            // third base64 char; can be padding, but not ws
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            ch = _inputBuffer[_inputPtr++] & 0xFF;
            bits = b64variant.decodeBase64Char(ch);

            // First branch: can get padding (-> 1 byte)
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    throw reportInvalidChar(b64variant, ch, 2);
                }
                // Ok, must get padding
                if (_inputPtr >= _inputEnd) {
                    loadMoreGuaranteed();
                }
                ch = _inputBuffer[_inputPtr++] & 0xFF;
                if (!b64variant.usesPaddingChar(ch)) {
                    throw reportInvalidChar(b64variant, ch, 3, "expected padding character '"+b64variant.getPaddingChar()+"'");
                }
                // Got 12 bits, only need 8, need to shift
                decodedData >>= 4;
                builder.append(decodedData);
                continue;
            }
            // Nope, 2 or 3 bytes
            decodedData = (decodedData << 6) | bits;
            // fourth and last base64 char; can be padding, but not ws
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            ch = _inputBuffer[_inputPtr++] & 0xFF;
            bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    throw reportInvalidChar(b64variant, ch, 3);
                }
                /* With padding we only get 2 bytes; but we have
                 * to shift it a bit so it is identical to triplet
                 * case with partial output.
                 * 3 chars gives 3x6 == 18 bits, of which 2 are
                 * dummies, need to discard:
                 */
                decodedData >>= 2;
                builder.appendTwoBytes(decodedData);
            } else {
                // otherwise, our triple is now complete
                decodedData = (decodedData << 6) | bits;
                builder.appendThreeBytes(decodedData);
            }
        }
    }

    protected IllegalArgumentException reportInvalidChar(Base64Variant b64variant, int ch, int bindex)
        throws IllegalArgumentException
    {
        return reportInvalidChar(b64variant, ch, bindex, null);
    }

    /**
     * @param bindex Relative index within base64 character unit; between 0
     *   and 3 (as unit has exactly 4 characters)
     */
    protected IllegalArgumentException reportInvalidChar(Base64Variant b64variant, int ch, int bindex, String msg)
        throws IllegalArgumentException
    {
        String base;
        if (ch <= INT_SPACE) {
            base = "Illegal white space character (code 0x"+Integer.toHexString(ch)+") as character #"+(bindex+1)+" of 4-char base64 unit: can only used between units";
        } else if (b64variant.usesPaddingChar(ch)) {
            base = "Unexpected padding character ('"+b64variant.getPaddingChar()+"') as character #"+(bindex+1)+" of 4-char base64 unit: padding only legal as 3rd or 4th character";
        } else if (!Character.isDefined(ch) || Character.isISOControl(ch)) {
            // Not sure if we can really get here... ? (most illegal xml chars are caught at lower level)
            base = "Illegal character (code 0x"+Integer.toHexString(ch)+") in base64 content";
        } else {
            base = "Illegal character '"+((char)ch)+"' (code 0x"+Integer.toHexString(ch)+") in base64 content";
        }
        if (msg != null) {
            base = base + ": " + msg;
        }
        return new IllegalArgumentException(base);
    }
}
