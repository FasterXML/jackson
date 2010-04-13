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
import org.codehaus.jackson.sym.BytesToNameCanonicalizer;
import org.codehaus.jackson.sym.CharsToNameCanonicalizer;
import org.codehaus.jackson.util.BufferRecycler;

/**
 * The main factory class of Jackson package, used to configure and
 * construct reader (aka parser, {@link JsonParser})
 * and writer (aka generator, {@link JsonGenerator})
 * instances.
 *<p>
 * Factory instances are thread-safe and reusable after configuration
 * (if any). Typically applications and services use only a single
 * globally shared factory instance, unless they need differently
 * configured factories. Factory reuse is important if efficiency matters;
 * most recycling of expensive construct is done on per-factory basis.
 *<p>
 * Creation of a factory instance is a light-weight operation,
 * and since there is no need for pluggable alternative implementations
 * (as there is no "standard" json processor API to implement),
 * the default constructor is used for constructing factory
 * instances.
 *
 * @author Tatu Saloranta
 */
public class JsonFactory
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
    /******************************************************
    /* Buffer, symbol table management
    /******************************************************
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
    protected CharsToNameCanonicalizer _rootCharSymbols = CharsToNameCanonicalizer.createRoot();

    /**
     * Alternative to the basic symbol table, some stream-based
     * parsers use different name canonicalization method.
     *<p>
     * TODO: should clean up this; looks messy having 2 alternatives
     * with not very clear differences.
     */
    protected BytesToNameCanonicalizer _rootByteSymbols = BytesToNameCanonicalizer.createRoot();

    /*
    /******************************************************
    /* Configuration
    /******************************************************
     */

    /**
     * Object that implements conversion functionality between
     * Java objects and Json content. For base JsonFactory implementation
     * usually not set by default, but can be explicitly set.
     * Sub-classes (like @link org.codehaus.jackson.map.MappingJsonFactory}
     * usually provide an implementation.
     */
    protected ObjectCodec _objectCodec;

    protected int _parserFeatures = DEFAULT_PARSER_FEATURE_FLAGS;

    protected int _generatorFeatures = DEFAULT_GENERATOR_FEATURE_FLAGS;

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
    public JsonFactory() { this(null); }

    public JsonFactory(ObjectCodec oc) { _objectCodec = oc; }

    /*
    /******************************************************
    /* Configuration, parser settings
    /******************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link JsonParser.Feature} for list of features)
     *
     * @since 1.2
     */
    public final JsonFactory configure(JsonParser.Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified parser feature
     * (check {@link JsonParser.Feature} for list of features)
     *
     * @since 1.2
     */
    public JsonFactory enable(JsonParser.Feature f) {
        _parserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link JsonParser.Feature} for list of features)
     *
     * @since 1.2
     */
    public JsonFactory disable(JsonParser.Feature f) {
        _parserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     *
     * @since 1.2
     */
    public final boolean isEnabled(JsonParser.Feature f) {
        return (_parserFeatures & f.getMask()) != 0;
    }

    // // // Older deprecated (as of 1.2) methods

    /**
     * @deprecated Use {@link #enable(JsonParser.Feature)} instead
     */
    public final void enableParserFeature(JsonParser.Feature f) {
        enable(f);
    }

    /**
     * @deprecated Use {@link #disable(JsonParser.Feature)} instead
     */
    public final void disableParserFeature(JsonParser.Feature f) {
        disable(f);
    }

    /**
     * @deprecated Use {@link #configure(JsonParser.Feature, boolean)} instead
     */
    public final void setParserFeature(JsonParser.Feature f, boolean state) {
        configure(f, state);
    }

    /**
     * @deprecated Use {@link #isEnabled(JsonParser.Feature)} instead
     */
    public final boolean isParserFeatureEnabled(JsonParser.Feature f) {
        return (_parserFeatures & f.getMask()) != 0;
    }

    /*
    //////////////////////////////////////////////////////
    // Configuration, generator settings
    //////////////////////////////////////////////////////
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link JsonGenerator.Feature} for list of features)
     *
     * @since 1.2
     */
    public final JsonFactory configure(JsonGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }


    /**
     * Method for enabling specified generator features
     * (check {@link JsonGenerator.Feature} for list of features)
     *
     * @since 1.2
     */
    public JsonFactory enable(JsonGenerator.Feature f) {
        _generatorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link JsonGenerator.Feature} for list of features)
     *
     * @since 1.2
     */
    public JsonFactory disable(JsonGenerator.Feature f) {
        _generatorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified generator feature is enabled.
     *
     * @since 1.2
     */
    public final boolean isEnabled(JsonGenerator.Feature f) {
        return (_generatorFeatures & f.getMask()) != 0;
    }

    // // // Older deprecated (as of 1.2) methods

    /**
     * @deprecated Use {@link #enable(JsonGenerator.Feature)} instead
     */
    public final void enableGeneratorFeature(JsonGenerator.Feature f) {
        enable(f);
    }

    /**
     * @deprecated Use {@link #disable(JsonGenerator.Feature)} instead
     */
    public final void disableGeneratorFeature(JsonGenerator.Feature f) {
        disable(f);
    }

    /**
     * @deprecated Use {@link #configure(JsonGenerator.Feature, boolean)} instead
     */
    public final void setGeneratorFeature(JsonGenerator.Feature f, boolean state) {
        configure(f, state);
    }

    /**
     * @deprecated Use {@link #isEnabled(JsonGenerator.Feature)} instead
     */
    public final boolean isGeneratorFeatureEnabled(JsonGenerator.Feature f) {
        return isEnabled(f);
    }

    /*
    //////////////////////////////////////////////////////
    // Configuration, other
    //////////////////////////////////////////////////////
     */

    public JsonFactory setCodec(ObjectCodec oc) {
        _objectCodec = oc;
        return this;
    }

    public ObjectCodec getCodec() { return _objectCodec; }

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
	return _createJsonParser(r, _createContext(r, false));
    }

    public JsonParser createJsonParser(byte[] data)
        throws IOException, JsonParseException
    {
        return _createJsonParser(data, 0, data.length, _createContext(data, true));
    }

    public JsonParser createJsonParser(byte[] data, int offset, int len)
        throws IOException, JsonParseException
    {
	return _createJsonParser(data, offset, len, _createContext(data, true));
    }

    public JsonParser createJsonParser(String content)
        throws IOException, JsonParseException
    {
	// true -> we own the Reader (and must close); not a big deal
	Reader r = new StringReader(content);
	return _createJsonParser(r, _createContext(r, true));
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
	// false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        ctxt.setEncoding(enc);
	return _createJsonGenerator(_createWriter(out, enc, ctxt), ctxt);
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
	return _createJsonGenerator(out, ctxt);
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
	OutputStream out = new FileOutputStream(f);
	// true -> yes, we have to manage the stream since we created it
        IOContext ctxt = _createContext(out, true);
        ctxt.setEncoding(enc);
	return _createJsonGenerator(_createWriter(out, enc, ctxt), ctxt);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
     */

    /**
     * Overridable construction method that actually instantiates desired generator.
     */
    protected IOContext _createContext(Object srcRef, boolean resourceManaged)
    {
        return new IOContext(_getBufferRecycler(), srcRef, resourceManaged);
    }

    /**
     * Overridable construction method that actually instantiates desired parser.
     */
    protected JsonParser _createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return new ByteSourceBootstrapper(ctxt, in).constructParser(_parserFeatures, _objectCodec, _rootByteSymbols, _rootCharSymbols);
    }

    protected JsonParser _createJsonParser(Reader r, IOContext ctxt)
	throws IOException, JsonParseException
    {
        return new ReaderBasedParser(ctxt, _parserFeatures, r, _objectCodec,
                _rootCharSymbols.makeChild(isEnabled(JsonParser.Feature.CANONICALIZE_FIELD_NAMES),
                    isEnabled(JsonParser.Feature.INTERN_FIELD_NAMES)));
        }

    protected JsonParser _createJsonParser(byte[] data, int offset, int len, IOContext ctxt)
        throws IOException, JsonParseException
    {
        // true -> managed (doesn't really matter; we have no stream!)
        return new ByteSourceBootstrapper(ctxt, data, offset, len).constructParser(_parserFeatures, _objectCodec, _rootByteSymbols, _rootCharSymbols);
    }

    /**
     * Overridable construction method that actually instantiates desired generator
     */
    protected JsonGenerator _createJsonGenerator(Writer out, IOContext ctxt)
        throws IOException
    {
        return new WriterBasedGenerator(ctxt, _generatorFeatures, _objectCodec, out);
    }

    /**
     * Method used by factory to create buffer recycler instances
     * for parsers and generators.
     *<p>
     * Note: only public to give access for <code>ObjectMapper</code>
     */
    public BufferRecycler _getBufferRecycler()
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
    protected InputStream _optimizedStreamFromURL(URL url)
        throws IOException
    {
        if ("file".equals(url.getProtocol())) {
            /* Can not do this if the path refers
             * to a network drive on windows. This fixes the problem;
             * might not be needed on all platforms (NFS?), but should not
             * matter a lot: performance penalty of extra wrapping is more
             * relevant when accessing local file system.
             */
            String host = url.getHost();
            if (host == null || host.length() == 0) {
                return new FileInputStream(url.getPath());
            }
        }
        return url.openStream();
    }

    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (enc == JsonEncoding.UTF8) { // We have optimized writer for UTF-8
	    return new UTF8Writer(ctxt, out);
	}
	// not optimal, but should do unless we really care about UTF-16/32 encoding speed
	return new OutputStreamWriter(out, enc.getJavaName());
    }
}
