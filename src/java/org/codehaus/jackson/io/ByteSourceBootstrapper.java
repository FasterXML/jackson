package org.codehaus.jackson.io;

import java.io.*;

import org.codehaus.jackson.JsonParseException;

/**
 * This class is used to determine the encoding of byte stream
 * that is to contain JSON content. Rules are fairly simple, and
 * defined in JSON specification (RFC-4627 or newer), except
 * for BOM handling, which is a property of underlying
 * streams.
 */
public final class ByteSourceBootstrapper
{
    /*
    ////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////
    */

    final IOContext mContext;

    final InputStream mIn;

    /*
    ///////////////////////////////////////////////////////////////
    // Input buffering
    ///////////////////////////////////////////////////////////////
    */

    final byte[] mInputBuffer;

    private int mInputPtr;

    private int mInputLen;

    /*
    ///////////////////////////////////////////////////////////////
    // Input location
    ///////////////////////////////////////////////////////////////
    */

    /**
     * Current number of input units (bytes or chars) that were processed in
     * previous blocks,
     * before contents of current input buffer.
     *<p>
     * Note: includes possible BOMs, if those were part of the input.
     */
    protected int mInputProcessed;

    /*
    ///////////////////////////////////////////////////////////////
    // Data gathered
    ///////////////////////////////////////////////////////////////
    */

    boolean mBigEndian = true;
    int mBytesPerChar = 0; // 0 means "dunno yet"

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    private ByteSourceBootstrapper(IOContext ctxt, InputStream in)
    {
        mContext = ctxt;
        mIn = in;
        mInputBuffer = ctxt.allocReadIOBuffer();
        mInputLen = mInputPtr = 0;
        mInputProcessed = 0;
    }

    private ByteSourceBootstrapper(IOContext ctxt, byte[] inputBuffer, int inputStart, int inputLen)
    {
        mContext = ctxt;
        mIn = null;
        mInputBuffer = inputBuffer;
        mInputPtr = inputStart;
        mInputLen = (inputStart + inputLen);
        // Need to offset this for correct location info
        mInputProcessed = -inputStart;
    }

    public static Reader bootstrap(IOContext ctxt, InputStream in)
        throws IOException, JsonParseException
    {
        return new ByteSourceBootstrapper(ctxt, in)._bootstrap();
    }

    public static Reader bootstrap(IOContext ctxt, byte[] inputBuffer, int inputStart, int inputLen)
        throws IOException, JsonParseException
    {
        return new ByteSourceBootstrapper(ctxt, inputBuffer, inputStart, inputLen)._bootstrap();
    }

    /*
    /////////////////////////////////////////////////////////////////
    // Internal methods, parsing
    /////////////////////////////////////////////////////////////////
    */

    /**
     * @return Actual reader instance, if possibly valid content found;
     *   exception otherwise
     */
    private Reader _bootstrap()
        throws IOException, JsonParseException
    {
        boolean foundEncoding = false;

        // First things first: BOM handling
        /* Note: we can require 4 bytes to be read, since no
         * combination of BOM + valid JSON content can have
         * shorter length (shortest valid JSON content is single
         * digit char, but BOMs are chosen such that combination
         * is always at least 4 chars long)
         */
        if (ensureLoaded(4)) {
            int quad =  (mInputBuffer[mInputPtr] << 24)
                | ((mInputBuffer[mInputPtr+1] & 0xFF) << 16)
                | ((mInputBuffer[mInputPtr+2] & 0xFF) << 8)
                | (mInputBuffer[mInputPtr+3] & 0xFF);
            
            if (handleBOM(quad)) {
                foundEncoding = true;
            } else {
                /* If no BOM, need to auto-detect based on first char;
                 * this works since it must be 7-bit ascii (wrt. unicode
                 * compatible encodings, only ones JSON can be transferred
                 * over)
                 */
                // UTF-32?
                if (checkUTF32(quad)) {
                    foundEncoding = true;
                } else if (checkUTF16(quad >>> 16)) {
                    foundEncoding = true;
                }
            }
        } else if (ensureLoaded(2)) {
            int i16 = ((mInputBuffer[mInputPtr] & 0xFF) << 8)
                | (mInputBuffer[mInputPtr+1] & 0xFF);
            if (checkUTF16(i16)) {
                foundEncoding = true;
            }
        }

        /* Not found yet? As per specs, this means it must be UTF-8. */
        Reader r;
        String enc;

        if (!foundEncoding) {
            enc = "UTF-8";
            r = new UTF8Reader(mContext, mIn, mInputBuffer, mInputPtr, mInputLen);
        } else if (mBytesPerChar == 2) {
            enc = mBigEndian ? "UTF-16BE" : "UTF-16LE";
            mContext.setEncoding(enc);

            // First: do we have a Stream? If not, need to create one:
            InputStream in = mIn;
            if (in == null) {
                in = new ByteArrayInputStream(mInputBuffer, mInputPtr, mInputLen);
            } else {
                /* Also, if we have any read but unused input (usually true),
                 * need to merge that input in:
                 */
                if (mInputPtr < mInputLen) {
                    in = new MergedStream(mContext, in, mInputBuffer, mInputPtr, mInputLen);
                }
            }
            r = new InputStreamReader(in, enc);
        } else if (mBytesPerChar == 4) {
            enc = mBigEndian ? "UTF-32BE" : "UTF-32LE";
            r = new UTF32Reader(mContext, mIn, mInputBuffer, mInputPtr, mInputLen,
                                mBigEndian);
        } else {
            throw new Error("Internal error"); // should never get here
        }
        mContext.setEncoding(enc);
        return r;
    }

