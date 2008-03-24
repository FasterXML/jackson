package org.codehaus.jackson;

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

    public Object getSourceRef() { return mSourceRef; }
    public int getLineNr() { return mLineNr; }
    public int getColumnNr() { return mColumnNr; }

    public long getCharOffset() { return mTotalChars; }

    public long getByteOffset()
    {
        /* Unfortunately, none of legal encodings are straight single-byte
         * encodings. Could determine offset for UTF-16/UTF-32, but the
         * most important one is UTF-8... so for now, let's just not
         * report anything.
         */
        return -1;
    }

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
