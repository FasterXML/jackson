package org.codehaus.jackson;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;

import org.codehaus.jackson.io.*;
import org.codehaus.jackson.impl.ReaderBasedParser;
import org.codehaus.jackson.impl.WriterBasedGenerator;
import org.codehaus.jackson.sym.NameCanonicalizer;
import org.codehaus.jackson.util.BufferRecycler;
import org.codehaus.jackson.util.SymbolTable;

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
    UTF8("UTF-8"),
        UTF16_BE("UTF-16BE"),
        UTF16_LE("UTF-16LE"),
        UTF32_BE("UTF-32BE"),
        UTF32_LE("UTF-32LE")
        ;
    
    final String mJavaName;
    
    JsonEncoding(String javaName) { mJavaName = javaName; }
    
    public String getJavaName() { return mJavaName; }
}
