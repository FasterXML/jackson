package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.util.TextBuffer;

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
    protected Reader mReader;

    /*
    ////////////////////////////////////////////////////
    // Current input data
    ////////////////////////////////////////////////////
     */

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source.
     */
    protected char[] mInputBuffer;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected ReaderBasedParserBase(IOContext ctxt, Reader r)
    {
        super(ctxt);
        mReader = r;
        mInputBuffer = ctxt.allocTokenBuffer();
    }

    /*
    ////////////////////////////////////////////////////
    // Low-level reading, other
    ////////////////////////////////////////////////////
     */

    protected final boolean loadMore()
        throws IOException
    {
        mCurrInputProcessed += mInputLast;
        mCurrInputRowStart -= mInputLast;

        if (mReader != null) {
            int count = mReader.read(mInputBuffer, 0, mInputBuffer.length);
            if (count > 0) {
                mInputPtr = 0;
                mInputLast = count;
                return true;
            }
            // End of input
            closeInput();
            // Should never return 0, so let's fail
            if (count == 0) {
                throw new IOException("Reader returned 0 characters when trying to read "+mInputLast);
            }
        }
        return false;
    }


    protected char getNextChar(String eofMsg)
        throws IOException, JsonParseException
    {
        if (mInputPtr >= mInputLast) {
            if (!loadMore()) {
                reportInvalidEOF(eofMsg);
            }
        }
        return mInputBuffer[mInputPtr++];
    }

    protected void closeInput()
        throws IOException
    {
        Reader r = mReader;
        if (r != null) {
            mReader = null;
            /* Reader takes care of returning buffers it uses. Likewise,
             * we need to take care of returning temporary buffers
             * we have allocated.
             */
            r.close();
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
        char[] buf = mInputBuffer;
        if (buf != null) {
            mInputBuffer = null;
            mIOContext.releaseTokenBuffer(buf);
        }
    }
}
