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
        if (mTokenIncomplete) {
            mTokenIncomplete = false;
            skipString(); // only strings can be partial
        }

        int i;

        // Space to skip?
        while (true) {
            if (mInputPtr >= mInputLast) {
                if (!loadMore()) {
                    handleEOF();
                    return (mCurrToken = null);
                }
            }
            i = (int) mInputBuffer[mInputPtr++];
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
        mTokenInputTotal = mCurrInputProcessed + mInputPtr - 1;
        mTokenInputRow = mCurrInputRow;
        mTokenInputCol = mInputPtr - mCurrInputRowStart - 1;

        // Closing scope?
        if (i == INT_RBRACKET) {
            if (!mParsingContext.isArray()) {
                reportMismatchedEndMarker(i, ']');
            }
            mParsingContext = mParsingContext.getParent();
            return (mCurrToken = JsonToken.END_ARRAY);
        }
        if (i == INT_RCURLY) {
            if (!mParsingContext.isObject()) {
                reportMismatchedEndMarker(i, '}');
            }
            mParsingContext = mParsingContext.getParent();
            return (mCurrToken = JsonToken.END_OBJECT);
        }

        // Nope. Have and/or need a separator?
        int sep = mParsingContext.handleSeparator(i);

        switch (sep) {
        case HANDLED_EXPECT_NAME:
        case HANDLED_EXPECT_VALUE:
            // Need to skip space, find next char
            while (true) {
                if (mInputPtr >= mInputLast) {
                    if (!loadMore()) {
                        reportError("Unexpected end-of-input within/between "+mParsingContext.getTypeDesc()+" entries");
                    }
                }
                i = (int) mInputBuffer[mInputPtr++];
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
            reportUnexpectedChar(i, "was expecting comma to separate "+mParsingContext.getTypeDesc()+" entries");
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
            //mParsingContext = mParsingContext.createChildArrayContext(this);
            mParsingContext = mParsingContext.createChildArrayContext(mTokenInputRow, mTokenInputCol);
            return (mCurrToken = JsonToken.START_ARRAY);
        case INT_LCURLY:
            //mParsingContext = mParsingContext.createChildObjectContext(this);
            mParsingContext = mParsingContext.createChildObjectContext(mTokenInputRow, mTokenInputCol);
            return (mCurrToken = JsonToken.START_OBJECT);
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
    // Internal methods, secondary parsing
    ////////////////////////////////////////////////////
     */

    protected JsonToken handleFieldName(int i)
        throws IOException, JsonParseException
    {
        if (i != INT_QUOTE) {
            reportUnexpectedChar(i, "was expecting double-quote to start field name");
        }
        mFieldInBuffer = false; // by default let's expect it won't get there

        /* First: let's try to see if we have a simple name: one that does
         * not cross input buffer boundary, and does not contain escape
         * sequences.
         */
        int ptr = mInputPtr;
        int hash = 0;
        final int inputLen = mInputLast;

        if (ptr < inputLen) {
            final int[] codes = CharTypes.getInputCode();
            final int maxCode = codes.length;

            do {
                int ch = mInputBuffer[ptr];
                if (ch < maxCode && codes[ch] != 0) {
                    if (ch == '"') {
                        int start = mInputPtr;
                        mInputPtr = ptr+1; // to skip the quote
                        String name = mSymbols.findSymbol(mInputBuffer, start, ptr - start, hash);
                        mParsingContext.setCurrentName(name);
                        return (mCurrToken = JsonToken.FIELD_NAME);
                    }
                    break;
                }
                hash = (hash * 31) + ch;
                ++ptr;
            } while (ptr < inputLen);
        }

        int start = mInputPtr;
        mInputPtr = ptr;
        return handleFieldName2(start, hash);
    }

    private JsonToken handleFieldName2(int startPtr, int hash)
        throws IOException, JsonParseException
    {
        mTextBuffer.resetWithShared(mInputBuffer, startPtr, (mInputPtr - startPtr));

        /* Output pointers; calls will also ensure that the buffer is
         * not shared and has room for at least one more char.
         */
        char[] outBuf = mTextBuffer.getCurrentSegment();
        int outPtr = mTextBuffer.getCurrentSegmentSize();

        while (true) {
            if (mInputPtr >= mInputLast) {
                if (!loadMore()) {
                    reportInvalidEOF(": was expecting closing quote for name");
                }
            }
            char c = mInputBuffer[mInputPtr++];
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
                outBuf = mTextBuffer.finishCurrentSegment();
                outPtr = 0;
            }
        }
        mTextBuffer.setCurrentLength(outPtr);
        {
            mFieldInBuffer = true; // yep, is now stored in text buffer
            TextBuffer tb = mTextBuffer;
            char[] buf = tb.getTextBuffer();
            int start = tb.getTextOffset();
            int len = tb.size();

            mParsingContext.setCurrentName(mSymbols.findSymbol(buf, start, len, hash));
        }
        return (mCurrToken = JsonToken.FIELD_NAME);
    }

    protected JsonToken startString()
        throws IOException, JsonParseException
    {
        /* First: let's try to see if we have simple String value: one
         * that does not cross input buffer boundary, and does not
         * contain escape sequences.
         */
        int ptr = mInputPtr;
        final int inputLen = mInputLast;

        if (ptr < inputLen) {
            final int[] codes = CharTypes.getInputCode();
            final int maxCode = codes.length;

            do {
                int ch = mInputBuffer[ptr];
                if (ch < maxCode && codes[ch] != 0) {
                    if (ch == '"') {
                        mTextBuffer.resetWithShared(mInputBuffer, mInputPtr, (ptr-mInputPtr));
                        mInputPtr = ptr+1;
                        return (mCurrToken = JsonToken.VALUE_STRING);
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
        mTextBuffer.resetWithShared(mInputBuffer, mInputPtr, (ptr-mInputPtr));
        mInputPtr = ptr;
        mTokenIncomplete = true;
        return (mCurrToken = JsonToken.VALUE_STRING);
    }

    protected void finishString()
        throws IOException, JsonParseException
    {
        /* Output pointers; calls will also ensure that the buffer is
         * not shared and has room for at least one more char.
         */
        char[] outBuf = mTextBuffer.getCurrentSegment();
        int outPtr = mTextBuffer.getCurrentSegmentSize();

        while (true) {
            if (mInputPtr >= mInputLast) {
                if (!loadMore()) {
                    reportInvalidEOF(": was expecting closing quote for a string value");
                }
            }
            char c = mInputBuffer[mInputPtr++];
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
                outBuf = mTextBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;
        }
        mTextBuffer.setCurrentLength(outPtr);
    }

    /**
     * Method called to skim through rest of unparsed String value,
     * if it is not needed. This can be done bit faster if contents
     * need not be stored for future access.
     */
    protected void skipString()
        throws IOException, JsonParseException
    {
        int inputPtr = mInputPtr;
        int inputLen = mInputLast;
        char[] inputBuffer = mInputBuffer;

        while (true) {
            if (inputPtr >= inputLen) {
                mInputPtr = inputPtr;
                if (!loadMore()) {
                    reportInvalidEOF(": was expecting closing quote for a string value");
                }
                inputPtr = mInputPtr;
                inputLen = mInputLast;
            }
            char c = inputBuffer[inputPtr++];
            int i = (int) c;
            if (i <= INT_BACKSLASH) {
                if (i == INT_BACKSLASH) {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    mInputPtr = inputPtr;
                    c = decodeEscaped();
                    inputPtr = mInputPtr;
                    inputLen = mInputLast;
                } else if (i <= INT_QUOTE) {
                    if (i == INT_QUOTE) {
                        mInputPtr = inputPtr;
                        break;
                    }
                    if (i < INT_SPACE) {
                        mInputPtr = inputPtr;
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
            if (mInputPtr >= mInputLast) {
                if (!loadMore()) {
                    reportInvalidEOF(" in a value");
                }
            }
            char c = mInputBuffer[mInputPtr];
            if (c != matchStr.charAt(i)) {
                reportInvalidToken(matchStr.substring(0, i));
            }
            ++mInputPtr;
        }
        /* Ok, fine; let's not bother checking anything beyond keyword.
         * If there's something wrong there, it'll cause a parsing
         * error later on.
         */
        return (mCurrToken = token);
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
            if (mInputPtr >= mInputLast) {
                if (!loadMore()) {
                    break;
                }
            }
            char c = mInputBuffer[mInputPtr];
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            ++mInputPtr;
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
        if (mInputPtr >= mInputLast) {
            if (!loadMore()) {
                reportInvalidEOF(" in character escape sequence");
            }
        }
        char c = mInputBuffer[mInputPtr++];

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
            if (mInputPtr >= mInputLast) {
                if (!loadMore()) {
                    reportInvalidEOF(" in character escape sequence");
                }
            }
            int ch = (int) mInputBuffer[mInputPtr++];
            int digit = CharTypes.charToHex(ch);
            if (digit < 0) {
                reportUnexpectedChar(ch, "expected a hex-digit for character escape sequence");
            }
            value = (value << 4) | digit;
        }
        return (char) value;
    }
}
