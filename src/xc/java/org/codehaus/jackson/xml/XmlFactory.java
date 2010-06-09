package org.codehaus.jackson.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.io.IOContext;

/**
* Factory used for constructing {@link FromXmlParser} and {@link ToXmlGenerator}
* instances.
*<p>
* Implements {@link JsonFactory} since interface for constructing XML backed
* parsers and generators is quite similar to dealing with JSON.
* 
* @author tatu
* 
* @since 1.6
*/
public class XmlFactory extends JsonFactory
{
    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_XML_PARSER_FEATURE_FLAGS = FromXmlParser.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_XML_GENERATOR_FEATURE_FLAGS = ToXmlGenerator.Feature.collectDefaults();

    /*
    /******************************************************
    /* Configuration
    /******************************************************
     */

    protected int _xmlParserFeatures = DEFAULT_XML_PARSER_FEATURE_FLAGS;

    protected int _xmlGeneratorFeatures = DEFAULT_XML_GENERATOR_FEATURE_FLAGS;
    
    /*
    /******************************************************
    /* Factory construction, configuration
    /******************************************************
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
    public XmlFactory() { this(null); }

    public XmlFactory(ObjectCodec oc) { super(oc); }

    /*
    /******************************************************
    /* Configuration, parser settings
    /******************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link XmlParser.Feature} for list of features)
     */
    public final XmlFactory configure(FromXmlParser.Feature f, boolean state)
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
     * (check {@link FromXmlParser.Feature} for list of features)
     */
    public XmlFactory enable(FromXmlParser.Feature f) {
        _xmlParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link JsonParser.Feature} for list of features)
     */
    public XmlFactory disable(FromXmlParser.Feature f) {
        _xmlParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(FromXmlParser.Feature f) {
        return (_xmlParserFeatures & f.getMask()) != 0;
    }

    /*
    /******************************************************
    /* Configuration, generator settings
    /******************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link JsonGenerator.Feature} for list of features)
     *
     * @since 1.2
     */
    public final XmlFactory configure(ToXmlGenerator.Feature f, boolean state) {
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
     */
    public XmlFactory enable(ToXmlGenerator.Feature f) {
        _xmlGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link JsonGenerator.Feature} for list of features)
     */
    public XmlFactory disable(ToXmlGenerator.Feature f) {
        _xmlGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(ToXmlGenerator.Feature f) {
        return (_xmlGeneratorFeatures & f.getMask()) != 0;
    }
    
    /*
    /******************************************************
    /* Overridden parts of public API
    /******************************************************
     */

    /**
     *<p>
     * note: co-variant return type
     */
    public ToXmlGenerator createJsonGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        return createJsonGenerator(out);
    }
    
    /*
    /******************************************************
    /* Extended public API
    /******************************************************
     */

    /**
     * Since Smile format always uses UTF-8 internally, no encoding need
     * to be passed to this method.
     */
    public ToXmlGenerator createJsonGenerator(OutputStream out)
        throws IOException
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
    protected FromXmlParser _createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        // !!! TBI
        return null;
        //return new ByteSourceBootstrapper(ctxt, in).constructParser(_parserFeatures, _objectCodec, _rootByteSymbols, _rootCharSymbols);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    protected FromXmlParser _createJsonParser(Reader r, IOContext ctxt)
        throws IOException, JsonParseException
    {
        // !!! TBI
        return null;
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    protected FromXmlParser _createJsonParser(byte[] data, int offset, int len, IOContext ctxt)
        throws IOException, JsonParseException
    {
        // !!! TBI
        return null;
        // true -> managed (doesn't really matter; we have no stream!)
        //return new ByteSourceBootstrapper(ctxt, data, offset, len).constructParser(_parserFeatures, _objectCodec, _rootByteSymbols, _rootCharSymbols);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * generator.
     */
    protected JsonGenerator _createJsonGenerator(Writer out, IOContext ctxt)
        throws IOException
    {
            // !!! TBI
            return null;
    }

    //public BufferRecycler _getBufferRecycler()

    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        // !!! TBI
        return null;
    }    

    /*
    /******************************************************
    /* Internal methods
    /******************************************************
     */
    
    protected ToXmlGenerator _createJsonGenerator(OutputStream out, IOContext ctxt)
        throws IOException
    {
        int feats = _xmlGeneratorFeatures;
        ToXmlGenerator gen = new ToXmlGenerator(ctxt, feats, _objectCodec);
        //if ((feats & ToXmlGenerator.Feature.WRITE_HEADER.getMask()) != 0)
        return gen;
    }

}
