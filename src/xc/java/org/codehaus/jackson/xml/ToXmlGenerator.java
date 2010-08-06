package org.codehaus.jackson.xml;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.ri.Stax2WriterAdapter;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.JsonGeneratorBase;
import org.codehaus.jackson.impl.JsonWriteContext;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.io.SerializedString;

/**
 * {@link JsonGenerator} that outputs JAXB-style XML output instead of JSON content.
 * Operation requires calling code (usually either standard Jackson serializers,
 * or in some cases (like <code>BeanSerializer</code>) customised ones) to do
 * additional configuration calls beyond regular {@link JsonGenerator} API,
 * mostly to pass namespace information.
 * 
 * @since 1.6
 */
public class ToXmlGenerator
    extends JsonGeneratorBase
{
    /**
     * Enumeration that defines all togglable extra XML-specific features
     */
    public enum Feature {
    	DUMMY(false)
        ;

        final boolean _defaultState;
        final int _mask;
        
        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
            _mask = (1 << ordinal());
        }
        
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    }
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected XMLStreamWriter2 _xmlWriter;
    
    final protected IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link org.codehaus.jackson.smile.SmileGenerator.Feature}s
     * are enabled.
     */
    protected int _xmlFeatures;

    /*
    /**********************************************************
    /* XML Output state
    /**********************************************************
     */

    /**
     * Element or attribute name to use for next output call.
     * Assigned by either code that initiates serialization
     * or bean serializer.
     */
    protected String _nextName = "unknown";
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public ToXmlGenerator(IOContext ctxt, int genericGeneratorFeatures, int xmlFeatures,
            ObjectCodec codec, XMLStreamWriter sw)
    {
        super(genericGeneratorFeatures, codec);
        _xmlFeatures = xmlFeatures;
        _ioContext = ctxt;
        _xmlWriter = Stax2WriterAdapter.wrapIfNecessary(sw);
    }

    /*
    /**********************************************************
    /* Overridden implementations
    /**********************************************************
     */
    
    @Override
    public void writeFieldName(SerializedString name)
        throws IOException, JsonGenerationException
    {
        writeFieldName(name.getValue());
    }
    
    /*
    /**********************************************************
    /* Extended API, configuration
    /**********************************************************
     */

    public ToXmlGenerator enable(Feature f) {
        _xmlFeatures |= f.getMask();
        return this;
    }

    public ToXmlGenerator disable(Feature f) {
        _xmlFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_xmlFeatures & f.getMask()) != 0;
    }

    public ToXmlGenerator configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /*
    /**********************************************************
    /* Extended API, passing XML specific settings
    /**********************************************************
     */
    
    /*
    /**********************************************************
    /* JsonGenerator output method implementations, structural
    /**********************************************************
     */

    @Override
    protected final void _writeStartArray() throws IOException, JsonGenerationException
    {
        // !!! TODO: cases where there is no wrapper
        try {
            _xmlWriter.writeStartElement(_nextName);
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }
    
    @Override
    protected void _writeEndArray()
        throws IOException, JsonGenerationException
    {
        // !!! TODO: cases where there is no wrapper
        try {
            _xmlWriter.writeEndElement();
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    protected void _writeStartObject()
        throws IOException, JsonGenerationException
    {
        try {
            _xmlWriter.writeStartElement(_nextName);
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    protected void _writeEndObject()
        throws IOException, JsonGenerationException
    {
        try {
            _xmlWriter.writeEndElement();
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    protected void _writeFieldName(String name, boolean commaBefore) throws IOException, JsonGenerationException
    {
        _nextName = name;
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException
    {
        _verifyValueWrite("write String value");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeCharacters(text);
                _xmlWriter.writeEndElement();
            } else {
                _xmlWriter.writeCharacters(text);                
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }    
    
    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write String value");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeCharacters(text, offset, len);
                _xmlWriter.writeEndElement();
            } else {
                _xmlWriter.writeCharacters(text, offset, len);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }
    
    /*
    /**********************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException, JsonGenerationException
    {
        try {
            _xmlWriter.writeRaw(text);
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException, JsonGenerationException
    {
        try {
            _xmlWriter.writeRaw(text, offset, len);
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException
    {
        try {
            _xmlWriter.writeRaw(text, offset, len);
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException
    {
        writeRaw(String.valueOf(c));
    }
    
    /*
    /**********************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException
    {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeBinary(data, offset, len);
                _xmlWriter.writeEndElement();
            } else {
                _xmlWriter.writeBinary(data, offset, len);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }
    
    /*
    /**********************************************************
    /* Output method implementations, primitive
    /**********************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write boolean value");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeBoolean(state);
                _xmlWriter.writeEndElement();
            } else {
                
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write null value");
        // !!! TODO: proper use of 'xsd:isNil'
        try {
            if (_nextName != null) {
                _xmlWriter.writeEmptyElement(_nextName);
            } else {
                // !!! TBI
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(int i) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeInt(i);
                _xmlWriter.writeEndElement();
            } else {
                _xmlWriter.writeInt(i);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeLong(l);
                _xmlWriter.writeEndElement();
            } else {
                _xmlWriter.writeLong(l);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeDouble(d);
                _xmlWriter.writeEndElement();
            } else {
                _xmlWriter.writeDouble(d);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeFloat(f);
                _xmlWriter.writeEndElement();
            } else {
                _xmlWriter.writeFloat(f);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException, JsonGenerationException
    {
        if (dec == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeDecimal(dec);
                _xmlWriter.writeEndElement();
            } else {
                _xmlWriter.writeDecimal(dec);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        try {
            if (_nextName != null) {
                _xmlWriter.writeStartElement(_nextName);
                _xmlWriter.writeInteger(v);
                _xmlWriter.writeEndElement();
            } else {
                _xmlWriter.writeInteger(v);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException,JsonGenerationException, UnsupportedOperationException
    {
        writeString(encodedValue);
    }

    /*
    /**********************************************************
    /* Implementations for other methods
    /**********************************************************
     */
    
    @Override
    protected final void _verifyValueWrite(String typeMsg)
        throws IOException, JsonGenerationException
    {
        int status = _writeContext.writeValue();
        if (status == JsonWriteContext.STATUS_EXPECT_NAME) {
            _reportError("Can not "+typeMsg+", expecting field name");
        }
    }

    /*
    /**********************************************************
    /* Low-level output handling
    /**********************************************************
     */

    @Override
    public final void flush() throws IOException
    {
        try {
            _xmlWriter.flush();
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void close()
        throws IOException
    {
//        boolean wasClosed = _closed;
        super.close();

        /* 05-Dec-2008, tatu: To add [JACKSON-27], need to close open
         *   scopes.
         */
        // First: let's see that we still have buffers...
        if (isEnabled(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)) {
            while (true) {
                JsonStreamContext ctxt = getOutputContext();
                if (ctxt.inArray()) {
                    writeEndArray();
                } else if (ctxt.inObject()) {
                    writeEndObject();
                } else {
                    break;
                }
            }
        }
        try {
            if (_ioContext.isResourceManaged() || isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET)) {
                _xmlWriter.closeCompletely();
            } else {
                _xmlWriter.close();
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    protected void _releaseBuffers() {
        // Nothing to do here, as we have no buffers
    }
}
