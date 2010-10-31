package org.codehaus.jackson.xml;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.io.Stax2ByteArraySource;

import org.codehaus.jackson.*;
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
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected int _xmlParserFeatures = DEFAULT_XML_PARSER_FEATURE_FLAGS;

    protected int _xmlGeneratorFeatures = DEFAULT_XML_GENERATOR_FEATURE_FLAGS;

    protected XMLInputFactory _xmlInputFactory;

    protected XMLOutputFactory _xmlOutputFactory;

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
    public XmlFactory() { this(null); }

    public XmlFactory(ObjectCodec oc) {
        this(oc, null, null);
    }

    public XmlFactory(ObjectCodec oc,
            XMLInputFactory xmlIn, XMLOutputFactory xmlOut)
    {
        super(oc);
        if (xmlIn == null) {
            /* 24-Jun-2010, tatu: Ugh. JDK authors seem to waffle on what the name of
             *   factory constructor method is...
             * 
             */
            //xmlIn = XMLInputFactory.newFactory();
            xmlIn = XMLInputFactory.newInstance();
        }
        if (xmlOut == null) {
            //xmlOut = XMLOutputFactory.newFactory();
            xmlOut = XMLOutputFactory.newInstance();
        }
        // Better ensure namespaces get built properly, so:
        xmlOut.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        // and for parser, force coalescing as well (much simpler to use)
        xmlIn.setProperty(XMLInputFactory.IS_COALESCING, true);
        _xmlInputFactory = xmlIn;
        _xmlOutputFactory = xmlOut;
    }

    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link FromXmlParser.Feature} for list of features)
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
    /**********************************************************
    /* Additional configuration
    /**********************************************************
     */

    public void setXMLInputFactory(XMLInputFactory f) {
        _xmlInputFactory = f;
    }

    public void setXMLOutputFactory(XMLOutputFactory f) {
        _xmlOutputFactory = f;
    }
    
    /*
    /**********************************************************
    /* Overridden parts of public API
    /**********************************************************
     */

    /**
     *<p>
     * note: co-variant return type
     */
    @Override
    public ToXmlGenerator createJsonGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        return new ToXmlGenerator(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(out));
    }

    @Override
    public ToXmlGenerator createJsonGenerator(Writer out)
        throws IOException
    {
        IOContext ctxt = _createContext(out, false);
        return new ToXmlGenerator(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(out));
    }

    @Override
    public ToXmlGenerator createJsonGenerator(File f, JsonEncoding enc)
        throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ctxt = _createContext(out, true);
        ctxt.setEncoding(enc);
        return new ToXmlGenerator(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(out));
    }
    
    /*
    /**********************************************************
    /* Overridden internal factory methods
    /**********************************************************
     */

    //protected IOContext _createContext(Object srcRef, boolean resourceManaged)

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected FromXmlParser _createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        XMLStreamReader sr;
        try {
            sr = _xmlInputFactory.createXMLStreamReader(in);
            sr = _initializeXmlReader(sr);
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
        return new FromXmlParser(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, sr);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected FromXmlParser _createJsonParser(Reader r, IOContext ctxt)
        throws IOException, JsonParseException
    {
        XMLStreamReader sr;
        try {
            sr = _xmlInputFactory.createXMLStreamReader(r);
            sr = _initializeXmlReader(sr);
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
        return new FromXmlParser(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, sr);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected FromXmlParser _createJsonParser(byte[] data, int offset, int len, IOContext ctxt)
        throws IOException, JsonParseException
    {
        XMLStreamReader sr;
        try {
            sr = _xmlInputFactory.createXMLStreamReader(new Stax2ByteArraySource(data, offset, len));
            sr = _initializeXmlReader(sr);
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
        return new FromXmlParser(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, sr);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected XMLStreamWriter _createXmlWriter(OutputStream out) throws IOException
    {
        try {
            return _initializeXmlWriter(_xmlOutputFactory.createXMLStreamWriter(out, "UTF-8"));
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
    }

    protected XMLStreamWriter _createXmlWriter(Writer w) throws IOException
    {
        try {
            return _initializeXmlWriter(_xmlOutputFactory.createXMLStreamWriter(w));
        } catch (XMLStreamException e) {
            return StaxUtil.throwXmlAsIOException(e);
        }
    }

    protected final XMLStreamWriter _initializeXmlWriter(XMLStreamWriter sw) throws IOException, XMLStreamException
    {
        // And just for Sun Stax parser (JDK default), seems that we better define default namespace
        // (Woodstox doesn't care) -- otherwise it'll add unnecessary odd declaration
        sw.setDefaultNamespace("");
        return sw;
    }

    protected final XMLStreamReader _initializeXmlReader(XMLStreamReader sr) throws IOException, XMLStreamException
    {
        // for now, nothing to do... except let's find the root element
        while (sr.next() != XMLStreamConstants.START_ELEMENT) {
            ;
        }
        return sr;
    }

}
