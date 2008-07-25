package org.codehaus.jackson.impl;

import java.io.*;
import java.math.BigDecimal;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.*;
import org.codehaus.jackson.util.CharTypes;

public final class WriterBasedGenerator
    extends JsonGeneratorBase
{
    final static int SHORT_WRITE = 32;

    final static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    final protected IOContext mIOContext;

    final protected Writer mWriter;

    /*
    ////////////////////////////////////////////////////
    // Output buffering
    ////////////////////////////////////////////////////
     */

    /**
     * Intermediate buffer in which contents are buffered before
     * being written using {@link #mWriter}.
     */
    protected char[] mOutputBuffer;

    protected int mOutputHead = 0;

    protected int mOutputTail = 0;

    protected int mOutputEnd;

    /**
     * 6-char temporary buffer allocated if needed, for constructing
     * escape sequences
     */
    protected char[] mEntityBuffer;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    public WriterBasedGenerator(IOContext ctxt, Writer w)
    {
        super();
        mIOContext = ctxt;
        mWriter = w;
        mOutputBuffer = ctxt.allocConcatBuffer();
        mOutputEnd = mOutputBuffer.length;
    }

    /*
    ////////////////////////////////////////////////////
    // Output method implementations, structural
    ////////////////////////////////////////////////////
     */

    protected void doWriteStartArray()
        throws IOException, JsonGenerationException
    {
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = '[';
    }

    protected void doWriteEndArray()
        throws IOException, JsonGenerationException
    {
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = ']';
    }

    protected void doWriteStartObject()
        throws IOException, JsonGenerationException
    {
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = '{';
    }

    protected void doWriteEndObject()
        throws IOException, JsonGenerationException
    {
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = '}';
    }

    public void doWriteFieldName(String name, boolean commaBefore)
        throws IOException, JsonGenerationException
    {
        if (mPrettyPrinter != null) {
            if (commaBefore) {
                mPrettyPrinter.writeObjectEntrySeparator(this);
            } else {
                mPrettyPrinter.beforeObjectEntries(this);
            }
        } else {
            if (commaBefore) {
                if (mOutputTail >= mOutputEnd) {
                    flushBuffer();
                }
                mOutputBuffer[mOutputTail++] = ',';
            }
        }
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = '"';
        doWriteString(name);
        // And finally, closing quotes
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = '"';
    }

    /*
    ////////////////////////////////////////////////////
    // Output method implementations, textual
    ////////////////////////////////////////////////////
     */

    public void writeString(String text)
        throws IOException, JsonGenerationException
    {
        verifyValueWrite("write text value");
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = '"';
        doWriteString(text);
        // And finally, closing quotes
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = '"';
    }

    public void writeString(char[] text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        verifyValueWrite("write text value");
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = '"';
        doWriteString(text, offset, len);
        // And finally, closing quotes
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = '"';
    }

    public void writeRaw(String text)
        throws IOException, JsonGenerationException
    {
        // Nothing to check, can just output as is
        int len = text.length();
        int room = mOutputEnd - mOutputTail;

        if (room == 0) {
            flushBuffer();
            room = mOutputEnd - mOutputTail;
        }
        // But would it nicely fit in? If yes, it's easy
        if (room >= len) {
            text.getChars(0, len, mOutputBuffer, mOutputTail);
            mOutputTail += len;
        } else {
            writeRawLong(text);
        }
    }

    public void writeRaw(String text, int start, int len)
        throws IOException, JsonGenerationException
    {
        // Nothing to check, can just output as is
        int room = mOutputEnd - mOutputTail;

        if (room < len) {
            flushBuffer();
            room = mOutputEnd - mOutputTail;
        }
        // But would it nicely fit in? If yes, it's easy
        if (room >= len) {
            text.getChars(start, start+len, mOutputBuffer, mOutputTail);
            mOutputTail += len;
        } else {            	
            writeRawLong(text.substring(start, start+len));
        }
    }

    public void writeRaw(char[] text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        // Only worth buffering if it's a short write?
        if (len < SHORT_WRITE) {
            int room = mOutputEnd - mOutputTail;
            if (len > room) {
                flushBuffer();
            }
            System.arraycopy(text, offset, mOutputBuffer, mOutputTail, len);
            mOutputTail += len;
            return;
        }
        // Otherwise, better just pass through:
        flushBuffer();
        mWriter.write(text, offset, len);
    }

    public void writeRaw(char c)
        throws IOException, JsonGenerationException
    {
        if (mOutputTail >= mOutputEnd) {
            flushBuffer();
        }
        mOutputBuffer[mOutputTail++] = c;
    }

    public void writeBinary(byte[] data, int offset, int len)
        throws IOException, JsonGenerationException
    {
        // !!! TBI: base64-based binary output

        throw new RuntimeException("Not yet implemented");
    }

    private void writeRawLong(String text)
        throws IOException, JsonGenerationException
    {
        int room = mOutputEnd - mOutputTail;
        // If not, need to do it by looping
        text.getChars(0, room, mOutputBuffer, mOutputTail);
        mOutputTail += room;
        flushBuffer();
        int offset = room;
        int len = text.length() - room;

        while (len > mOutputEnd) {
            int amount = mOutputEnd;
            text.getChars(offset, offset+amount, mOutputBuffer, 0);
            mOutputHead = 0;
            mOutputTail = amount;
            flushBuffer();
            offset += amount;
            len -= amount;
        }
        // And last piece (at most length of buffer)
        text.getChars(offset, offset+len, mOutputBuffer, 0);
        mOutputHead = 0;
        mOutputTail = len;
    }

    /*
    ////////////////////////////////////////////////////
    // Output method implementations, primitive
    ////////////////////////////////////////////////////
     */

    public void writeNumber(int i)
        throws IOException, JsonGenerationException
    {
        verifyValueWrite("write number");
        // up to 10 digits, minus sign
        if ((mOutputTail + 11) >= mOutputEnd) {
            flushBuffer();
        }
        mOutputTail = NumberOutput.outputInt(i, mOutputBuffer, mOutputTail);
    }

    public void writeNumber(long l)
        throws IOException, JsonGenerationException
    {
        // up to 20 digits, minus sign
        verifyValueWrite("write number");
        if ((mOutputTail + 21) >= mOutputEnd) {
            flushBuffer();
        }
        mOutputTail = NumberOutput.outputLong(l, mOutputBuffer, mOutputTail);
    }

    public void writeNumber(double d)
        throws IOException, JsonGenerationException
    {
        // What is the max length for doubles? 40 chars?
        verifyValueWrite("write number");

        // !!! TODO: use a more efficient printing method?
        writeRaw(String.valueOf(d));
    }

    public void writeNumber(float f)
        throws IOException, JsonGenerationException
    {
        // What is the max length for floats?
        verifyValueWrite("write number");

        // !!! TODO: use a more efficient printing method?
        writeRaw(String.valueOf(f));
    }

    public void writeNumber(BigDecimal dec)
        throws IOException, JsonGenerationException
    {
        // Don't really know max length for big decimal, no point checking
        verifyValueWrite("write number");

        // !!! TODO: use a more efficient printing method?
        writeRaw(dec.toString());
    }

    public void writeNumber(String encodedValue)
        throws IOException, JsonGenerationException
    {
        verifyValueWrite("write number");
        writeRaw(encodedValue);
    }

    public void writeBoolean(boolean state)
        throws IOException, JsonGenerationException
    {
        verifyValueWrite("write boolean value");
        if ((mOutputTail + 5) >= mOutputEnd) {
            flushBuffer();
        }
        int ptr = mOutputTail;
        char[] buf = mOutputBuffer;
        if (state) {
            buf[ptr] = 't';
            buf[++ptr] = 'r';
            buf[++ptr] = 'u';
            buf[++ptr] = 'e';
        } else {
            buf[ptr] = 'f';
            buf[++ptr] = 'a';
            buf[++ptr] = 'l';
            buf[++ptr] = 's';
            buf[++ptr] = 'e';
        }
        mOutputTail = ptr+1;
    }

    public void writeNull()
        throws IOException, JsonGenerationException
    {
        verifyValueWrite("write null value");
        if ((mOutputTail + 4) >= mOutputEnd) {
            flushBuffer();
        }
        int ptr = mOutputTail;
        char[] buf = mOutputBuffer;
        buf[ptr] = 'n';
        buf[++ptr] = 'u';
        buf[++ptr] = 'l';
        buf[++ptr] = 'l';
        mOutputTail = ptr+1;
    }

    /*
    ////////////////////////////////////////////////////
    // Implementations for other methods
    ////////////////////////////////////////////////////
     */

    protected final void verifyValueWrite(String typeMsg)
        throws IOException, JsonGenerationException
    {
        int status = mWriteContext.writeValue();
        if (status == JsonWriteContext.STATUS_EXPECT_NAME) {
            reportError("Can not "+typeMsg+", expecting field name");
        }

        if (mPrettyPrinter == null) {
            char c;
            switch (status) {
            case JsonWriteContext.STATUS_OK_AFTER_COMMA:
                c = ',';
                break;
            case JsonWriteContext.STATUS_OK_AFTER_COLON:
                c = ':';
                break;
            case JsonWriteContext.STATUS_OK_AFTER_SPACE:
                c = ' ';
                break;
            case JsonWriteContext.STATUS_OK_AS_IS:
            default:
                return;
            }
            if (mOutputTail >= mOutputEnd) {
                flushBuffer();
            }
            mOutputBuffer[mOutputTail] = c;
            ++mOutputTail;
            return;
        }

        // If we have a pretty printer, it knows what to do:
        switch (status) {
        case JsonWriteContext.STATUS_OK_AFTER_COMMA: // array
            mPrettyPrinter.writeArrayValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AFTER_COLON:
            mPrettyPrinter.writeObjectFieldValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AFTER_SPACE:
            mPrettyPrinter.writeRootValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AS_IS:
            // First entry, but of which context?
            if (mWriteContext.inArray()) {
                mPrettyPrinter.beforeArrayValues(this);
            } else if (mWriteContext.inObject()) {
                mPrettyPrinter.beforeObjectEntries(this);
            }
            break;
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Low-level output handling
    ////////////////////////////////////////////////////
     */

    @Override
    public final void flush()
        throws IOException
    {
        flushBuffer();
        mWriter.flush();
    }

    @Override
    public void close()
        throws IOException
    {
        flushBuffer();
        /* Note: writer is responsible for its own buffers (acquired
         * using processing context), and will close them as appropriate.
         */
        mWriter.close();
        // Also, internal buffer(s) can now be released as well
        releaseBuffers();
    }

    @Override
    protected void releaseBuffers()
    {
        char[] buf = mOutputBuffer;
        if (buf != null) {
            mOutputBuffer = null;
            mIOContext.releaseConcatBuffer(buf);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, low-level writing
    ////////////////////////////////////////////////////
     */

    private void doWriteString(String text)
        throws IOException, JsonGenerationException
    {
        /* One check first: if String won't fit in the buffer, let's
         * segment writes. No point in extending buffer to huge sizes
         * (like if someone wants to include multi-megabyte base64
         * encoded stuff or such)
         */
        int len = text.length();
        if (len > mOutputEnd) { // Let's reserve space for entity at begin/end
            doWriteLongString(text);
            return;
        }

        // Ok: we know String will fit in buffer ok
        // But do we need to flush first?
        if ((mOutputTail + len) > mOutputEnd) {
            flushBuffer();
        }
        text.getChars(0, len, mOutputBuffer, mOutputTail);

        // And then we'll need to verify need for escaping etc:
        int end = mOutputTail + len;
        final int[] escCodes = CharTypes.getOutputEscapes();
        final int escLen = escCodes.length;

        output_loop:
        while (mOutputTail < end) {
            // Fast loop for chars not needing escaping
            escape_loop:
            while (true) {
                char c = mOutputBuffer[mOutputTail];
                if (c < escLen && escCodes[c] != 0) {
                    break escape_loop;
                }
                if (++mOutputTail >= end) {
                    break output_loop;
                }
            }

            // Ok, bumped into something that needs escaping.
            /* First things first: need to flush the buffer.
             * Inlined, as we don't want to lose tail pointer
             */
            int flushLen = (mOutputTail - mOutputHead);
            if (flushLen > 0) {
                mWriter.write(mOutputBuffer, mOutputHead, flushLen);
            }
            /* In any case, tail will be the new start, so hopefully
             * we have room now.
             */
            {
                int escCode = escCodes[mOutputBuffer[mOutputTail]];
                ++mOutputTail;
                int needLen = (escCode < 0) ? 6 : 2;
                // If not, need to call separate method (note: buffer is empty now)
                if (needLen > mOutputTail) {
                    mOutputHead = mOutputTail;
                    writeSingleEscape(escCode);
                } else {
                    // But if it fits, can just prepend to buffer
                    int ptr = mOutputTail - needLen;
                    mOutputHead = ptr;
                    appendSingleEscape(escCode, mOutputBuffer, ptr);
                }
            }
        }
    }

    /**
     * Method called to write "long strings", strings whose length exceeds
     * output buffer length.
     */
    private void doWriteLongString(String text)
        throws IOException, JsonGenerationException
    {
        // First things first: let's flush the buffer to get some more room
        flushBuffer();

        // Then we can write 
        final int textLen = text.length();
        int offset = 0;
        do {
            int max = mOutputEnd;
            int segmentLen = ((offset + max) > textLen)
                ? (textLen - offset) : max;
            text.getChars(offset, offset+segmentLen, mOutputBuffer, 0);
            doWriteSegment(segmentLen);
            offset += segmentLen;
        } while (offset < textLen);
    }
    /**
     * Method called to output textual context which has been copied
     * to the output buffer prior to call. If any escaping is needed,
     * it will also be handled by the method.
     *<p>
     * Note: when called, textual content to write is within output
     * buffer, right after buffered content (if any). That's why only
     * length of that text is passed, as buffer and offset are implied.
     */
    private final void doWriteSegment(int end)
        throws IOException, JsonGenerationException
    {
        final int[] escCodes = CharTypes.getOutputEscapes();
        final int escLen = escCodes.length;

        int ptr = 0;

        output_loop:
        while (ptr < end) {
            // Fast loop for chars not needing escaping
            int start = ptr;
            while (true) {
                char c = mOutputBuffer[ptr];
                if (c < escLen && escCodes[c] != 0) {
                    break;
                }
                if (++ptr >= end) {
                    break;
                }
            }

            // Ok, bumped into something that needs escaping.
            /* First things first: need to flush the buffer.
             * Inlined, as we don't want to lose tail pointer
             */
            int flushLen = (ptr - start);
            if (flushLen > 0) {
                mWriter.write(mOutputBuffer, start, flushLen);
                if (ptr >= end) {
                    break output_loop;
                }
            }
            /* In any case, tail will be the new start, so hopefully
             * we have room now.
             */
            {
                int escCode = escCodes[mOutputBuffer[ptr]];
                ++ptr;
                int needLen = (escCode < 0) ? 6 : 2;
                // If not, need to call separate method (note: buffer is empty now)
                if (needLen > mOutputTail) {
                    writeSingleEscape(escCode);
                } else {
                    // But if it fits, can just prepend to buffer
                    ptr -= needLen;
                    appendSingleEscape(escCode, mOutputBuffer, ptr);
                }
            }
        }
    }

    /**
     * This method called when the string content is already in
     * a char buffer, and need not be copied for processing.
     */
    private void doWriteString(char[] text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        /* Let's just find longest spans of non-escapable
         * content, and for each see if it makes sense
         * to copy them, or write through
         */
        len += offset; // -> len marks the end from now on
        final int[] escCodes = CharTypes.getOutputEscapes();
        final int escLen = escCodes.length;
        while (offset < len) {
            int start = offset;

            while (true) {
                char c = text[offset];
                if (c < escLen && escCodes[c] != 0) {
                    break;
                }
                if (++offset >= len) {
                    break;
                }
            }

            // Short span? Better just copy it to buffer first:
            int newAmount = offset - start;
            if (newAmount < SHORT_WRITE) {
                // Note: let's reserve room for escaped char (up to 6 chars)
                if ((mOutputTail + newAmount) > mOutputEnd) {
                    flushBuffer();
                }
                if (newAmount > 0) {
                    System.arraycopy(text, start, mOutputBuffer, mOutputTail, newAmount);
                    mOutputTail += newAmount;
                }
            } else { // Nope: better just write through
                flushBuffer();
                mWriter.write(text, start, newAmount);
            }
            // Was this the end?
            if (offset >= len) { // yup
                break;
            }
            // Nope, need to escape the char.
            int escCode = escCodes[text[offset]];
            ++offset;
            int needLen = (escCode < 0) ? 6 : 2;
            if ((mOutputTail + needLen) > mOutputEnd) {
                flushBuffer();
            }
            appendSingleEscape(escCode, mOutputBuffer, mOutputTail);
            mOutputTail += needLen;
        }
    }

    /**
     * @param escCode Character code for escape sequence (\C); or -1
     *   to indicate a generic (\\uXXXX) sequence.
     */
    private void writeSingleEscape(int escCode)
        throws IOException
    {
        char[] buf = mEntityBuffer;
        if (buf == null) {
            buf = new char[6];
            buf[0] = '\\';
            buf[2] = '0';
            buf[3] = '0';
        }

        if (escCode < 0) { // control char, value -(char + 1)
            int value = -(escCode + 1);
            buf[1] = 'u';
            // We know it's a control char, so only the last 2 chars are non-0
            buf[4] = HEX_CHARS[value >> 4];
            buf[5] = HEX_CHARS[value & 0xF];
            mWriter.write(buf, 0, 6);
        } else {
            buf[1] = (char) escCode;
            mWriter.write(buf, 0, 2);
        }
    }

    private void appendSingleEscape(int escCode, char[] buf, int ptr)
    {
        if (escCode < 0) { // control char, value -(char + 1)
            int value = -(escCode + 1);
            buf[ptr] = '\\';
            buf[++ptr] = 'u';
            // We know it's a control char, so only the last 2 chars are non-0
            buf[++ptr] = '0';
            buf[++ptr] = '0';
            buf[++ptr] = HEX_CHARS[value >> 4];
            buf[++ptr] = HEX_CHARS[value & 0xF];
        } else {
            buf[ptr] = '\\';
            buf[ptr+1] = (char) escCode;
        }
    }


    protected void flushBuffer()
        throws IOException
    {
        int len = mOutputTail - mOutputHead;
        if (len > 0) {
            int offset = mOutputHead;
            mOutputTail = mOutputHead = 0;
            mWriter.write(mOutputBuffer, offset, len);
        }
    }
}
