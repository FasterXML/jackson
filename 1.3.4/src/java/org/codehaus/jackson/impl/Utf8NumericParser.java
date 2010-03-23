package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

/**
 * Intermediate class that implements handling of numeric parsing,
 * when using UTF-8 encoded byte-based input source.
 * Separate from the actual parser class just to isolate numeric
 * parsing: would be nice to use aggregation, but unfortunately
 * many parts are hard to implement without direct access to
 * underlying buffers.
 */
public abstract class Utf8NumericParser
    extends StreamBasedParserBase
{
    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    public Utf8NumericParser(IOContext pc, int features,
                             InputStream in,
                             byte[] inputBuffer, int start, int end,
                             boolean bufferRecyclable)
    {
        super(pc, features, in, inputBuffer, start, end, bufferRecyclable);
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
    @Override
    protected final JsonToken parseNumberText(int c)
        throws IOException, JsonParseException
    {
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int outPtr = 0;
        boolean negative = (c == INT_MINUS);

        // Need to prepend sign?
        if (negative) {
            outBuf[outPtr++] = '-';
            // Must have something after sign too
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            c = (int) _inputBuffer[_inputPtr++] & 0xFF;
        }

        int intLen = 0;
        boolean eof = false;

        // Ok, first the obligatory integer part:
        int_loop:
        while (true) {
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
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            outBuf[outPtr++] = (char) c;
            if (_inputPtr >= _inputEnd && !loadMore()) {
                // EOF is legal for main level int values
                c = CHAR_NULL;
                eof = true;
                break int_loop;
            }
            c = (int) _inputBuffer[_inputPtr++] & 0xFF;
        }
        // Also, integer part is not optional
        if (intLen == 0) {
            reportInvalidNumber("Missing integer part (next char "+_getCharDesc(c)+")");
        }

        int fractLen = 0;
        // And then see if we get other parts
        if (c == '.') { // yes, fraction
            outBuf[outPtr++] = (char) c;

            fract_loop:
            while (true) {
                if (_inputPtr >= _inputEnd && !loadMore()) {
                    eof = true;
                    break fract_loop;
                }
                c = (int) _inputBuffer[_inputPtr++] & 0xFF;
                if (c < INT_0 || c > INT_9) {
                    break fract_loop;
                }
                ++fractLen;
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outBuf[outPtr++] = (char) c;
            }
            // must be followed by sequence of ints, one minimum
            if (fractLen == 0) {
                reportUnexpectedNumberChar(c, "Decimal point not followed by a digit");
            }
        }

        int expLen = 0;
        if (c == 'e' || c == 'E') { // exponent?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            outBuf[outPtr++] = (char) c;
            // Not optional, can require that we get one more char
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            c = (int) _inputBuffer[_inputPtr++] & 0xFF;
            // Sign indicator?
            if (c == '-' || c == '+') {
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outBuf[outPtr++] = (char) c;
                // Likewise, non optional:
                if (_inputPtr >= _inputEnd) {
                    loadMoreGuaranteed();
                }
                c = (int) _inputBuffer[_inputPtr++] & 0xFF;
            }

            exp_loop:
            while (c <= INT_9 && c >= INT_0) {
                ++expLen;
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outBuf[outPtr++] = (char) c;
                if (_inputPtr >= _inputEnd && !loadMore()) {
                    eof = true;
                    break exp_loop;
                }
                c = (int) _inputBuffer[_inputPtr++] & 0xFF;
            }
            // must be followed by sequence of ints, one minimum
            if (expLen == 0) {
                reportUnexpectedNumberChar(c, "Exponent indicator not followed by a digit");
            }
        }

        // Ok; unless we hit end-of-input, need to push last char read back
        if (!eof) {
            --_inputPtr;
        }
        _textBuffer.setCurrentLength(outPtr);

        // And there we have it!
        return reset(negative, intLen, fractLen, expLen);
    }
}
