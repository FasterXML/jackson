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

public final class JsonFactory
{
    /**
     * Legal JSON content always uses an Unicode encoding from the
     * small list. As such we can just enumerate all legal types
     * here
     */
    public enum Encoding {
        UTF8("UTF-8"),
            UTF16_BE("UTF-16BE"),
            UTF16_LE("UTF-16LE"),
            UTF32_BE("UTF-32BE"),
            UTF32_LE("UTF-32LE")
            ;

        final String mJavaName;

        Encoding(String javaName) { mJavaName = javaName; }

        public String getJavaName() { return mJavaName; }
    }

    /**
     * This <code>ThreadLocal</code> contains a {@link SoftRerefence}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final static ThreadLocal<SoftReference<BufferRecycler>> mRecyclerRef = new ThreadLocal<SoftReference<BufferRecycler>>();

    /**
     * Each factory comes equipped with a shared root symbol table.
     * It should not be linked back to the original blueprint, to
     * avoid contents from leaking between factories.
     */
    private SymbolTable mCharSymbols = SymbolTable.createRoot();

    /**
     * Alternative to the basic symbol table, some stream-based
     * parsers use different name canonicalization method.
     *<p>
     * TODO: should clean up this; looks messy having 2 alternatives
     * with not very clear differences.
     */
    private NameCanonicalizer mByteSymbols = NameCanonicalizer.createRoot();

    public JsonFactory() { }

    /*
    //////////////////////////////////////////////////////
    // Reader factories
    //////////////////////////////////////////////////////
     */

    public JsonParser createJsonParser(File f)
        throws IOException, JsonParseException
    {
        return ByteSourceBootstrapper.bootstrap
            (createContext(f), new FileInputStream(f), mCharSymbols, mByteSymbols);
    }

    public JsonParser createJsonParser(URL url)
        throws IOException, JsonParseException
    {
        InputStream in = optimizedStreamFromURL(url);
        return ByteSourceBootstrapper.bootstrap
            (createContext(url), in, mCharSymbols, mByteSymbols);
    }

    /**
     *<p>
     * Note: no encoding argument is taken since it can always be
     * auto-detected as suggested by Json RFC.
     */
    public JsonParser createJsonParser(InputStream in)
        throws IOException, JsonParseException
    {
        return ByteSourceBootstrapper.bootstrap
            (createContext(in), in, mCharSymbols, mByteSymbols);
    }

    public JsonParser createJsonParser(Reader r)
        throws IOException, JsonParseException
    {
        return new ReaderBasedParser(createContext(r), r, mCharSymbols.makeChild());
    }

    /*
    //////////////////////////////////////////////////////
    // Generator factories
    //////////////////////////////////////////////////////
     */

    public JsonGenerator createJsonGenerator(OutputStream out, Encoding enc)
        throws IOException
    {
        IOContext ctxt = createContext(out);
        ctxt.setEncoding(enc.getJavaName());
        if (enc == Encoding.UTF8) { // We have optimized writer for UTF-8
            return new WriterBasedGenerator(ctxt, new UTF8Writer(ctxt, out));
        }
        return new WriterBasedGenerator(ctxt, new OutputStreamWriter(out, enc.getJavaName()));
    }

    public JsonGenerator createJsonGenerator(Writer out)
        throws IOException
    {
        IOContext ctxt = createContext(out);
        return new WriterBasedGenerator(ctxt, out);
    }

    public JsonGenerator createJsonGenerator(File f, Encoding enc)
        throws IOException
    {
        return createJsonGenerator(new FileOutputStream(f), enc);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
     */

    protected IOContext createContext(Object srcRef)
    {
        return new IOContext(getBufferRecycler(), srcRef);
    }

    protected BufferRecycler getBufferRecycler()
    {
        SoftReference<BufferRecycler> ref = mRecyclerRef.get();
        BufferRecycler br = (ref == null) ? null : ref.get();

        if (br == null) {
            br = new BufferRecycler();
            if (ref == null) {
                mRecyclerRef.set(new SoftReference<BufferRecycler>(br));
            }
        }
        return br;
    }

    public static InputStream optimizedStreamFromURL(URL url)
        throws IOException
    {
        if ("file".equals(url.getProtocol())) {
            /* Can not do this if the path refers
             * to a network drive on windows. This fixes the problem;
             * might not be needed on all platforms (NFS?), but should not
             * matter a lot: performance penalty of extra wrapping is more
             * relevant when accessing local file system.
             */
            if (url.getHost() == null) {
                return new FileInputStream(url.getPath());
            }
        }
        return url.openStream();
    }
}
