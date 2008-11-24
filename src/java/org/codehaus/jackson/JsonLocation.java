package org.codehaus.jackson;

/**
 * Object that encapsulates Location information used to report
 * parsing or generation errors, as well as current location
 * within input or output streams.
 */
public class JsonLocation
{
    final long _totalChars;

    final int _lineNr;
    final int _columnNr;

    /**
     * Displayable description for input source: file path, url
     */
    final Object _sourceRef;

    public JsonLocation(Object srcRef, long totalChars, int lineNr, int colNr)
    {
        _totalChars = totalChars;
        _lineNr = lineNr;
        _columnNr = colNr;
        _sourceRef = srcRef;
    }

    /**
     * Reference to the original resource being read, if one available.
     * For example, when a parser has been constructed by passing
     * a {@link java.io.File} instance, this method would return
     * that File. Will return null if no such reference is available,
     * for example when {@link java.io.InputStream} was used to
     * construct the parser instance.
     */
    public Object getSourceRef() { return _sourceRef; }

    /**
     * @return Line number of the location (1-based)
     */
    public int getLineNr() { return _lineNr; }

    /**
     * @return Column number of the location (1-based)
     */
    public int getColumnNr() { return _columnNr; }

    /**
     * @return Character offset within underlying stream, reader or writer,
     *   if available; -1 if not.
     */
    public long getCharOffset() { return _totalChars; }

    /**
     * @return Byte offset within underlying stream, reader or writer,
     *   if available; -1 if not.
     */
    public long getByteOffset()
    {
        /* Unfortunately, none of legal encodings are straight single-byte
         * encodings. Could determine offset for UTF-16/UTF-32, but the
         * most important one is UTF-8... so for now, let's just not
         * report anything.
         */
        return -1;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(80);
        sb.append("[Source: ");
        if (_sourceRef == null) {
            sb.append("UNKNOWN");
        } else {
            sb.append(_sourceRef.toString());
        }
        sb.append("; line: ");
        sb.append(_lineNr);
        sb.append(", column: ");
        sb.append(_columnNr);
        sb.append(']');
        return sb.toString();
    }
}
