package org.codehaus.jackson;

/**
 * Enumeration that defines legal encodings that can be used
 * for JSON content, based on list of allowed encodings from
 * <a href="http://www.ietf.org/rfc/rfc4627.txt">JSON specification</a>.
 *<p>
 * Note: if application want to explicitly disregard Encoding
 * limitations (to read in JSON encoded using an encoding not
 * listed as allowed), they can use {@link java.io.Reader} /
 * {@link java.io.Writer} instances as input
 * source (or output target).
 */
public enum JsonEncoding {
    UTF8("UTF-8", false), // N/A for big-endian, really
        UTF16_BE("UTF-16BE", true),
        UTF16_LE("UTF-16LE", false),
        UTF32_BE("UTF-32BE", true),
        UTF32_LE("UTF-32LE", false)
        ;
    
    final String mJavaName;

    final boolean mBigEndian;
    
    JsonEncoding(String javaName, boolean bigEndian)
    {
        mJavaName = javaName;
        mBigEndian = bigEndian;
    }

    /**
     * Method for accessing encoding name that JDK will support.
     *
     * @return Matching encoding name that JDK will support.
     */
    public String getJavaName() { return mJavaName; }

    /**
     * Whether encoding is big-endian (if encoding supports such
     * notion). If no such distinction is made (as is the case for
     * {@link #UTF8}), return value is undefined.
     *
     * @return True for big-endian encodings; false for little-endian
     *   (or if not applicable)
     */
    public boolean isBigEndian() { return mBigEndian; }
}
