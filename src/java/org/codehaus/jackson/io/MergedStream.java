package org.codehaus.jackson.io;

import java.io.*;


/**
 * Simple {@link InputStream} implementation that is used to "unwind" some
 * data previously read from an input stream; so that as long as some of
 * that data remains, it's returned; but as long as it's read, we'll
 * just use data from the underlying original stream. 
 * This is similar to {@link java.io.PushbackInputStream}, but here there's
 * only one implicit pushback, when instance is constructed.
 */
public final class MergedStream
    extends InputStream
{
    final protected IOContext mContext;

    final InputStream mIn;

    byte[] mBuffer;

    int mPtr;

    final int mEnd;

    public MergedStream(IOContext context,
                        InputStream in, byte[] buf, int start, int end)
    {
        mContext = context;
        mIn = in;
        mBuffer = buf;
        mPtr = start;
        mEnd = end;
    }

    public int available()
        throws IOException
    {
        if (mBuffer != null) {
            return mEnd - mPtr;
        }
        return mIn.available();
    }

    public void close()
        throws IOException
    {
        freeMergedBuffer();
        mIn.close();
    }

    public void mark(int readlimit)
    {
        if (mBuffer == null) {
            mIn.mark(readlimit);
        }
    }
    
    public boolean markSupported()
    {
        // Only supports marks past the initial rewindable section...
        return (mBuffer == null) && mIn.markSupported();
    }
    
    public int read()
        throws IOException
    {
        if (mBuffer != null) {
            int c = mBuffer[mPtr++] & 0xFF;
            if (mPtr >= mEnd) {
                freeMergedBuffer();
            }
            return c;
        }
        return mIn.read();
    }
    
    public int read(byte[] b)
        throws IOException
    {
        return read(b, 0, b.length);
    }

    public int 	read(byte[] b, int off, int len)
        throws IOException
    {
        if (mBuffer != null) {
            int avail = mEnd - mPtr;
            if (len > avail) {
                len = avail;
            }
            System.arraycopy(mBuffer, mPtr, b, off, len);
            mPtr += len;
            if (mPtr >= mEnd) {
                freeMergedBuffer();
            }
            return len;
        }
        return mIn.read(b, off, len);
    }

    public void reset()
        throws IOException
    {
        if (mBuffer == null) {
            mIn.reset();
        }
    }

    public long skip(long n)
        throws IOException
    {
        long count = 0L;

        if (mBuffer != null) {
            int amount = mEnd - mPtr;

            if (amount > n) { // all in pushed back segment?
                mPtr += (int) n;
                return amount;
            }
            freeMergedBuffer();
            count += amount;
            n -= amount;
        }

        if (n > 0) {
            count += mIn.skip(n);
        }
        return count;
    }

    private void freeMergedBuffer()
    {
        byte[] buf = mBuffer;
        if (buf != null) {
            mBuffer = null;
            mContext.releaseReadIOBuffer(buf);
        }
    }
}
