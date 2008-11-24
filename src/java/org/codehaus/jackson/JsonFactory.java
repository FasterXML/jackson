/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code and binary code bundles.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * JsonFactory is the main factory class of Jackson package.
 * It is used for constructing streaming parser (readers) and
 * generators (writers.
 *<p>
 * Factory instances are thread-safe and reusable after configuration
 * (if any). Typically applications and services use only a single
 * globally shared factory instance, unless they need differently
 * configured factories.
 *
 * @author Tatu Saloranta
 */
public final class JsonFactory
{
    /**
     * This <code>ThreadLocal</code> contains a {@link SoftRerefence}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final static ThreadLocal<SoftReference<BufferRecycler>> _recyclerRef = new ThreadLocal<SoftReference<BufferRecycler>>();

    /**
     * Each factory comes equipped with a shared root symbol table.
     * It should not be linked back to the original blueprint, to
     * avoid contents from leaking between factories.
     */
    private SymbolTable _charSymbols = SymbolTable.createRoot();

    /**
     * Alternative to the basic symbol table, some stream-based
     * parsers use different name canonicalization method.
     *<p>
     * TODO: should clean up this; looks messy having 2 alternatives
     * with not very clear differences.
     */
    private NameCanonicalizer _byteSymbols = NameCanonicalizer.createRoot();

    /**
     * Creation of a factory instance is quite light-weight operation,
     * and since there is no need for pluggable alternative implementations
     * (since there is no "standard" json processor API to implement),
     * default constructor is used for constructing factories.
     * Also, there is no separation between parser and generator
     * construction.
     */
    public JsonFactory() { }

    /*
    //////////////////////////////////////////////////////
    // Reader factories
    //////////////////////////////////////////////////////
     */

    /**
     * Method for constructing json parser instance to parse
     * contents of specified file. Encoding is auto-detected
     * from contents according to json specification recommended
     * mechanism.
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param f File that contains JSON content to parse
     */
    public JsonParser createJsonParser(File f)
        throws IOException, JsonParseException
    {
        return _createJsonParser(new FileInputStream(f), _createContext(f));
    }

    /**
     * Method for constructing json parser instance to parse
     * contents of resource reference by given URL.
     * Encoding is auto-detected
     * from contents according to json specification recommended
     * mechanism.
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param url URL pointing to resource that contains JSON content to parse
     */
    public JsonParser createJsonParser(URL url)
        throws IOException, JsonParseException
    {
        return _createJsonParser(_optimizedStreamFromURL(url), _createContext(url));
    }

    /**
     * Method for constructing json parser instance to parse
     * the contents accessed via specified input stream.
     *<p>
     * Input stream will <b>NOT be owned</b> (not managed) by
     * the parser, since caller does have access to it and
     * is expected to close it if and as necessary.
     *<p>
     * Note: no encoding argument is taken since it can always be
     * auto-detected as suggested by Json RFC.
     *
     * @param in InputStream to use for reading JSON content to parse
     */
    public JsonParser createJsonParser(InputStream in)
        throws IOException, JsonParseException
    {
        return _createJsonParser(in, _createContext(in));
    }

    /**
     * Method for constructing json parser instance to parse
     * the contents accessed via specified Reader.
     *<p>
     * Reader will <b>NOT be owned</b> (not managed) by
     * the parser, since caller does have access to it and
     * is expected to close it if and as necessary.
     *
     * @param r Reader to use for reading JSON content to parse
     */
    public JsonParser createJsonParser(Reader r)
        throws IOException, JsonParseException
    {
        return new ReaderBasedParser(_createContext(r), r, _charSymbols.makeChild());
    }

    private JsonParser _createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        ByteSourceBootstrapper bb = new ByteSourceBootstrapper(ctxt, in);
        JsonEncoding enc = bb.detectEncoding();
        if (enc == JsonEncoding.UTF8) {
            return bb.createFastUtf8Parser(_byteSymbols.makeChild());
        }
        return new ReaderBasedParser(ctxt, bb.constructReader(), _charSymbols.makeChild());
    }

    /*
    //////////////////////////////////////////////////////
    // Generator factories
    //////////////////////////////////////////////////////
     */

    /**
     * Method for constructing json generator for writing json content
     * using specified output stream.
     * Encoding to use must be specified, and needs to be one of available
     * types (as per JSON specification).
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the output stream when
     * {@link JsonGenerator#close} is called. 
     * Using application needs to close it explicitly.
     *
     * @param out OutputStream to use for writing json content 
     * @param enc Character encoding to use
     */
    public JsonGenerator createJsonGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        IOContext ctxt = _createContext(out);
        ctxt.setEncoding(enc);
        if (enc == JsonEncoding.UTF8) { // We have optimized writer for UTF-8
            return new WriterBasedGenerator(ctxt, new UTF8Writer(ctxt, out));
        }
        return new WriterBasedGenerator(ctxt, new OutputStreamWriter(out, enc.getJavaName()));
    }

    /**
     * Method for constructing json generator for writing json content
     * using specified Writer.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called. Using application
     * needs to close Writer explicitly.
     *
     * @param out Writer to use for writing json content 
     */
    public JsonGenerator createJsonGenerator(Writer out)
        throws IOException
    {
        IOContext ctxt = _createContext(out);
        return new WriterBasedGenerator(ctxt, out);
    }

    /**
     * Method for constructing json generator for writing json content
     * to specified file, overwriting contents it might have (or creating
     * it if such file does not yet exist).
     * Encoding to use must be specified, and needs to be one of available
     * types (as per JSON specification).
     *<p>
     * Underlying stream <b>is owned</b> by the generator constructed,
     * i.e. generator will handle closing of file when
     * {@link JsonGenerator#close} is called.
     *
     * @param f File to write contents to
     * @param enc Character encoding to use
     */
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

    /**
     * Method used by the factory to create parsing context for parser
     * instances.
     */
    protected IOContext _createContext(Object srcRef)
    {
        return new IOContext(_getBufferRecycler(), srcRef);
    }

    /**
     * Method used by factory to create buffer recycler instances
     * for parsers and generators.
     */
    protected BufferRecycler _getBufferRecycler()
    {
        SoftReference<BufferRecycler> ref = _recyclerRef.get();
        BufferRecycler br = (ref == null) ? null : ref.get();

        if (br == null) {
            br = new BufferRecycler();
            if (ref == null) {
                _recyclerRef.set(new SoftReference<BufferRecycler>(br));
            }
        }
        return br;
    }

    /**
     * Helper methods used for constructing an optimal stream for
     * parsers to use, when input is to be read from an URL.
     * This helps when reading file content via URL.
     */
    protected static InputStream _optimizedStreamFromURL(URL url)
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
