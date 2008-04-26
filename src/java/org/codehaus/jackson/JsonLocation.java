package org.codehaus.jackson;

/**
 * Object that encapsulates Location information used to report
 * parsing or generation errors, as well as current location
 * within input or output streams.
 */
public class JsonLocation
{
    final long mTotalChars;

    final int mLineNr;
    final int mColumnNr;

    /**
     * Displayable description for input source: file path, url
     */
    final Object mSourceRef;

    public JsonLocation(Object srcRef, long totalChars, int lineNr, int colNr)
    {
        mTotalChars = totalChars;
        mLineNr = lineNr;
        mColumnNr = colNr;
        mSourceRef = srcRef;
    }

    /**
     * Reference to the original resource being read, if one available.
     * For example, when a parser has been constructed by passing
     * a {@link java.io.File} instance, this method would return
     * that File. Will return null if no such reference is available,
     * for example when {@link java.io.InputStream} was used to
     * construct the parser instance.
     */
    public Object getSourceRef() { return mSourceRef; }

    /**
     * @return Line number of the location (1-based)
     */
    public int getLineNr() { return mLineNr; }

    /**
     * @return Column number of the location (1-based)
     */
    public int getColumnNr() { return mColumnNr; }

    /**
     * @return Character offset within underlying stream, reader or writer,
     *   if available; -1 if not.
     */
    public long getCharOffset() { return mTotalChars; }

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
        if (mSourceRef == null) {
            sb.append("UNKNOWN");
        } else {
            sb.append(mSourceRef.toString());
        }
        sb.append("; line: ");
        sb.append(mLineNr);
        sb.append(", column: ");
        sb.append(mColumnNr);
        sb.append(']');
        return sb.toString();
    }
}
