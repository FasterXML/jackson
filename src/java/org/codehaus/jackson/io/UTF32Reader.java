package org.codehaus.jackson.io;

import java.io.*;


/**
 * Since JDK does not come with UTF-32/UCS-4, let's implement a simple
 * decoder to use.
 */
public final class UTF32Reader
    extends BaseReader
{
    final boolean mBigEndian;

    /**
     * Although input is fine with full Unicode set, Java still uses
     * 16-bit chars, so we may have to split high-order chars into
     * surrogate pairs.
     */
    char mSurrogate = NULL_CHAR;

    /**
     * Total read character count; used for error reporting purposes
     */
    int mCharCount = 0;

    /**
     * Total read byte count; used for error reporting purposes
     */
    int mByteCount = 0;

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    public UTF32Reader(IOContext ctxt,
                       InputStream in, byte[] buf, int ptr, int len,
                       boolean isBigEndian)
    {
        super(ctxt, in, buf, ptr, len);
        mBigEndian = isBigEndian;
    }

    /*
    ////////////////////////////////////////
    // Public API
    ////////////////////////////////////////
    */

    @Override
	public int read(char[] cbuf, int start, int len)
        throws IOException
    {
        // Already EOF?
        if (mBuffer == null) {
            return -1;
        }
        if (len < 1) {
            return len;
        }
        // Let's then ensure there's enough room...
        if (start < 0 || (start+len) > cbuf.length) {
            reportBounds(cbuf, start, len);
        }

        len += start;
        int outPtr = start;

        // Ok, first; do we have a surrogate from last round?
        if (mSurrogate != NULL_CHAR) {
            cbuf[outPtr++] = mSurrogate;
            mSurrogate = NULL_CHAR;
            // No need to load more, already got one char
        } else {
            /* Note: we'll try to avoid blocking as much as possible. As a
             * result, we only need to get 4 bytes for a full char.
             */
            int left = (mLength - mPtr);
            if (left < 4) {
                if (!loadMore(left)) { // (legal) EOF?
                    return -1;
                }
            }
        }

        main_loop:
        while (outPtr < len) {
            int ptr = mPtr;
            int ch;

            if (mBigEndian) {
                ch = (mBuffer[ptr] << 24) | ((mBuffer[ptr+1] & 0xFF) << 16)
                    | ((mBuffer[ptr+2] & 0xFF) << 8) | (mBuffer[ptr+3] & 0xFF);
            } else {
                ch = (mBuffer[ptr] & 0xFF) | ((mBuffer[ptr+1] & 0xFF) << 8)
                    | ((mBuffer[ptr+2] & 0xFF) << 16) | (mBuffer[ptr+3] << 24);
            }
            mPtr += 4;

            // Does it need to be split to surrogates?
            // (also, we can and need to verify illegal chars)
            if (ch > 0xFFFF) { // need to split into surrogates?
                if (ch > LAST_VALID_UNICODE_CHAR) {
                    reportInvalid(ch, outPtr-start,
                                  "(above "+Integer.toHexString(LAST_VALID_UNICODE_CHAR)+") ");
                }
                ch -= 0x10000; // to normalize it starting with 0x0
                cbuf[outPtr++] = (char) (0xD800 + (ch >> 10));
                // hmmh. can this ever be 0? (not legal, at least?)
                ch = (0xDC00 | (ch & 0x03FF));
                // Room for second part?
                if (outPtr >= len) { // nope
                    mSurrogate = (char) ch;
                    break main_loop;
                }
            }
            cbuf[outPtr++] = (char) ch;
            if (mPtr >= mLength) {
                break main_loop;
            }
        }

        len = outPtr - start;
        mCharCount += len;
        return len;
    }

    /*
    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////
    */

    private void reportUnexpectedEOF(int gotBytes, int needed)
        throws IOException
    {
        int bytePos = mByteCount + gotBytes;
        int charPos = mCharCount;

        throw new CharConversionException("Unexpected EOF in the middle of a 4-byte UTF-32 char: got "
                                          +gotBytes+", needed "+needed
                                          +", at char #"+charPos+", byte #"+bytePos+")");
    }

    private void reportInvalid(int value, int offset, String msg)
        throws IOException
    {
        int bytePos = mByteCount + mPtr - 1;
        int charPos = mCharCount + offset;

        throw new CharConversionException("Invalid UTF-32 character 0x"
                                          +Integer.toHexString(value)
                                          +msg+" at char #"+charPos+", byte #"+bytePos+")");
    }

    /**
     * @param available Number of "unused" bytes in the input buffer
     *
     * @return True, if enough bytes were read to allow decoding of at least
     *   one full character; false if EOF was encountered instead.
     */
    private boolean loadMore(int available)
        throws IOException
    {
        mByteCount += (mLength - available);

        // Bytes that need to be moved to the beginning of buffer?
        if (available > 0) {
            if (mPtr > 0) {
                for (int i = 0; i < available; ++i) {
                    mBuffer[i] = mBuffer[mPtr+i];
                }
                mPtr = 0;
            }
            mLength = available;
        } else {
            /* Ok; here we can actually reasonably expect an EOF,
             * so let's do a separate read right away:
             */
            mPtr = 0;
            int count = mIn.read(mBuffer);
            if (count < 1) {
                mLength = 0;
                if (count < 0) { // -1
                    freeBuffers(); // to help GC?
                    return false;
                }
                // 0 count is no good; let's err out
                reportStrangeStream();
            }
            mLength = count;
        }

        /* Need at least 4 bytes; if we don't get that many, it's an
         * error.
         */
        while (mLength < 4) {
            int count = mIn.read(mBuffer, mLength, mBuffer.length - mLength);
            if (count < 1) {
                if (count < 0) { // -1, EOF... no good!
                    freeBuffers(); // to help GC?
                    reportUnexpectedEOF(mLength, 4);
                }
                // 0 count is no good; let's err out
                reportStrangeStream();
            }
            mLength += count;
        }
        return true;
    }
}

