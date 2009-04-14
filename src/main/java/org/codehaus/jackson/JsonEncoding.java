package org.codehaus.jackson;

/**
 * Legal JSON content always uses an Unicode encoding from this
 * short list of allowed (as per RFC) encoding.
 * As such we can just enumerate all legal types here
 *<p>
 * Note: if using apps want to explicitly disregarding Encoding
 * limitations, they can use Reader/Writer instances as input
 * output sources.
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
