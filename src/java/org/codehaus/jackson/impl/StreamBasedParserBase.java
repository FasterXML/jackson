package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.io.IOContext;

/**
 * This is a simple low-level input reader base class, used by
 * JSON parser. It is used when underlying input source is
 * a byte stream such as {@link InputStream}.
 * The reason for sub-classing (over composition)
 * is due to need for direct access to low-level byte buffers
 * and positions.
 *
 * @author Tatu Saloranta
 */
public abstract class StreamBasedParserBase
    extends JsonNumericParserBase
{
    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Input stream that can be used for reading more content, if one
     * in use. May be null, if input comes just as a full buffer,
     * or if the stream has been closed.
     */
    protected InputStream mInputStream;

    /*
    ////////////////////////////////////////////////////
    // Current input data
    ////////////////////////////////////////////////////
     */

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source, but in some cases pre-loaded buffer
     * is handed to the parser.
     */
    protected byte[] mInputBuffer;

    /**
     * Flag that indicates whether the input buffer is recycable (and
     * needs to be returned to recycler once we are done) or not.
     */
    protected boolean mBufferRecyclable;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected StreamBasedParserBase(IOContext ctxt, InputStream in,
                                    byte[] inputBuffer, int start, int end,
                                    boolean bufferRecyclable)
    {
        super(ctxt);
        mInputStream = in;
        mInputBuffer = inputBuffer;
        _inputPtr = start;
        _inputEnd = end;
        mBufferRecyclable = bufferRecyclable;
    }

    /*
    ////////////////////////////////////////////////////
    // Low-level reading, other
    ////////////////////////////////////////////////////
     */

    protected final boolean loadMore()
        throws IOException
    {
        _currInputProcessed += _inputEnd;
        _currInputRowStart -= _inputEnd;

        if (mInputStream != null) {
            int count = mInputStream.read(mInputBuffer, 0, mInputBuffer.length);
            if (count > 0) {
                _inputPtr = 0;
                _inputEnd = count;
                return true;
            }
            // End of input
            closeInput();
            // Should never return 0, so let's fail
            if (count == 0) {
                throw new IOException("Reader returned 0 characters when trying to read "+_inputEnd);
            }
        }
        return false;
    }

    protected void closeInput()
        throws IOException
    {
        InputStream in = mInputStream;
        if (in != null) {
            mInputStream = null;
            in.close();
        }
    }

    /**
     * Method called to release internal buffers owned by the base
     * reader. This may be called along with {@link #closeInput} (for
     * example, when explicitly closing this reader instance), or
     * separately (if need be).
     */
    @Override
    protected void releaseBuffers()
        throws IOException
    {
        super.releaseBuffers();
        if (mBufferRecyclable) {
            byte[] buf = mInputBuffer;
            if (buf != null) {
                mInputBuffer = null;
                _ioContext.releaseReadIOBuffer(buf);
            }
        }
    }
}
