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
import org.codehaus.jackson.sym.SymbolTable;
import org.codehaus.jackson.util.BufferRecycler;

/**
 * JsonFactory is the main factory class of Jackson package.
 * It is used for constructing streaming parser
 * (aka readers, deserializers) and generators (aka writers, serializers).
 *<p>
 * Factory instances are thread-safe and reusable after configuration
 * (if any). Typically applications and services use only a single
 * globally shared factory instance, unless they need differently
 * configured factories.
 *<p>
 * Creation of a factory instance is a light-weight operation,
 * and since there is no need for pluggable alternative implementations
 * (as there is no "standard" json processor API to implement),
 * the default constructor is used for constructing factory
 * instances.
 *
 * @author Tatu Saloranta
 */
public final class JsonFactory
{
    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_PARSER_FEATURE_FLAGS = JsonParser.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_GENERATOR_FEATURE_FLAGS = JsonGenerator.Feature.collectDefaults();

    /*
    ///////////////////////////////////////////////////////
    // Buffer, symbol table management
    ///////////////////////////////////////////////////////
     */

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

    /*
    ///////////////////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////////////////
     */

    private int _parserFeatures = DEFAULT_PARSER_FEATURE_FLAGS;

    private int _generatorFeatures = DEFAULT_GENERATOR_FEATURE_FLAGS;

    /**
     * Default constructor used to create factory instances.
     * Creation of a factory instance is a light-weight operation,
     * but it is still a good idea to reuse limited number of
     * factory instances (and quite often just a single instance):
     * factories are used as context for storing some reused
     * processing objects (such as symbol tables parsers use)
     * and this reuse only works within context of a single
     * factory instance.
     */
    public JsonFactory() { }

    /*
    //////////////////////////////////////////////////////
    // Configuration
    //////////////////////////////////////////////////////
     */

    /**
     * Method for enabling specified parser features
     * (check {@link JsonParser.Feature} for list of features)
     */
    public void enableParserFeature(JsonParser.Feature f) {
        _parserFeatures |= f.getMask();
    }

    /**
     * Method for disabling specified parser features
     * (check {@link JsonParser.Feature} for list of features)
     */
    public void disableParserFeature(JsonParser.Feature f) {
        _parserFeatures &= ~f.getMask();
    }

    public void setParserFeature(JsonParser.Feature f, boolean state) {
        if (state) {
            enableParserFeature(f);
        } else {
            disableParserFeature(f);
        }
    }

    public boolean isParserFeatureEnabled(JsonParser.Feature f) {
        return (_parserFeatures & f.getMask()) != 0;
    }

    /**
     * Method for enabling specified generator features
     * (check {@link JsonGenerator.Feature} for list of features)
     */
    public void enableGeneratorFeature(JsonGenerator.Feature f) {
        _generatorFeatures |= f.getMask();
    }

    /**
     * Method for disabling specified generator features
     * (check {@link JsonGenerator.Feature} for list of features)
     */
    public void disableGeneratorFeature(JsonGenerator.Feature f) {
        _generatorFeatures &= ~f.getMask();
    }

    public void setGeneratorFeature(JsonGenerator.Feature f, boolean state) {
        if (state) {
            enableGeneratorFeature(f);
        } else {
            disableGeneratorFeature(f);
        }
    }

    public boolean isGeneratorFeatureEnabled(JsonGenerator.Feature f) {
        return (_generatorFeatures & f.getMask()) != 0;
    }

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
        return _createJsonParser(new FileInputStream(f), _createContext(f, true));
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
        return _createJsonParser(_optimizedStreamFromURL(url), _createContext(url, true));
    }

    /**
     * Method for constructing json parser instance to parse
     * the contents accessed via specified input stream.
     *<p>
     * The input stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link org.codehaus.jackson.JsonParser.Feature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *<p>
     * Note: no encoding argument is taken since it can always be
     * auto-detected as suggested by Json RFC.
     *
     * @param in InputStream to use for reading JSON content to parse
     */
    public JsonParser createJsonParser(InputStream in)
        throws IOException, JsonParseException
    {
        return _createJsonParser(in, _createContext(in, false));
    }

    /**
     * Method for constructing json parser instance to parse
     * the contents accessed via specified Reader.
     <p>
     * The read stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link org.codehaus.jackson.JsonParser.Feature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *<p>
     *
     * @param r Reader to use for reading JSON content to parse
     */
    public JsonParser createJsonParser(Reader r)
        throws IOException, JsonParseException
    {
        return new ReaderBasedParser(_createContext(r, false), _parserFeatures, r, _charSymbols.makeChild());
    }

    public JsonParser createJsonParser(byte[] data, int offset, int len)
        throws IOException, JsonParseException
    {
        // !!! TODO: make efficient (see [JACKSON-24])
        InputStream in = new ByteArrayInputStream(data, offset, len);
        // true -> must be managed as caller didn't hand stream
        return _createJsonParser(in,  _createContext(in, true));
    }

    public final JsonParser createJsonParser(byte[] data)
        throws IOException, JsonParseException
    {
        return createJsonParser(data, 0, data.length);
    }

    public final JsonParser createJsonParser(String content)
        throws IOException, JsonParseException
    {
        StringReader r = new StringReader(content);
        // true -> must be managed as caller didn't hand Reader
        return new ReaderBasedParser(_createContext(r, true), _parserFeatures, r, _charSymbols.makeChild());
    }

    private JsonParser _createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return new ByteSourceBootstrapper(ctxt, in).constructParser(_parserFeatures, _byteSymbols, _charSymbols);
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
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link org.codehaus.jackson.JsonGenerator.Feature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *
     * @param out OutputStream to use for writing json content 
     * @param enc Character encoding to use
     */
    public JsonGenerator createJsonGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        IOContext ctxt = _createContext(out, false);
        ctxt.setEncoding(enc);
        if (enc == JsonEncoding.UTF8) { // We have optimized writer for UTF-8
            return new WriterBasedGenerator(ctxt, _generatorFeatures, new UTF8Writer(ctxt, out));
        }
        return new WriterBasedGenerator(ctxt, _generatorFeatures, new OutputStreamWriter(out, enc.getJavaName()));
    }

    /**
     * Method for constructing json generator for writing json content
     * using specified Writer.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link org.codehaus.jackson.JsonGenerator.Feature#AUTO_CLOSE_TARGET} is enabled).
     * Using application needs to close it explicitly.
     *
     * @param out Writer to use for writing json content 
     */
    public JsonGenerator createJsonGenerator(Writer out)
        throws IOException
    {
        IOContext ctxt = _createContext(out, false);
        return new WriterBasedGenerator(ctxt, _generatorFeatures, out);
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
    protected IOContext _createContext(Object srcRef, boolean resourceManaged)
    {
        return new IOContext(_getBufferRecycler(), srcRef, resourceManaged);
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
