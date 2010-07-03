package org.codehaus.jackson.io;

import java.io.*;


public final class UTF8Writer
    extends Writer
{
    final static int SURR1_FIRST = 0xD800;
    final static int SURR1_LAST = 0xDBFF;
    final static int SURR2_FIRST = 0xDC00;
    final static int SURR2_LAST = 0xDFFF;

    final protected IOContext mContext;

    OutputStream mOut;

    byte[] mOutBuffer;

    final int mOutBufferLast;

    int mOutPtr;

    /**
     * When outputting chars from BMP, surrogate pairs need to be coalesced.
     * To do this, both pairs must be known first; and since it is possible
     * pairs may be split, we need temporary storage for the first half
     */
    int mSurrogate = 0;

    public UTF8Writer(IOContext ctxt, OutputStream out)
    {
        mContext = ctxt;
        mOut = out;

        mOutBuffer = ctxt.allocWriteEncodingBuffer();
        /* Max. expansion for a single char (in unmodified UTF-8) is
         * 4 bytes (or 3 depending on how you view it -- 4 when recombining
         * surrogate pairs)
         */
        mOutBufferLast = mOutBuffer.length - 4;
        mOutPtr = 0;
    }

    @Override
	public Writer append(char c)
        throws IOException
    {
        write(c);
        return this;
    }

    @Override
	public void close()
        throws IOException
    {
        if (mOut != null) {
            if (mOutPtr > 0) {
                mOut.write(mOutBuffer, 0, mOutPtr);
                mOutPtr = 0;
            }
            OutputStream out = mOut;
            mOut = null;

            byte[] buf = mOutBuffer;
            if (buf != null) {
                mOutBuffer = null;
                mContext.releaseWriteEncodingBuffer(buf);
            }

            out.close();

            /* Let's 'flush' orphan surrogate, no matter what; but only
             * after cleanly closing everything else.
             */
            int code = mSurrogate;
            mSurrogate = 0;
            if (code > 0) {
                throwIllegal(code);
            }
        }
    }

    @Override
	public void flush()
        throws IOException
    {
        if (mOutPtr > 0) {
            mOut.write(mOutBuffer, 0, mOutPtr);
            mOutPtr = 0;
        }
        mOut.flush();
    }

    @Override
	public void write(char[] cbuf)
        throws IOException
    {
        write(cbuf, 0, cbuf.length);
    }

    @Override
	public void write(char[] cbuf, int off, int len)
        throws IOException
    {
        if (len < 2) {
            if (len == 1) {
                write(cbuf[off]);
            }
            return;
        }

        // First: do we have a leftover surrogate to deal with?
        if (mSurrogate > 0) {
            char second = cbuf[off++];
            --len;
            write(convertSurrogate(second));
            // will have at least one more char
        }

        int outPtr = mOutPtr;
        byte[] outBuf = mOutBuffer;
        int outBufLast = mOutBufferLast; // has 4 'spare' bytes

        // All right; can just loop it nice and easy now:
        len += off; // len will now be the end of input buffer

        output_loop:
        for (; off < len; ) {
            /* First, let's ensure we can output at least 4 bytes
             * (longest UTF-8 encoded codepoint):
             */
            if (outPtr >= outBufLast) {
                mOut.write(outBuf, 0, outPtr);
                outPtr = 0;
            }

            int c = cbuf[off++];
            // And then see if we have an Ascii char:
            if (c < 0x80) { // If so, can do a tight inner loop:
                outBuf[outPtr++] = (byte)c;
                // Let's calc how many ascii chars we can copy at most:
                int maxInCount = (len - off);
                int maxOutCount = (outBufLast - outPtr);

                if (maxInCount > maxOutCount) {
                    maxInCount = maxOutCount;
                }
                maxInCount += off;
                ascii_loop:
                while (true) {
                    if (off >= maxInCount) { // done with max. ascii seq
                        continue output_loop;
                    }
                    c = cbuf[off++];
                    if (c >= 0x80) {
                        break ascii_loop;
                    }
                    outBuf[outPtr++] = (byte) c;
                }
            }

            // Nope, multi-byte:
            if (c < 0x800) { // 2-byte
                outBuf[outPtr++] = (byte) (0xc0 | (c >> 6));
                outBuf[outPtr++] = (byte) (0x80 | (c & 0x3f));
            } else { // 3 or 4 bytes
                // Surrogates?
                if (c < SURR1_FIRST || c > SURR2_LAST) {
                    outBuf[outPtr++] = (byte) (0xe0 | (c >> 12));
                    outBuf[outPtr++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                    outBuf[outPtr++] = (byte) (0x80 | (c & 0x3f));
                    continue;
                }
                // Yup, a surrogate:
                if (c > SURR1_LAST) { // must be from first range
                    mOutPtr = outPtr;
                    throwIllegal(c);
                }
                mSurrogate = c;
                // and if so, followed by another from next range
                if (off >= len) { // unless we hit the end?
                    break;
                }
                c = convertSurrogate(cbuf[off++]);
                if (c > 0x10FFFF) { // illegal in JSON as well as in XML
                    mOutPtr = outPtr;
                    throwIllegal(c);
                }
                outBuf[outPtr++] = (byte) (0xf0 | (c >> 18));
                outBuf[outPtr++] = (byte) (0x80 | ((c >> 12) & 0x3f));
                outBuf[outPtr++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                outBuf[outPtr++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        mOutPtr = outPtr;
    }
    
    @Override
	public void write(int c) throws IOException
    {
        // First; do we have a left over surrogate?
        if (mSurrogate > 0) {
            c = convertSurrogate(c);
            // If not, do we start with a surrogate?
        } else if (c >= SURR1_FIRST && c <= SURR2_LAST) {
            // Illegal to get second part without first:
            if (c > SURR1_LAST) {
                throwIllegal(c);
            }
            // First part just needs to be held for now
            mSurrogate = c;
            return;
        }

        if (mOutPtr >= mOutBufferLast) { // let's require enough room, first
            mOut.write(mOutBuffer, 0, mOutPtr);
            mOutPtr = 0;
        }

        if (c < 0x80) { // ascii
            mOutBuffer[mOutPtr++] = (byte) c;
        } else {
            int ptr = mOutPtr;
            if (c < 0x800) { // 2-byte
                mOutBuffer[ptr++] = (byte) (0xc0 | (c >> 6));
                mOutBuffer[ptr++] = (byte) (0x80 | (c & 0x3f));
            } else if (c <= 0xFFFF) { // 3 bytes
                mOutBuffer[ptr++] = (byte) (0xe0 | (c >> 12));
                mOutBuffer[ptr++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                mOutBuffer[ptr++] = (byte) (0x80 | (c & 0x3f));
            } else { // 4 bytes
                if (c > 0x10FFFF) { // illegal
                    throwIllegal(c);
                }
                mOutBuffer[ptr++] = (byte) (0xf0 | (c >> 18));
                mOutBuffer[ptr++] = (byte) (0x80 | ((c >> 12) & 0x3f));
                mOutBuffer[ptr++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                mOutBuffer[ptr++] = (byte) (0x80 | (c & 0x3f));
            }
            mOutPtr = ptr;
        }
    }

    @Override
	public void write(String str) throws IOException
    {
        write(str, 0, str.length());
    }

    @Override
	public void write(String str, int off, int len)  throws IOException
    {
        if (len < 2) {
            if (len == 1) {
                write(str.charAt(off));
            }
            return;
        }

        // First: do we have a leftover surrogate to deal with?
        if (mSurrogate > 0) {
            char second = str.charAt(off++);
            --len;
            write(convertSurrogate(second));
            // will have at least one more char (case of 1 char was checked earlier on)
        }

        int outPtr = mOutPtr;
        byte[] outBuf = mOutBuffer;
        int outBufLast = mOutBufferLast; // has 4 'spare' bytes

        // All right; can just loop it nice and easy now:
        len += off; // len will now be the end of input buffer

        output_loop:
        for (; off < len; ) {
            /* First, let's ensure we can output at least 4 bytes
             * (longest UTF-8 encoded codepoint):
             */
            if (outPtr >= outBufLast) {
                mOut.write(outBuf, 0, outPtr);
                outPtr = 0;
            }

            int c = str.charAt(off++);
            // And then see if we have an Ascii char:
            if (c < 0x80) { // If so, can do a tight inner loop:
                outBuf[outPtr++] = (byte)c;
                // Let's calc how many ascii chars we can copy at most:
                int maxInCount = (len - off);
                int maxOutCount = (outBufLast - outPtr);

                if (maxInCount > maxOutCount) {
                    maxInCount = maxOutCount;
                }
                maxInCount += off;
                ascii_loop:
                while (true) {
                    if (off >= maxInCount) { // done with max. ascii seq
                        continue output_loop;
                    }
                    c = str.charAt(off++);
                    if (c >= 0x80) {
                        break ascii_loop;
                    }
                    outBuf[outPtr++] = (byte) c;
                }
            }

            // Nope, multi-byte:
            if (c < 0x800) { // 2-byte
                outBuf[outPtr++] = (byte) (0xc0 | (c >> 6));
                outBuf[outPtr++] = (byte) (0x80 | (c & 0x3f));
            } else { // 3 or 4 bytes
                // Surrogates?
                if (c < SURR1_FIRST || c > SURR2_LAST) {
                    outBuf[outPtr++] = (byte) (0xe0 | (c >> 12));
                    outBuf[outPtr++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                    outBuf[outPtr++] = (byte) (0x80 | (c & 0x3f));
                    continue;
                }
                // Yup, a surrogate:
                if (c > SURR1_LAST) { // must be from first range
                    mOutPtr = outPtr;
                    throwIllegal(c);
                }
                mSurrogate = c;
                // and if so, followed by another from next range
                if (off >= len) { // unless we hit the end?
                    break;
                }
                c = convertSurrogate(str.charAt(off++));
                if (c > 0x10FFFF) { // illegal, as per RFC 4627
                    mOutPtr = outPtr;
                    throwIllegal(c);
                }
                outBuf[outPtr++] = (byte) (0xf0 | (c >> 18));
                outBuf[outPtr++] = (byte) (0x80 | ((c >> 12) & 0x3f));
                outBuf[outPtr++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                outBuf[outPtr++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        mOutPtr = outPtr;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////////////
     */

    /**
     * Method called to calculate UTF codepoint, from a surrogate pair.
     */
    private int convertSurrogate(int secondPart)
        throws IOException
    {
        int firstPart = mSurrogate;
        mSurrogate = 0;

        // Ok, then, is the second part valid?
        if (secondPart < SURR2_FIRST || secondPart > SURR2_LAST) {
            throw new IOException("Broken surrogate pair: first char 0x"+Integer.toHexString(firstPart)+", second 0x"+Integer.toHexString(secondPart)+"; illegal combination");
        }
        return 0x10000 + ((firstPart - SURR1_FIRST) << 10) + (secondPart - SURR2_FIRST);
    }

    private void throwIllegal(int code)
        throws IOException
    {
        if (code > 0x10FFFF) { // over max?
            throw new IOException("Illegal character point (0x"+Integer.toHexString(code)+") to output; max is 0x10FFFF as per RFC 4627");
        }
        if (code >= SURR1_FIRST) {
            if (code <= SURR1_LAST) { // Unmatched first part (closing without second part?)
                throw new IOException("Unmatched first part of surrogate pair (0x"+Integer.toHexString(code)+")");
            }
            throw new IOException("Unmatched second part of surrogate pair (0x"+Integer.toHexString(code)+")");
        }

        // should we ever get this?
        throw new IOException("Illegal character point (0x"+Integer.toHexString(code)+") to output");
    }
}
