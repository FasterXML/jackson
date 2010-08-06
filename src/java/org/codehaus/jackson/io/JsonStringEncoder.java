package org.codehaus.jackson.io;

import java.lang.ref.SoftReference;

import org.codehaus.jackson.util.BufferRecycler;
import org.codehaus.jackson.util.ByteArrayBuilder;
import org.codehaus.jackson.util.CharTypes;
import org.codehaus.jackson.util.TextBuffer;

/**
 * Helper class used for efficient encoding of JSON String values (including
 * JSON field names) into Strings or UTF-8 byte arrays.
 *
 * @since 1.6
 */
public final class JsonStringEncoder
{
    private final static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftRerefence}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final protected static ThreadLocal<SoftReference<JsonStringEncoder>> _threadEncoder
        = new ThreadLocal<SoftReference<JsonStringEncoder>>();

    /**
     * Lazily constructed text buffer used to produce JSON encoded Strings
     * as characters (without UTF-8 encoding)
     */
    protected TextBuffer _textBuffer;

    /**
     * Lazily-constructed builder used for UTF-8 encoding of text values
     * (quoted and unquoted)
     */
    protected ByteArrayBuilder _byteBuilder;
    
    /**
     * Temporary buffer used for composing quote/escape sequences
     */
    protected final char[] _quoteBuffer = new char[6];
    
    /*
    /**********************************************************
    /* Construction, instance access
    /**********************************************************
     */
    
    public JsonStringEncoder() { }
    
    /**
     * Factory method for getting an instance; this is either recycled per-thread instance,
     * or a newly constructed one.
     */
    public static JsonStringEncoder getInstance()
    {
        SoftReference<JsonStringEncoder> ref = _threadEncoder.get();
        JsonStringEncoder enc = (ref == null) ? null : ref.get();

        if (enc == null) {
            enc = new JsonStringEncoder();
            _threadEncoder.set(new SoftReference<JsonStringEncoder>(enc));
        }
        return enc;
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    /**
     * Method that will quote text contents using JSON standard quoting,
     * and return results as a character array
     */
    public char[] quoteAsString(String input)
    {
        TextBuffer textBuffer = _textBuffer;
        if (textBuffer == null) {
            // no allocator; can add if we must, shouldn't need to
            _textBuffer = textBuffer = new TextBuffer(null);
        }
        char[] outputBuffer = textBuffer.emptyAndGetCurrentSegment();
        final int[] escCodes = CharTypes.getOutputEscapes();
        final int escCodeCount = escCodes.length;
        int inPtr = 0;
        final int inputLen = input.length();
        int outPtr = 0;
 
        outer_loop:
        while (inPtr < inputLen) {
            tight_loop:
            while (true) {
                char c = input.charAt(inPtr);
                if (c < escCodeCount && escCodes[c] != 0) {
                    break tight_loop;
                }
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outputBuffer[outPtr++] = c;
                if (++inPtr >= inputLen) {
                    break outer_loop;
                }
            }
            // something to escape; 2 or 6-char variant? 
            int escCode = escCodes[input.charAt(inPtr++)];
            int length = _appendSingleEscape(escCode, _quoteBuffer);
            if ((outPtr + length) > outputBuffer.length) {
                int first = outputBuffer.length - outPtr;
                if (first > 0) {
                    System.arraycopy(_quoteBuffer, 0, outputBuffer, outPtr, first);
                }
                outputBuffer = textBuffer.finishCurrentSegment();
                int second = length - first;
                System.arraycopy(_quoteBuffer, first, outputBuffer, outPtr, second);
                outPtr += second;
            } else {
                System.arraycopy(_quoteBuffer, 0, outputBuffer, outPtr, length);
                outPtr += length;
            }
            
        }
        textBuffer.setCurrentLength(outPtr);
        return textBuffer.contentsAsArray();
    }

    /**
     * Will quote given JSON String value using standard quoting, encode
     * results as UTF-8, and return result as a byte array.
     */
    public byte[] quoteAsUTF8(String text)
    {
        ByteArrayBuilder byteBuilder = _byteBuilder;
        if (byteBuilder == null) {
            // no allocator; can add if we must, shouldn't need to
            _byteBuilder = byteBuilder = new ByteArrayBuilder(null);
        }
        // !!! TBI
        return null;
    }

    /**
     * Will encode given String as UTF-8 (without any quoting), return
     * resulting byte array.
     */
    public byte[] encodeAsUTF8(String text)
    {
        // !!! TBI
        return null;
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private int _appendSingleEscape(int escCode, char[] quoteBuffer)
    {
        if (quoteBuffer == null) {
            quoteBuffer = new char[6];
            quoteBuffer[0] = '\\';
            quoteBuffer[2] = '0';
            quoteBuffer[3] = '0';
        }
        if (escCode < 0) { // control char, value -(char + 1)
            int value = -(escCode + 1);
            quoteBuffer[1] = 'u';
            // We know it's a control char, so only the last 2 chars are non-0
            quoteBuffer[4] = HEX_CHARS[value >> 4];
            quoteBuffer[5] = HEX_CHARS[value & 0xF];
            return 6;
        }
        quoteBuffer[1] = (char) escCode;
        return 2;
    }
}
