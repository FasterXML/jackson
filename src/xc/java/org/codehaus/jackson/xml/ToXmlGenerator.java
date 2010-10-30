package org.codehaus.jackson.xml;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.ri.Stax2WriterAdapter;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.JsonGeneratorBase;
import org.codehaus.jackson.impl.JsonWriteContext;
import org.codehaus.jackson.io.IOContext;

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
    protected String _nextLocalName = "unknown";

    /**
     * Namespace URI of the next element or attribute to be output.
     * Assigned by either code that initiates serialization
     * or bean serializer.
     */
//    protected String _nextNamespace = "http://fasterxml.com/bogus";
    protected String _nextNamespace = "";

    /**
     * Marker flag that indicates whether next name to write
     * implies an attribute (true) or element (false)
     */
    protected boolean _nextIsAttribute = false;
    
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

    public void setNextIsAttribute(boolean isAttribute)
    {
        _nextIsAttribute = isAttribute;
    }
    
    public void setNextElementName(String namespace, String localName)
    {
        _nextNamespace = (namespace == null) ? "" : namespace;
        _nextLocalName = localName;
        _nextIsAttribute = false;
    }
    
    public void setNextAttributeName(String namespace, String localName)
    {
        _nextNamespace = (namespace == null) ? "" : namespace;
        _nextLocalName = localName;
        _nextIsAttribute = true;
    }
    
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
            _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
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
            if (!_nextIsAttribute) { // attributes must be written along with value...
                _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    protected void _writeEndObject()
        throws IOException, JsonGenerationException
    {
        try {
            // note: since attributes don't nest, can only have one attribute active, so:
            if (_nextIsAttribute) {
                _nextIsAttribute = false;
            } else {
                _xmlWriter.writeEndElement();
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    protected void _writeFieldName(String name, boolean commaBefore)
        throws IOException, JsonGenerationException
    {
        _nextLocalName = name;
    }

    @Override
    protected void _writeFieldName(SerializableString name, boolean commaBefore)
        throws IOException, JsonGenerationException
    {
        // !!! TODO: improve
        _writeFieldName(name.getValue(), commaBefore);
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) { // must write attribute name and value with one call
                    _xmlWriter.writeAttribute(_nextNamespace, _nextLocalName, text);
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeCharacters(text);
                    _xmlWriter.writeEndElement();
                } 
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    _xmlWriter.writeAttribute(_nextNamespace, _nextLocalName, new String(text, offset, len));
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeCharacters(text, offset, len);
                    _xmlWriter.writeEndElement();
                }
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    // Stax2 API only has 'full buffer' write method:
                    byte[] fullBuffer = toFullBuffer(data, offset, len);
                    _xmlWriter.writeBinaryAttribute("", _nextNamespace, _nextLocalName, fullBuffer);
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeBinary(data, offset, len);
                    _xmlWriter.writeEndElement();
                }
            } else {
                _xmlWriter.writeBinary(data, offset, len);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    private byte[] toFullBuffer(byte[] data, int offset, int len)
    {
        // might already be ok:
        if (offset == 0 && len == data.length) {
            return data;
        }
        byte[] result = new byte[len];
        if (len > 0) {
            System.arraycopy(data, offset, result, 0, len);
        }
        return result;
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    _xmlWriter.writeBooleanAttribute(null, _nextNamespace, _nextLocalName, state);
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeBoolean(state);
                    _xmlWriter.writeEndElement();
                }
            } else {
                // !!! TBI
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    /* With attributes, best just leave it out, right? (since there's no way
                     * to use 'xsi:nil')
                     */
                } else {
                    _xmlWriter.writeEmptyElement(_nextNamespace, _nextLocalName);
                }
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    _xmlWriter.writeIntAttribute(null, _nextNamespace, _nextLocalName, i);
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeInt(i);
                    _xmlWriter.writeEndElement();
                }
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    _xmlWriter.writeLongAttribute(null, _nextNamespace, _nextLocalName, l);
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeLong(l);
                    _xmlWriter.writeEndElement();
                }
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    _xmlWriter.writeDoubleAttribute(null, _nextNamespace, _nextLocalName, d);
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeDouble(d);
                    _xmlWriter.writeEndElement();
                }
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    _xmlWriter.writeFloatAttribute(null, _nextNamespace, _nextLocalName, f);
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeFloat(f);
                    _xmlWriter.writeEndElement();
                }
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    _xmlWriter.writeDecimalAttribute(null, _nextNamespace, _nextLocalName, dec);
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeDecimal(dec);
                    _xmlWriter.writeEndElement();
                }
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
            if (_nextLocalName != null) {
                if (_nextIsAttribute) {
                    _xmlWriter.writeIntegerAttribute(null, _nextNamespace, _nextLocalName, v);
                } else {
                    _xmlWriter.writeStartElement(_nextNamespace, _nextLocalName);
                    _xmlWriter.writeInteger(v);
                    _xmlWriter.writeEndElement();
                }
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
