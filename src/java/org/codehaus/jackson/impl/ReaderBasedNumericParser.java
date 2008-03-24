package org.codehaus.jackson.impl;

import java.io.IOException;
import java.io.Reader;

import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

/**
 * Intermediate class that implements handling of numeric parsing.
 * Separate from the actual parser class just to isolate numeric
 * parsing: would be nice to use aggregation, but unfortunately
 * many parts are hard to implement without direct access to
 * underlying buffers.
 */
public abstract class ReaderBasedNumericParser
    extends ReaderBasedParserBase
{
    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    public ReaderBasedNumericParser(IOContext pc, Reader r)
    {
        super(pc, r);
    }

    /*
    ////////////////////////////////////////////////////
    // Textual parsing of number values
    ////////////////////////////////////////////////////
     */

    /**
     * Initial parsing method for number values. It needs to be able
     * to parse enough input to be able to determine whether the
     * value is to be considered a simple integer value, or a more
     * generic decimal value: latter of which needs to be expressed
     * as a floating point number. The basic rule is that if the number
     * has no fractional or exponential part, it is an integer; otherwise
     * a floating point number.
     *<p>
     * Because much of input has to be processed in any case, no partial
     * parsing is done: all input text will be stored for further
     * processing. However, actual numeric value conversion will be
     * deferred, since it is usually the most complicated and costliest
     * part of processing.
     */
    protected final JsonToken parseNumberText(int ch)
        throws IOException, JsonParseException
    {
        /* Although we will always be complete with respect to textual
         * representation (that is, all characters will be parsed),
         * actual conversion to a number is deferred. Thus, need to
         * note that no representations are valid yet
         */
        boolean negative = (ch == INT_MINUS);
        int ptr = mInputPtr;
        int startPtr = ptr-1; // to include sign/digit already read
        final int inputLen = mInputLast;

        dummy_loop:
        do { // dummy loop, to be able to break out
            if (negative) { // need to read the next digit
                if (ptr >= mInputLast) {
                    break dummy_loop;
                }
                ch = mInputBuffer[ptr++];
                // First check: must have a digit to follow minus sign
                if (ch > INT_9 || ch < INT_0) {
                    reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
                }
                /* (note: has been checked for non-negative already, in
                 * the dispatching code that determined it should be
                 * a numeric value)
                 */
            }

            /* First, let's see if the whole number is contained within
             * the input buffer unsplit. This should be the common case;
             * and to simplify processing, we will just reparse contents
             * in the alternative case (number split on buffer boundary)
             */
            
            int intLen = 1; // already got one
            
            // First let's get the obligatory integer part:
            
            int_loop:
            while (true) {
                if (ptr >= mInputLast) {
                    break dummy_loop;
                }
                ch = (int) mInputBuffer[ptr++];
                if (ch < INT_0 || ch > INT_9) {
                    break int_loop;
                }
                // The only check: no leading zeroes
                if (++intLen == 2) { // To ensure no leading zeroes
                    if (mInputBuffer[ptr-2] == '0') {
                        reportInvalidNumber("Leading zeroes not allowed");
                    }
                }
            }

            int fractLen = 0;
            
            // And then see if we get other parts
            if (ch == INT_DECIMAL_POINT) { // yes, fraction
                fract_loop:
                while (true) {
                    if (ptr >= inputLen) {
                        break dummy_loop;
                    }
                    ch = (int) mInputBuffer[ptr++];
                    if (ch < INT_0 || ch > INT_9) {
                        break fract_loop;
                    }
                    ++fractLen;
                }
                // must be followed by sequence of ints, one minimum
                if (fractLen == 0) {
                    reportUnexpectedNumberChar(ch, "Decimal point not followed by a digit");
                }
            }

            int expLen = 0;
            if (ch == INT_e || ch == INT_E) { // and/or expontent
                if (ptr >= inputLen) {
                    break dummy_loop;
                }
                // Sign indicator?
                ch = (int) mInputBuffer[ptr++];
                if (ch == INT_MINUS || ch == INT_PLUS) { // yup, skip for now
                    if (ptr >= inputLen) {
                        break dummy_loop;
                    }
                    ch = (int) mInputBuffer[ptr++];
                }
                while (ch <= INT_9 && ch >= INT_0) {
                    ++expLen;
                    if (ptr >= inputLen) {
                        break dummy_loop;
                    }
                    ch = (int) mInputBuffer[ptr++];
                }
                // must be followed by sequence of ints, one minimum
                if (expLen == 0) {
                    reportUnexpectedNumberChar(ch, "Exponent indicator not followed by a digit");
                }
            }

            // Got it all: let's add to text buffer for parsing, access
            --ptr; // need to push back following separator
            mInputPtr = ptr;
            int len = ptr-startPtr;
            mTextBuffer.resetWithShared(mInputBuffer, startPtr, len);
            return reset(negative, intLen, fractLen, expLen);
        } while (false);

        mInputPtr = negative ? (startPtr+1) : startPtr;
        return parseNumberText2(negative);
    }

    /**
     * Method called to parse a number, when the primary parse
     * method has failed to parse it, due to it being split on
     * buffer boundary. As a result code is very similar, except
     * that it has to explicitly copy contents to the text buffer
     * instead of just sharing the main input buffer.
     */
    private final JsonToken parseNumberText2(boolean negative)
        throws IOException, JsonParseException
    {
        mTextBuffer.resetWithEmpty();
        char[] outBuf = mTextBuffer.getCurrentSegment();
        int outPtr = 0;

        // Need to prepend sign?
        if (negative) {
            outBuf[outPtr++] = '-';
        }

        char c;
        int intLen = 0;
        boolean eof = false;

        // Ok, first the obligatory integer part:
        int_loop:
        while (true) {
            if (mInputPtr >= mInputLast && !loadMore()) {
                // EOF is legal for main level int values
                c = CHAR_NULL;
                eof = true;
                break int_loop;
            }
            c = mInputBuffer[mInputPtr++];
            if (c < INT_0 || c > INT_9) {
                break int_loop;
            }
            ++intLen;
            // Quickie check: no leading zeroes allowed
            if (intLen == 2) {
                if (outBuf[outPtr-1] == '0') {
                    reportInvalidNumber("Leading zeroes not allowed");
                }
            }
            if (outPtr >= outBuf.length) {
                outBuf = mTextBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            outBuf[outPtr++] = c;
        }
        // Also, integer part is not optional
        if (intLen == 0) {
            reportInvalidNumber("Missing integer part (next char "+getCharDesc(c)+")");
        }

        int fractLen = 0;
        // And then see if we get other parts
        if (c == '.') { // yes, fraction
            outBuf[outPtr++] = c;

            fract_loop:
            while (true) {
                if (mInputPtr >= mInputLast && !loadMore()) {
                    eof = true;
                    break fract_loop;
                }
                c = mInputBuffer[mInputPtr++];
                if (c < INT_0 || c > INT_9) {
                    break fract_loop;
                }
                ++fractLen;
                if (outPtr >= outBuf.length) {
                    outBuf = mTextBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outBuf[outPtr++] = c;
            }
            // must be followed by sequence of ints, one minimum
            if (fractLen == 0) {
                reportUnexpectedNumberChar(c, "Decimal point not followed by a digit");
            }
        }

        int expLen = 0;
        if (c == 'e' || c == 'E') { // exponent?
            if (outPtr >= outBuf.length) {
                outBuf = mTextBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            outBuf[outPtr++] = c;
            // Not optional, can require that we get one more char
            c = (mInputPtr < mInputLast) ? mInputBuffer[mInputPtr++]
                : getNextChar("expected a digit for number exponent");
            // Sign indicator?
            if (c == '-' || c == '+') {
                if (outPtr >= outBuf.length) {
                    outBuf = mTextBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outBuf[outPtr++] = c;
                // Likewise, non optional:
                c = (mInputPtr < mInputLast) ? mInputBuffer[mInputPtr++]
                    : getNextChar("expected a digit for number exponent");
            }

            exp_loop:
            while (c <= INT_9 && c >= INT_0) {
                ++expLen;
                if (outPtr >= outBuf.length) {
                    outBuf = mTextBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outBuf[outPtr++] = c;
                if (mInputPtr >= mInputLast && !loadMore()) {
                    eof = true;
                    break exp_loop;
                }
                c = mInputBuffer[mInputPtr++];
            }
            // must be followed by sequence of ints, one minimum
            if (expLen == 0) {
                reportUnexpectedNumberChar(c, "Exponent indicator not followed by a digit");
            }
        }

        // Ok; unless we hit end-of-input, need to push last char read back
        if (!eof) {
            --mInputPtr;
        }
        mTextBuffer.setCurrentLength(outPtr);

        // And there we have it!
        return reset(negative, intLen, fractLen, expLen);
    }

}
