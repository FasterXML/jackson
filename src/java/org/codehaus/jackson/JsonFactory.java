package org.codehaus.jackson;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;

import org.codehaus.jackson.io.*;
import org.codehaus.jackson.impl.ByteSourceBootstrapper;
import org.codehaus.jackson.impl.ReaderBasedParser;
import org.codehaus.jackson.impl.WriterBasedGenerator;
import org.codehaus.jackson.sym.NameCanonicalizer;
import org.codehaus.jackson.util.BufferRecycler;
import org.codehaus.jackson.util.SymbolTable;

public final class JsonFactory
{
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
        return createJsonParser(new FileInputStream(f), createContext(f));
    }

    public JsonParser createJsonParser(URL url)
        throws IOException, JsonParseException
    {
        return createJsonParser(optimizedStreamFromURL(url), createContext(url));
    }

    /**
     *<p>
     * Note: no encoding argument is taken since it can always be
     * auto-detected as suggested by Json RFC.
     */
    public JsonParser createJsonParser(InputStream in)
        throws IOException, JsonParseException
    {
        return createJsonParser(in, createContext(in));
    }

    public JsonParser createJsonParser(InputStream in, boolean fast)
        throws IOException, JsonParseException
    {
        return createJsonParser(in, createContext(in), fast);
    }

    private JsonParser createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return createJsonParser(in, ctxt, true);
    }

    /* !!! For testing alternative implementations
     */
    private JsonParser createJsonParser(InputStream in, IOContext ctxt,
                                        boolean fast)
        throws IOException, JsonParseException
    {
        ByteSourceBootstrapper bb = new ByteSourceBootstrapper(ctxt, in);
        JsonEncoding enc = bb.detectEncoding();
        if (fast && enc == JsonEncoding.UTF8) {
            return bb.createFastUtf8Parser(mByteSymbols);
        }
        return new ReaderBasedParser(ctxt, bb.constructReader(), mCharSymbols.makeChild());
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

    public JsonGenerator createJsonGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        IOContext ctxt = createContext(out);
        ctxt.setEncoding(enc);
        if (enc == JsonEncoding.UTF8) { // We have optimized writer for UTF-8
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

    public JsonGenerator createJsonGenerator(File f, JsonEncoding enc)
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
