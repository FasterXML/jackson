package org.codehaus.jackson.smile;

import java.io.*;
import java.net.URL;

import org.codehaus.jackson.*;
import org.codehaus.jackson.format.InputAccessor;
import org.codehaus.jackson.format.MatchStrength;
import org.codehaus.jackson.io.IOContext;

/**
 * Factory used for constructing {@link SmileParser} and {@link SmileGenerator}
 * instances; both of which handle
 * <a href="http://wiki.fasterxml.com/SmileFormat">Smile</a> encoded data.
 *<p>
 * Extends {@link JsonFactory} mostly so that users can actually use it in place
 * of regular non-Smile factory instances.
 *<p>
 * Note on using non-byte-based sources/targets (char based, like
 * {@link java.io.Reader} and {@link java.io.Writer}): these can not be
 * used for Smile-format documents, and thus will either downgrade to
 * textual JSON (when parsing), or throw exception (when trying to create
 * generator).
 * 
 * @author tatu
 * 
 * @since 1.6
 */
public class SmileFactory extends JsonFactory
{
    /**
     * Name used to identify Smile format.
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_SMILE = "Smile";
    
    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_SMILE_PARSER_FEATURE_FLAGS = SmileParser.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_SMILE_GENERATOR_FEATURE_FLAGS = SmileGenerator.Feature.collectDefaults();

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Whether non-supported methods (ones trying to output using
     * char-based targets like {@link java.io.Writer}, for example)
     * should be delegated to regular Jackson JSON processing
     * (if set to true); or throw {@link UnsupportedOperationException}
     * (if set to false)
     */
    protected boolean _cfgDelegateToTextual;

    protected int _smileParserFeatures = DEFAULT_SMILE_PARSER_FEATURE_FLAGS;

    protected int _smileGeneratorFeatures = DEFAULT_SMILE_GENERATOR_FEATURE_FLAGS;

    /*
    /**********************************************************
    /* Factory construction, configuration
    /**********************************************************
     */

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
    public SmileFactory() { this(null); }

    public SmileFactory(ObjectCodec oc) { super(oc); }

    public void delegateToTextual(boolean state) {
        _cfgDelegateToTextual = state;
    }

    /*
    /**********************************************************
    /* Format detection functionality (since 1.8)
    /**********************************************************
     */
    
    @Override
    public String getFormatName()
    {
        return FORMAT_NAME_SMILE;
    }
    
