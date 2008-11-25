package org.codehaus.jackson.io;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.util.BufferRecycler;
import org.codehaus.jackson.util.TextBuffer;

/**
 * To limit number of configuration and state objects to pass, all
 * contextual objects that need to be passed by the factory to
 * readers and writers are combined under this object. One instance
 * is created for each reader and writer.
 */
public final class IOContext
{
    // // // Configuration

    final BufferRecycler mBufferRecycler;

    /**
     * Reference to the source object, which can be used for displaying
     * location information
     */
    final Object mSourceRef;

    /**
     * Encoding used by the underlying stream, if known.
     */
    protected JsonEncoding mEncoding;

    // // // Allocated buffers that need to be kept track of

    /**
     * Reference to the allocated I/O buffer for low-level input reading,
     * if any allocated.
     */
    protected byte[] mReadIOBuffer = null;

    /**
     * Reference to the allocated I/O buffer for low-level input writing
     * if any allocated.
     */
    protected byte[] mWriteIOBuffer = null;

    /**
     * Reference to the buffer allocated for tokenization purposes,
     * in which character input is read, and from which it can be
     * further returned.
     */
    protected char[] mTokenBuffer = null;

    /**
     * Reference to the buffer allocated for buffering it for
     * output, before being encoded: generally this means concatenating
     * output, then encoding when buffer fills up.
     */
    protected char[] mConcatBuffer = null;

    /**
     * Reference temporary buffer Parser instances need if calling
     * app decides it wants to access name via 'getTextCharacters' method.
     * Regular text buffer can not be used as it may contain textual
     * representation of the value token.
     */
    protected char[] mNameCopyBuffer = null;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public IOContext(BufferRecycler br, Object sourceRef)
    {
        mBufferRecycler = br;
        mSourceRef = sourceRef;
    }

    public void setEncoding(JsonEncoding enc)
    {
        mEncoding = enc;
    }

    public TextBuffer constructTextBuffer()
    {
        return new TextBuffer(mBufferRecycler);
    }

    /**
     *<p>
     * Note: the method can only be called once during its life cycle.
     * This is to protect against accidental sharing.
     */
    public byte[] allocReadIOBuffer()
    {
        if (mReadIOBuffer != null) {
            throw new IllegalStateException("Trying to call allocReadIOBuffer() second time");
        }
        mReadIOBuffer = mBufferRecycler.allocByteBuffer(BufferRecycler.ByteBufferType.READ_IO_BUFFER);
        return mReadIOBuffer;
    }

    public byte[] allocWriteIOBuffer()
    {
        if (mWriteIOBuffer != null) {
            throw new IllegalStateException("Trying to call allocWriteIOBuffer() second time");
        }
        mWriteIOBuffer = mBufferRecycler.allocByteBuffer(BufferRecycler.ByteBufferType.WRITE_IO_BUFFER);
        return mWriteIOBuffer;
    }

    public char[] allocTokenBuffer()
    {
        if (mTokenBuffer != null) {
            throw new IllegalStateException("Trying to call allocTokenBuffer() second time");
        }
        mTokenBuffer = mBufferRecycler.allocCharBuffer(BufferRecycler.CharBufferType.TOKEN_BUFFER);
        return mTokenBuffer;
    }

    public char[] allocConcatBuffer()
    {
        if (mConcatBuffer != null) {
            throw new IllegalStateException("Trying to call allocConcatBuffer() second time");
        }
        mConcatBuffer = mBufferRecycler.allocCharBuffer(BufferRecycler.CharBufferType.CONCAT_BUFFER);
        return mConcatBuffer;
    }

    public char[] allocNameCopyBuffer(int minSize)
    {
        if (mNameCopyBuffer != null) {
            throw new IllegalStateException("Trying to call allocNameCopyBuffer() second time");
        }
        mNameCopyBuffer = mBufferRecycler.allocCharBuffer(BufferRecycler.CharBufferType.NAME_COPY_BUFFER, minSize);
        return mNameCopyBuffer;
    }

    /**
     * Method to call when all the processing buffers can be safely
     * recycled.
     */
    public void releaseReadIOBuffer(byte[] buf)
    {
        if (buf != null) {
            /* Let's do sanity checks to ensure once-and-only-once release,
             * as well as avoiding trying to release buffers not owned
             */
            if (buf != mReadIOBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            mReadIOBuffer = null;
            mBufferRecycler.releaseByteBuffer(BufferRecycler.ByteBufferType.READ_IO_BUFFER, buf);
        }
    }

    public void releaseWriteIOBuffer(byte[] buf)
    {
        if (buf != null) {
            /* Let's do sanity checks to ensure once-and-only-once release,
             * as well as avoiding trying to release buffers not owned
             */
            if (buf != mWriteIOBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            mWriteIOBuffer = null;
            mBufferRecycler.releaseByteBuffer(BufferRecycler.ByteBufferType.WRITE_IO_BUFFER, buf);
        }
    }

    public void releaseTokenBuffer(char[] buf)
    {
        if (buf != null) {
            if (buf != mTokenBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            mTokenBuffer = null;
            mBufferRecycler.releaseCharBuffer(BufferRecycler.CharBufferType.TOKEN_BUFFER, buf);
        }
    }

    public void releaseConcatBuffer(char[] buf)
    {
        if (buf != null) {
            if (buf != mConcatBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            mConcatBuffer = null;
            mBufferRecycler.releaseCharBuffer(BufferRecycler.CharBufferType.CONCAT_BUFFER, buf);
        }
    }

    public void releaseNameCopyBuffer(char[] buf)
    {
        if (buf != null) {
            if (buf != mNameCopyBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            mNameCopyBuffer = null;
            mBufferRecycler.releaseCharBuffer(BufferRecycler.CharBufferType.NAME_COPY_BUFFER, buf);
        }
    }

    /*
    //////////////////////////////////////////////////////
    // Public API, accessors
    //////////////////////////////////////////////////////
     */

    public Object getSourceReference() { return mSourceRef; }
    public JsonEncoding getEncoding() { return mEncoding; }
}