    /**
     * @return True if a BOM was succesfully found, and encoding
     *   thereby recognized.
     */
    private boolean handleBOM(int quad)
        throws IOException
    {
        /* Handling of (usually) optional BOM (required for
         * multi-byte formats); first 32-bit charsets:
         */
        switch (quad) {
        case 0x0000FEFF:
            mBigEndian = true;
            mInputPtr += 4;
            mBytesPerChar = 4;
            return true;
        case 0xFFFE0000: // UCS-4, LE?
            mInputPtr += 4;
            mBytesPerChar = 4;
            mBigEndian = false;
            return true;
        case 0x0000FFFE: // UCS-4, in-order...
            reportWeirdUCS4("2143"); // throws exception
        case 0xFEFF0000: // UCS-4, in-order...
            reportWeirdUCS4("3412"); // throws exception
        }
        // Ok, if not, how about 16-bit encoding BOMs?
        int msw = quad >>> 16;
        if (msw == 0xFEFF) { // UTF-16, BE
            mInputPtr += 2;
            mBytesPerChar = 2;
            mBigEndian = true;
            return true;
        }
        if (msw == 0xFFFE) { // UTF-16, LE
            mInputPtr += 2;
            mBytesPerChar = 2;
            mBigEndian = false;
            return true;
        }
        // And if not, then UTF-8 BOM?
        if ((quad >>> 8) == 0xEFBBBF) { // UTF-8
            mInputPtr += 3;
            mBytesPerChar = 1;
            mBigEndian = true; // doesn't really matter
            return true;
        }
        return false;
    }

    private boolean checkUTF32(int quad)
        throws IOException
    {
        /* Handling of (usually) optional BOM (required for
         * multi-byte formats); first 32-bit charsets:
         */
        if ((quad >> 8) == 0) { // 0x000000?? -> UTF32-BE
            mBigEndian = true;
        } else if ((quad & 0x00FFFFFF) == 0) { // 0x??000000 -> UTF32-LE
            mBigEndian = false;
        } else if ((quad & ~0x00FF0000) == 0) { // 0x00??0000 -> UTF32-in-order
            reportWeirdUCS4("3412");
        } else if ((quad & ~0x0000FF00) == 0) { // 0x0000??00 -> UTF32-in-order
            reportWeirdUCS4("2143");
        } else {
            // Can not be valid UTF-32 encoded JSON...
            return false;
        }
        mInputPtr += 4;
        mBytesPerChar = 4;
        return true;
    }

    private boolean checkUTF16(int i16)
    {
        if ((i16 & 0xFF00) == 0) { // UTF-16BE
            mBigEndian = true;
        } else if ((i16 & 0x00FF) == 0) { // UTF-16LE
            mBigEndian = false;
        } else { // nope, not  UTF-16
            return false;
        }
        mInputPtr += 2;
        mBytesPerChar = 2;
        return true;
    }

    /*
    /////////////////////////////////////////////////////////////////
    // Internal methods, problem reporting
    /////////////////////////////////////////////////////////////////
    */

    private void reportWeirdUCS4(String type)
        throws IOException
    {
        throw new CharConversionException("Unsupported UCS-4 endianness ("+type+") detected");
    }

    /*
    /////////////////////////////////////////////////////////////////
    // Internal methods, raw input access
    /////////////////////////////////////////////////////////////////
    */

    protected boolean ensureLoaded(int minimum)
        throws IOException
    {
        /* Let's assume here buffer has enough room -- this will always
         * be true for the limited used this method gets
         */
        int gotten = (mInputLen - mInputPtr);
        while (gotten < minimum) {
            int count;

            if (mIn == null) { // block source
                count = -1;
            } else {
                count = mIn.read(mInputBuffer, mInputLen, mInputBuffer.length - mInputLen);
            }
            if (count < 1) {
                return false;
            }
            mInputLen += count;
            gotten += count;
        }
        return true;
    }
}

