package org.codehaus.jackson.impl;

import java.io.*;

import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

/**
 * Intermediate class that implements handling of numeric parsing,
 * when using UTF-8 encoded byte-based input source.
 * Separate from the actual parser class just to isolate numeric
 * parsing: would be nice to use aggregation, but unfortunately
 * many parts are hard to implement without direct access to
 * underlying buffers.
 */
public abstract class Utf8NumericParser
    extends StreamBasedParserBase
{
    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    public Utf8NumericParser(IOContext pc, InputStream in,
                             byte[] inputBuffer, int start, int end,
                             boolean bufferRecyclable)
    {
        super(pc, in, inputBuffer, start, end, bufferRecyclable);
    }

    /*
    ////////////////////////////////////////////////////
    // Textual parsing of number values
    ////////////////////////////////////////////////////
     */

    /**
     * Initial parsing method for number values. It needs to be able
     * to parse enough input to be able to determine whether the
     * value is to be considered a simple integer value, or a more
     * generic decimal value: latter of which needs to be expressed
     * as a floating point number. The basic rule is that if the number
     * has no fractional or exponential part, it is an integer; otherwise
     * a floating point number.
     *<p>
     * Because much of input has to be processed in any case, no partial
     * parsing is done: all input text will be stored for further
     * processing. However, actual numeric value conversion will be
     * deferred, since it is usually the most complicated and costliest
     * part of processing.
     */
    protected final JsonToken parseNumberText(int ch)
        throws IOException, JsonParseException
    {
        // !!! TBI
        return null;
    }

    /**
     * Method called to parse a number, when the primary parse
     * method has failed to parse it, due to it being split on
     * buffer boundary. As a result code is very similar, except
     * that it has to explicitly copy contents to the text buffer
     * instead of just sharing the main input buffer.
     */
    private final JsonToken parseNumberText2(boolean negative)
        throws IOException, JsonParseException
    {
        // !!! TBI
        return null;
    }
}
