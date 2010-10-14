package perf;
//package org.codehaus.jackson.io;

import java.io.*;

import org.codehaus.jackson.io.IOContext;

/**
 * Simple basic class for optimized readers in this package; implements
 * "cookie-cutter" methods that are used by all actual implementations.
 */
abstract class BaseReader
    extends Reader
{
    /**
     * JSON actually limits available Unicode range in the high end
     * to the same as xml (to basically limit UTF-8 max byte sequence
     * length to 4)
     */
    final protected static int LAST_VALID_UNICODE_CHAR = 0x10FFFF;

    final protected static char NULL_CHAR = (char) 0;
    final protected static char NULL_BYTE = (byte) 0;

    final protected IOContext mContext;

    protected InputStream mIn;

    protected byte[] mBuffer;

    protected int mPtr;
    protected int mLength;

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    protected BaseReader(IOContext context,
                         InputStream in, byte[] buf, int ptr, int len)
    {
        mContext = context;
        mIn = in;
        mBuffer = buf;
        mPtr = ptr;
        mLength = len;
    }

    /*
    ////////////////////////////////////////
    // Reader API
    ////////////////////////////////////////
    */

    @Override
    public void close()  throws IOException
    {
        InputStream in = mIn;

        if (in != null) {
            mIn = null;
            freeBuffers();
            in.close();
        }
    }

    char[] mTmpBuf = null;

    /**
     * Although this method is implemented by the base class, AND it should
     * never be called by main code, let's still implement it bit more
     * efficiently just in case
     */
    @Override
    public int read()
        throws IOException
    {
        if (mTmpBuf == null) {
            mTmpBuf = new char[1];
        }
        if (read(mTmpBuf, 0, 1) < 1) {
            return -1;
        }
        return mTmpBuf[0];
    }

    /*
    ////////////////////////////////////////
    // Internal/package methods:
    ////////////////////////////////////////
    */

    /**
     * This method should be called along with (or instead of) normal
     * close. After calling this method, no further reads should be tried.
     * Method will try to recycle read buffers (if any).
     */
    public final void freeBuffers()
    {
        byte[] buf = mBuffer;
        if (buf != null) {
            mBuffer = null;
            mContext.releaseReadIOBuffer(buf);
        }
    }

    protected void reportBounds(char[] cbuf, int start, int len)
        throws IOException
    {
        throw new ArrayIndexOutOfBoundsException("read(buf,"+start+","+len+"), cbuf["+cbuf.length+"]");
    }

    protected void reportStrangeStream()
        throws IOException
    {
        throw new IOException("Strange I/O stream, returned 0 bytes on read");
    }
}
