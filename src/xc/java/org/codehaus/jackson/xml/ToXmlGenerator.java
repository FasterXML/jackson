package org.codehaus.jackson.xml;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

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
import org.codehaus.jackson.xml.util.DefaultXmlPrettyPrinter;
import org.codehaus.jackson.xml.util.StaxUtil;

/**
 * {@link JsonGenerator} that outputs JAXB-style XML output instead of JSON content.
 * Operation requires calling code (usually either standard Jackson serializers,
 * or in some cases (like <code>BeanSerializer</code>) customised ones) to do
 * additional configuration calls beyond regular {@link JsonGenerator} API,
 * mostly to pass namespace information.
 * 
 * @since 1.6
 */
public final class ToXmlGenerator
    extends JsonGeneratorBase
{
    /**
     * If we support optional definition of element names, this is the element
     * name to use...
     */
    protected final static String DEFAULT_UNKNOWN_ELEMENT = "unknown";
    
    /**
     * Enumeration that defines all togglable extra XML-specific features
     */
    public enum Feature {
        /**
         * Feature that controls whether XML declaration should be written before
         * when generator is initialized (true) or not (false)
         */
        WRITE_XML_DECLARATION(false),

        /**
         * Feature that controls whether output should be done as XML 1.1; if so,
         * certain aspects may differ from default (1.0) processing: for example,
         * XML declaration will be automatically added (regardless of setting
         * <code>WRITE_XML_DECLARATION</code>) as this is required for reader to
         * know to use 1.1 compliant handling. XML 1.1 can be used to allow quoted
         * control characters (Ascii codes 0 through 31) as well as additional linefeeds
         * and name characters.
         */
        WRITE_XML_1_1(false)
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
    protected QName _nextName = null;

    /**
     * Marker flag that indicates whether next name to write
     * implies an attribute (true) or element (false)
     */
    protected boolean _nextIsAttribute = false;
    
    /**
     * To support proper serialization of arrays it is necessary to keep
     * stack of element names, so that we can "revert" to earlier 
     */
    protected LinkedList<QName> _elementNameStack = new LinkedList<QName>();
    
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

    /**
     * Method called before writing any other output, to optionally
     * output XML declaration.
     */
    public void initGenerator()  throws IOException, JsonGenerationException
    {
        try {
            if ((_xmlFeatures & Feature.WRITE_XML_1_1.getMask()) != 0) {
                _xmlWriter.writeStartDocument("UTF-8", "1.1");
            } else if ((_xmlFeatures & Feature.WRITE_XML_DECLARATION.getMask()) != 0) {
                _xmlWriter.writeStartDocument("UTF-8", "1.0");
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
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
    /* Extended API, access to some internal components
    /**********************************************************
     */

    /**
     * Method that allows application direct access to underlying
     * Stax {@link XMLStreamWriter}. Note that use of writer is
     * discouraged, and may interfere with processing of this writer;
     * however, occasionally it may be necessary.
     *<p>
     * Note: writer instance will always be of type
     * {@link org.codehaus.stax2.XMLStreamWriter2} (including
     * Typed Access API) so upcasts are safe.
     * 
     * @since 1.7
     */
    public XMLStreamWriter getStaxWriter() {
        return _xmlWriter;
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

    public final void setNextName(QName name)
    {
        _nextName = name;
    }

    /**
     * Methdod called when a structured (collection, array, map) is being
     * output.
     * 
     * @param wrapperName Element used as wrapper around elements, if any (null if none)
     * @param wrappedName Element used around individual content items (can not
     *   be null)
     */
    public void startWrappedValue(QName wrapperName, QName wrappedName) throws IOException, JsonGenerationException
    {
        if (wrapperName != null) {
            try {
                _xmlWriter.writeStartElement(wrapperName.getNamespaceURI(), wrapperName.getLocalPart());
            } catch (XMLStreamException e) {
                StaxUtil.throwXmlAsIOException(e);
            }
        }
        this.setNextName(wrappedName);
    }

    /**
     * Method called after a structured collection output has completed
     */
    public void finishWrappedValue(QName wrapperName, QName wrappedName) throws IOException, JsonGenerationException
    {
        // First: wrapper to close?
        if (wrapperName != null) {
            try {
                _xmlWriter.writeEndElement();
            } catch (XMLStreamException e) {
                StaxUtil.throwXmlAsIOException(e);
            }
        }
    }
    
    /*
    /**********************************************************
    /* JsonGenerator output method implementations, structural
    /**********************************************************
     */

    @Override
    public final void _writeStartArray() throws IOException, JsonGenerationException
    {
        // nothing to do here; no-operation
    }
    
    @Override
    public void _writeEndArray()
        throws IOException, JsonGenerationException
    {
        // nothing to do here; no-operation
    }

    @Override
    public void _writeStartObject()
        throws IOException, JsonGenerationException
    {
        if (_nextName == null) {
            handleMissingName();
        }
        // Need to keep track of names to make Lists work correctly
        _elementNameStack.addLast(_nextName);
        try {
            _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void _writeEndObject()
        throws IOException, JsonGenerationException
    {
        // We may want to repeat same element, so:
        _nextName = _elementNameStack.removeLast();
        try {
            // note: since attributes don't nest, can only have one attribute active, so:
            _nextIsAttribute = false;
            _xmlWriter.writeEndElement();
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void _writeFieldName(String name, boolean commaBefore)
        throws IOException, JsonGenerationException
    {
        // Should this ever get called?
        String ns = (_nextName == null) ? "" : _nextName.getNamespaceURI();
        setNextName(new QName(ns, name));
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeFieldName(SerializedString name)
        throws IOException, JsonGenerationException
    {
        writeFieldName(name.getValue());
    }

    @Override
    public void writeFieldName(SerializableString name)
        throws IOException, JsonGenerationException
    {
        writeFieldName(name.getValue());
    }
    
    @Override
    public void writeString(String text) throws IOException,JsonGenerationException
    {
        _verifyValueWrite("write String value");
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) { // must write attribute name and value with one call
                _xmlWriter.writeAttribute(_nextName.getNamespaceURI(), _nextName.getLocalPart(), text);
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeCharacters(text);
                _xmlWriter.writeEndElement();
            } 
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }    
    
    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write String value");
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) {
                _xmlWriter.writeAttribute(_nextName.getNamespaceURI(), _nextName.getLocalPart(), new String(text, offset, len));
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeCharacters(text, offset, len);
                _xmlWriter.writeEndElement();
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeString(SerializableString text) throws IOException, JsonGenerationException {
        writeString(text.getValue());
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
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) {
                // Stax2 API only has 'full buffer' write method:
                byte[] fullBuffer = toFullBuffer(data, offset, len);
                _xmlWriter.writeBinaryAttribute("", _nextName.getNamespaceURI(), _nextName.getLocalPart(), fullBuffer);
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeBinary(data, offset, len);
                _xmlWriter.writeEndElement();
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
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) {
                _xmlWriter.writeBooleanAttribute(null, _nextName.getNamespaceURI(), _nextName.getLocalPart(), state);
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeBoolean(state);
                _xmlWriter.writeEndElement();
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write null value");
        if (_nextName == null) {
            handleMissingName();
        }
        // !!! TODO: proper use of 'xsd:isNil'
        try {
            if (_nextIsAttribute) {
                /* With attributes, best just leave it out, right? (since there's no way
                 * to use 'xsi:nil')
                 */
            } else {
                _xmlWriter.writeEmptyElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(int i) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) {
                _xmlWriter.writeIntAttribute(null, _nextName.getNamespaceURI(), _nextName.getLocalPart(), i);
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeInt(i);
                _xmlWriter.writeEndElement();
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) {
                _xmlWriter.writeLongAttribute(null, _nextName.getNamespaceURI(), _nextName.getLocalPart(), l);
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeLong(l);
                _xmlWriter.writeEndElement();
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) {
                _xmlWriter.writeDoubleAttribute(null, _nextName.getNamespaceURI(), _nextName.getLocalPart(), d);
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeDouble(d);
                _xmlWriter.writeEndElement();
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) {
                _xmlWriter.writeFloatAttribute(null, _nextName.getNamespaceURI(), _nextName.getLocalPart(), f);
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeFloat(f);
                _xmlWriter.writeEndElement();
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
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) {
                _xmlWriter.writeDecimalAttribute(null, _nextName.getNamespaceURI(), _nextName.getLocalPart(), dec);
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeDecimal(dec);
                _xmlWriter.writeEndElement();
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
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) {
                _xmlWriter.writeIntegerAttribute(null, _nextName.getNamespaceURI(), _nextName.getLocalPart(), v);
            } else {
                _xmlWriter.writeStartElement(_nextName.getNamespaceURI(), _nextName.getLocalPart());
                _xmlWriter.writeInteger(v);
                _xmlWriter.writeEndElement();
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
    /* Implementations, overrides for other methods
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

    /**
     * Standard JSON indenter does not work well with XML, use
     * default XML indenter instead.
     */
    @Override
    public final JsonGenerator useDefaultPrettyPrinter()
    {
        return setPrettyPrinter(new DefaultXmlPrettyPrinter());
    }
    
    /*
    /**********************************************************
    /* Low-level output handling
    /**********************************************************
     */

    @Override
    public final void flush() throws IOException
    {
        if (isEnabled(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)) {
            try {
                _xmlWriter.flush();
            } catch (XMLStreamException e) {
                StaxUtil.throwXmlAsIOException(e);
            }
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
            try {
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
            } catch (ArrayIndexOutOfBoundsException e) {
                /* 29-Nov-2010, tatu: Stupid, stupid SJSXP doesn't do array checks, so we get
                 *   hit by this as a collateral problem in some cases. Yuck.
                 */
                throw new JsonGenerationException(e);
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

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected void handleMissingName()
    {
        throw new IllegalStateException("No element/attribute name specified when trying to output element");
    }
}
