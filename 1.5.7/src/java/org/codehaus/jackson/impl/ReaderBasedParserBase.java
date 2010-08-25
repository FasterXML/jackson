package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;

/**
 * This is a simple low-level input reader base class, used by
 * JSON parser.
 * The reason for sub-classing (over composition)
 * is due to need for direct access to character buffers
 * and positions.
 *
 * @author Tatu Saloranta
 */
public abstract class ReaderBasedParserBase
    extends JsonNumericParserBase
{
    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Reader that can be used for reading more content, if one
     * buffer from input source, but in some cases pre-loaded buffer
     * is handed to the parser.
     */
    protected Reader _reader;

    /*
    ////////////////////////////////////////////////////
    // Current input data
    ////////////////////////////////////////////////////
     */

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source.
     */
    protected char[] _inputBuffer;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected ReaderBasedParserBase(IOContext ctxt, int features, Reader r)
    {
        super(ctxt, features);
        _reader = r;
        _inputBuffer = ctxt.allocTokenBuffer();
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

        if (_reader != null) {
            int count = _reader.read(_inputBuffer, 0, _inputBuffer.length);
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

    protected char getNextChar(String eofMsg)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                _reportInvalidEOF(eofMsg);
            }
        }
        return _inputBuffer[_inputPtr++];
    }

    @Override
    protected void _closeInput() throws IOException
    {
        /* 25-Nov-2008, tatus: As per [JACKSON-16] we are not to call close()
         *   on the underlying Reader, unless we "own" it, or auto-closing
         *   feature is enabled.
         *   One downside is that when using our optimized
         *   Reader (granted, we only do that for UTF-32...) this
         *   means that buffer recycling won't work correctly.
         */
        if (_reader != null) {
            if (_ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_SOURCE)) {
                _reader.close();
            }
            _reader = null;
        }
    }

    /**
     * Method called to release internal buffers owned by the base
     * reader. This may be called along with {@link #_closeInput} (for
     * example, when explicitly closing this reader instance), or
     * separately (if need be).
     */
    @Override
    protected void _releaseBuffers()
        throws IOException
    {
        super._releaseBuffers();
        char[] buf = _inputBuffer;
        if (buf != null) {
            _inputBuffer = null;
            _ioContext.releaseTokenBuffer(buf);
        }
    }
}
