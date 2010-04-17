package org.codehaus.jackson.smile;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;

/**
 * Factory used for constructing {@link SmileParser} and {@link SmileGenerator}
 * instances.
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
    /*
    /******************************************************
    /* Configuration
    /******************************************************
     */

    /**
     * Whether non-supported methods (ones trying to output using
     * char-based targets like {@link java.io.Writer}, for example)
     * should be delegated to regular Jackson JSON processing
     * (if set to true); or throw {@link UnsupportedOperationException}
     * (if set to false)
     */
    protected boolean _cfgDelegateToTextual;
    
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
    public SmileFactory() { this(null); }

    public SmileFactory(ObjectCodec oc) { super(oc); }

    public void delegateToTextual(boolean state) {
        _cfgDelegateToTextual = state;
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
    protected JsonParser _createJsonParser(InputStream in, IOContext ctxt)
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
    protected JsonParser _createJsonParser(byte[] data, int offset, int len, IOContext ctxt)
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
        if (_cfgDelegateToTextual) {
            return super._createJsonGenerator(out, ctxt);
        }
        throw new UnsupportedOperationException("Can not create generator for non-byte-based target");
    }

    //public BufferRecycler _getBufferRecycler()

    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (_cfgDelegateToTextual) {
            return super._createWriter(out, enc, ctxt);
        }
        throw new UnsupportedOperationException("Can not create generator for non-byte-based target");
        /*
        if (enc == JsonEncoding.UTF8) { // We have optimized writer for UTF-8
            return new UTF8Writer(ctxt, out);
        }
        // not optimal, but should do unless we really care about UTF-16/32 encoding speed
        return new OutputStreamWriter(out, enc.getJavaName());
        */
    }    
}