    /**
     * Sub-classes need to override this method (as of 1.8)
     */
    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        return SmileParserBootstrapper.hasSmileFormat(acc);
    }
    
    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link SmileParser.Feature} for list of features)
     */
    public final SmileFactory configure(SmileParser.Feature f, boolean state)
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
     * (check {@link SmileParser.Feature} for list of features)
     */
    public SmileFactory enable(SmileParser.Feature f) {
        _smileParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link SmileParser.Feature} for list of features)
     */
    public SmileFactory disable(SmileParser.Feature f) {
        _smileParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(SmileParser.Feature f) {
        return (_smileParserFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Configuration, generator settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link org.codehaus.jackson.JsonGenerator.Feature} for list of features)
     *
     * @since 1.2
     */
    public final SmileFactory configure(SmileGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }


    /**
     * Method for enabling specified generator features
     * (check {@link org.codehaus.jackson.JsonGenerator.Feature} for list of features)
     */
    public SmileFactory enable(SmileGenerator.Feature f) {
        _smileGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link org.codehaus.jackson.JsonGenerator.Feature} for list of features)
     */
    public SmileFactory disable(SmileGenerator.Feature f) {
        _smileGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(SmileGenerator.Feature f) {
        return (_smileGeneratorFeatures & f.getMask()) != 0;
    }
    
    /*
    /**********************************************************
    /* Overridden parser factory methods
    /**********************************************************
     */

    @Override
    public SmileParser createJsonParser(File f)
        throws IOException, JsonParseException
    {
        return _createJsonParser(new FileInputStream(f), _createContext(f, true));
    }

    @Override
    public SmileParser createJsonParser(URL url)
        throws IOException, JsonParseException
    {
        return _createJsonParser(_optimizedStreamFromURL(url), _createContext(url, true));
    }

    @Override
    public SmileParser createJsonParser(InputStream in)
        throws IOException, JsonParseException
    {
        return _createJsonParser(in, _createContext(in, false));
    }

    //public JsonParser createJsonParser(Reader r)
    
    @Override
    public SmileParser createJsonParser(byte[] data)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(data, true);
        return _createJsonParser(data, 0, data.length, ctxt);
    }
    
    @Override
    public SmileParser createJsonParser(byte[] data, int offset, int len)
        throws IOException, JsonParseException
    {
        return _createJsonParser(data, offset, len, _createContext(data, true));
    }

    /*
    /**********************************************************
    /* Overridden generator factory methods
    /**********************************************************
     */
    
    /**
     *<p>
     * note: co-variant return type
     */
    @Override
    public SmileGenerator createJsonGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        return createJsonGenerator(out);
    }

    /**
     * Since Smile format always uses UTF-8 internally, no encoding need
     * to be passed to this method.
     */
    @Override
    public SmileGenerator createJsonGenerator(OutputStream out) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        return _createJsonGenerator(out, ctxt);
    }
    
    /*
    /******************************************************
    /* Overridden internal factory methods
    /******************************************************
     */

    //protected IOContext _createContext(Object srcRef, boolean resourceManaged)

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected SmileParser _createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return new SmileParserBootstrapper(ctxt, in).constructParser(_parserFeatures,
        		_smileParserFeatures, _objectCodec, _rootByteSymbols);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected JsonParser _createJsonParser(Reader r, IOContext ctxt)
        throws IOException, JsonParseException
    {
        if (_cfgDelegateToTextual) {
            return super._createJsonParser(r, ctxt);
        }
        throw new UnsupportedOperationException("Can not create generator for non-byte-based target");
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected SmileParser _createJsonParser(byte[] data, int offset, int len, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return new SmileParserBootstrapper(ctxt, data, offset, len).constructParser(_parserFeatures,
        		_smileParserFeatures, _objectCodec, _rootByteSymbols);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * generator.
     */
    @Override
    protected JsonGenerator _createJsonGenerator(Writer out, IOContext ctxt)
        throws IOException
    {
        if (_cfgDelegateToTextual) {
            return super._createJsonGenerator(out, ctxt);
        }
        throw new UnsupportedOperationException("Can not create generator for non-byte-based target");
    }

    //public BufferRecycler _getBufferRecycler()

    @Override
    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (_cfgDelegateToTextual) {
            return super._createWriter(out, enc, ctxt);
        }
        throw new UnsupportedOperationException("Can not create generator for non-byte-based target");
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    protected SmileGenerator _createJsonGenerator(OutputStream out, IOContext ctxt)
        throws IOException
    {
        int feats = _smileGeneratorFeatures;
        /* One sanity check: MUST write header if shared string values setting is enabled,
         * or quoting of binary data disabled.
         * But should we force writing, or throw exception, if settings are in conflict?
         * For now, let's error out...
         */
        SmileGenerator gen = new SmileGenerator(ctxt, _generatorFeatures, feats, _objectCodec, out);
        if ((feats & SmileGenerator.Feature.WRITE_HEADER.getMask()) != 0) {
            gen.writeHeader();
        } else {
            if ((feats & SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES.getMask()) != 0) {
                throw new JsonGenerationException(
                        "Inconsistent settings: WRITE_HEADER disabled, but CHECK_SHARED_STRING_VALUES enabled; can not construct generator"
                        +" due to possible data loss (either enable WRITE_HEADER, or disable CHECK_SHARED_STRING_VALUES to resolve)");
            }
            if ((feats & SmileGenerator.Feature.ENCODE_BINARY_AS_7BIT.getMask()) == 0) {
        	throw new JsonGenerationException(
        			"Inconsistent settings: WRITE_HEADER disabled, but ENCODE_BINARY_AS_7BIT disabled; can not construct generator"
        			+" due to possible data loss (either enable WRITE_HEADER, or ENCODE_BINARY_AS_7BIT to resolve)");
            }
        }
        return gen;
    }
}
