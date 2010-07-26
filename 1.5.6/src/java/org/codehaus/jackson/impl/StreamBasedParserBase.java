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
    protected InputStream _inputStream;

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
    protected byte[] _inputBuffer;

    /**
     * Flag that indicates whether the input buffer is recycable (and
     * needs to be returned to recycler once we are done) or not.
     */
    protected boolean _bufferRecyclable;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected StreamBasedParserBase(IOContext ctxt, int features,
                                    InputStream in,
                                    byte[] inputBuffer, int start, int end,
                                    boolean bufferRecyclable)
    {
        super(ctxt, features);
        _inputStream = in;
        _inputBuffer = inputBuffer;
        _inputPtr = start;
        _inputEnd = end;
        _bufferRecyclable = bufferRecyclable;
    }

    /*
    ////////////////////////////////////////////////////
    // Low-level reading, other
    ////////////////////////////////////////////////////
     */

    @Override
	protected final boolean loadMore()
        throws IOException
    {
        _currInputProcessed += _inputEnd;
        _currInputRowStart -= _inputEnd;

        if (_inputStream != null) {
            int count = _inputStream.read(_inputBuffer, 0, _inputBuffer.length);
            if (count > 0) {
                _inputPtr = 0;
                _inputEnd = count;
                return true;
            }
            // End of input
            _closeInput();
            // Should never return 0, so let's fail
            if (count == 0) {
                throw new IOException("Reader returned 0 characters when trying to read "+_inputEnd);
            }
        }
        return false;
    }

    @Override
    protected void _closeInput() throws IOException
    {
        /* 25-Nov-2008, tatus: As per [JACKSON-16] we are not to call close()
         *   on the underlying Reader, unless we "own" it, or auto-closing
         *   feature is enabled.
         */
        if (_inputStream != null) {
            if (_ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_SOURCE)) {
                _inputStream.close();
            }
            _inputStream = null;
        }
    }

    /**
     * Method called to release internal buffers owned by the base
     * reader. This may be called along with {@link #_closeInput} (for
     * example, when explicitly closing this reader instance), or
     * separately (if need be).
     */
    @Override
    protected void _releaseBuffers() throws IOException
    {
        super._releaseBuffers();
        if (_bufferRecyclable) {
            byte[] buf = _inputBuffer;
            if (buf != null) {
                _inputBuffer = null;
                _ioContext.releaseReadIOBuffer(buf);
            }
        }
    }
}
