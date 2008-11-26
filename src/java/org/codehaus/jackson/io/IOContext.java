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
    /*
    //////////////////////////////////////////////////////
    // Configuration
    //////////////////////////////////////////////////////
     */

    /**
     * Reference to the source object, which can be used for displaying
     * location information
     */
    final Object _sourceRef;

    /**
     * Encoding used by the underlying stream, if known.
     */
    protected JsonEncoding _encoding;

    /**
     * Flag that indicates whether underlying input/output source/target
     * object is fully managed by the owner of this context (parser or
     * generator). If true, it is, and is to be closed by parser/generator;
     * if false, calling application has to do closing (unless auto-closing
     * feature is enabled for the parser/generator in question; in which
     * case it acts like the owner).
     */
    protected final boolean _managedResource;

    /*
    //////////////////////////////////////////////////////
    // Buffer handling, recycling
    //////////////////////////////////////////////////////
     */

    final BufferRecycler _bufferRecycler;

    /**
     * Reference to the allocated I/O buffer for low-level input reading,
     * if any allocated.
     */
    protected byte[] _readIOBuffer = null;

    /**
     * Reference to the allocated I/O buffer for low-level input writing
     * if any allocated.
     */
    protected byte[] _writeIOBuffer = null;

    /**
     * Reference to the buffer allocated for tokenization purposes,
     * in which character input is read, and from which it can be
     * further returned.
     */
    protected char[] _tokenBuffer = null;

    /**
     * Reference to the buffer allocated for buffering it for
     * output, before being encoded: generally this means concatenating
     * output, then encoding when buffer fills up.
     */
    protected char[] _concatBuffer = null;

    /**
     * Reference temporary buffer Parser instances need if calling
     * app decides it wants to access name via 'getTextCharacters' method.
     * Regular text buffer can not be used as it may contain textual
     * representation of the value token.
     */
    protected char[] _nameCopyBuffer = null;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public IOContext(BufferRecycler br, Object sourceRef, boolean managedResource)
    {
        _bufferRecycler = br;
        _sourceRef = sourceRef;
        _managedResource = managedResource;
    }

    public void setEncoding(JsonEncoding enc)
    {
        _encoding = enc;
    }

    /*
    //////////////////////////////////////////////////////
    // Public API, accessors
    //////////////////////////////////////////////////////
     */

    public Object getSourceReference() { return _sourceRef; }
    public JsonEncoding getEncoding() { return _encoding; }
    public boolean isResourceManaged() { return _managedResource; }

    /*
    //////////////////////////////////////////////////////
    // Public API, buffer management
    //////////////////////////////////////////////////////
     */

    public TextBuffer constructTextBuffer()
    {
        return new TextBuffer(_bufferRecycler);
    }

    /**
     *<p>
     * Note: the method can only be called once during its life cycle.
     * This is to protect against accidental sharing.
     */
    public byte[] allocReadIOBuffer()
    {
        if (_readIOBuffer != null) {
            throw new IllegalStateException("Trying to call allocReadIOBuffer() second time");
        }
        _readIOBuffer = _bufferRecycler.allocByteBuffer(BufferRecycler.ByteBufferType.READ_IO_BUFFER);
        return _readIOBuffer;
    }

    public byte[] allocWriteIOBuffer()
    {
        if (_writeIOBuffer != null) {
            throw new IllegalStateException("Trying to call allocWriteIOBuffer() second time");
        }
        _writeIOBuffer = _bufferRecycler.allocByteBuffer(BufferRecycler.ByteBufferType.WRITE_IO_BUFFER);
        return _writeIOBuffer;
    }

    public char[] allocTokenBuffer()
    {
        if (_tokenBuffer != null) {
            throw new IllegalStateException("Trying to call allocTokenBuffer() second time");
        }
        _tokenBuffer = _bufferRecycler.allocCharBuffer(BufferRecycler.CharBufferType.TOKEN_BUFFER);
        return _tokenBuffer;
    }

    public char[] allocConcatBuffer()
    {
        if (_concatBuffer != null) {
            throw new IllegalStateException("Trying to call allocConcatBuffer() second time");
        }
        _concatBuffer = _bufferRecycler.allocCharBuffer(BufferRecycler.CharBufferType.CONCAT_BUFFER);
        return _concatBuffer;
    }

    public char[] allocNameCopyBuffer(int minSize)
    {
        if (_nameCopyBuffer != null) {
            throw new IllegalStateException("Trying to call allocNameCopyBuffer() second time");
        }
        _nameCopyBuffer = _bufferRecycler.allocCharBuffer(BufferRecycler.CharBufferType.NAME_COPY_BUFFER, minSize);
        return _nameCopyBuffer;
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
            if (buf != _readIOBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            _readIOBuffer = null;
            _bufferRecycler.releaseByteBuffer(BufferRecycler.ByteBufferType.READ_IO_BUFFER, buf);
        }
    }

    public void releaseWriteIOBuffer(byte[] buf)
    {
        if (buf != null) {
            /* Let's do sanity checks to ensure once-and-only-once release,
             * as well as avoiding trying to release buffers not owned
             */
            if (buf != _writeIOBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            _writeIOBuffer = null;
            _bufferRecycler.releaseByteBuffer(BufferRecycler.ByteBufferType.WRITE_IO_BUFFER, buf);
        }
    }

    public void releaseTokenBuffer(char[] buf)
    {
        if (buf != null) {
            if (buf != _tokenBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            _tokenBuffer = null;
            _bufferRecycler.releaseCharBuffer(BufferRecycler.CharBufferType.TOKEN_BUFFER, buf);
        }
    }

    public void releaseConcatBuffer(char[] buf)
    {
        if (buf != null) {
            if (buf != _concatBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            _concatBuffer = null;
            _bufferRecycler.releaseCharBuffer(BufferRecycler.CharBufferType.CONCAT_BUFFER, buf);
        }
    }

    public void releaseNameCopyBuffer(char[] buf)
    {
        if (buf != null) {
            if (buf != _nameCopyBuffer) {
                throw new IllegalArgumentException("Trying to release buffer not owned by the context");
            }
            _nameCopyBuffer = null;
            _bufferRecycler.releaseCharBuffer(BufferRecycler.CharBufferType.NAME_COPY_BUFFER, buf);
        }
    }
}
