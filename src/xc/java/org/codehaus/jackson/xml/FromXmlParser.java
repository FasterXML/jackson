package org.codehaus.jackson.xml;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLStreamLocation2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.ri.Stax2ReaderAdapter;

import org.codehaus.jackson.Base64Variant;
import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.impl.JsonParserMinimalBase;
import org.codehaus.jackson.impl.JsonReadContext;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.util.ByteArrayBuilder;

/**
 * {@link JsonParser} implementation that exposes XML structure as
 * set of JSON events that can be used for data binding.
 * 
 * @since 1.6
 */
public class FromXmlParser
    extends JsonParserMinimalBase
{
    /**
     * Enumeration that defines all togglable features for Smile generators.
     */
    public enum Feature {
        DUMMY_PLACEHOLDER(false)
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

    final protected XMLStreamReader2 _xmlReader;

    /**
     * Bit flag composed of bits that indicate which
     * {@link org.codehaus.jackson.smile.SmileGenerator.Feature}s
     * are enabled.
     */
    protected int _xmlFeatures;
    
    protected ObjectCodec _objectCodec;

    /*
    /**********************************************************
    /* I/O and Parsing state
    /**********************************************************
     */

    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;
    
    final protected IOContext _ioContext;
    
    /**
     * Information about parser context, context in which
     * the next token is to be parsed (root, array, object).
     */
    protected JsonReadContext _parsingContext;

    /**
     * ByteArrayBuilder is needed if 'getBinaryValue' is called. If so,
     * we better reuse it for remainder of content.
     */
    protected ByteArrayBuilder _byteArrayBuilder = null;

    /**
     * We will hold on to decoded binary data, for duration of
     * current event, so that multiple calls to
     * {@link #getBinaryValue} will not need to decode data more
     * than once.
     */
    protected byte[] _binaryValue;
    
    /*
    /**********************************************************
    /* Additional XML state
    /**********************************************************
     */

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public FromXmlParser(IOContext ctxt, int genericParserFeatures, int xmlFeatures,
            ObjectCodec codec, XMLStreamReader xmlReader)
    {
        super(genericParserFeatures);
        _xmlFeatures = xmlFeatures;
        _ioContext = ctxt;
        _objectCodec = codec;
        _xmlReader = Stax2ReaderAdapter.wrapIfNecessary(xmlReader);
    }

    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }
    
    /*
    /**********************************************************
    /* JsonParser impl
    /**********************************************************
     */
    
    /**
     * Method that can be called to get the name associated with
     * the current event.
     */
    @Override
    public String getCurrentName()
        throws IOException, JsonParseException
    {
        // [JACKSON-395]: start markers require information from parent
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            JsonReadContext parent = _parsingContext.getParent();
            return parent.getCurrentName();
        }
        return _parsingContext.getCurrentName();
    }

    @Override
    public void close() throws IOException
    {
        if (!_closed) {
            _closed = true;
            try {
                if (_ioContext.isResourceManaged() || isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE)) {
                    _xmlReader.closeCompletely();
                } else {
                    _xmlReader.close();
                }
            } catch (XMLStreamException e) {
                StaxUtil.throwXmlAsIOException(e);
            } finally {
                // as per [JACKSON-324], do in finally block
                // Also, internal buffer(s) can now be released as well
                _releaseBuffers();
            }
        }
    }

    @Override
    public boolean isClosed() { return _closed; }

    @Override
    public JsonReadContext getParsingContext()
    {
        return _parsingContext;
    }

    /**
     * Method that return the <b>starting</b> location of the current
     * token; that is, position of the first character from input
     * that starts the current token.
     */
    @Override
    public JsonLocation getTokenLocation()
    {
        return _extractLocation(_xmlReader.getLocationInfo().getStartLocation());
    }

    /**
     * Method that returns location of the last processed character;
     * usually for error reporting purposes
     */
    @Override
    public JsonLocation getCurrentLocation()
    {
        return _extractLocation(_xmlReader.getLocationInfo().getCurrentLocation());
    }
    
    @Override
    public JsonToken nextToken() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
    /**********************************************************
    /* Public API, access to token information, text
    /**********************************************************
     */

    @Override
    public String getText() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public char[] getTextCharacters() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getTextLength() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getTextOffset() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * XML input actually would offer access to character arrays; but since
     * we must coalesce things it cannot really be exposed.
     */
    @Override
    public boolean hasTextCharacters()
    {
        return false;
    }

    /*
    /**********************************************************
    /* Public API, access to token information, binary
    /**********************************************************
     */
    
    @Override
    public byte[] getBinaryValue(Base64Variant b64variant)
        throws IOException, JsonParseException
    {
        if (_currToken != JsonToken.VALUE_STRING &&
                (_currToken != JsonToken.VALUE_EMBEDDED_OBJECT || _binaryValue == null)) {
            _reportError("Current token ("+_currToken+") not VALUE_STRING or VALUE_EMBEDDED_OBJECT, can not access as binary");
        }
        /* To ensure that we won't see inconsistent data, better clear up
         * state...
         */
        if (_binaryValue == null) {
            try {
                _binaryValue = _decodeBase64(b64variant);
            } catch (IllegalArgumentException iae) {
                throw _constructError("Failed to decode VALUE_STRING as base64 ("+b64variant+"): "+iae.getMessage());
            }
        }        
        return _binaryValue;
    }
    
    protected byte[] _decodeBase64(Base64Variant b64variant)
        throws IOException, JsonParseException
    {
        ByteArrayBuilder builder = _getByteArrayBuilder();
    
        final String str = getText();
        int ptr = 0;
        int len = str.length();

        main_loop:
        while (ptr < len) {
            // first, we'll skip preceding white space, if any
            char ch;
            do {
                ch = str.charAt(ptr++);
                if (ptr >= len) {
                    break main_loop;
                }
            } while (ch <= INT_SPACE);
            int bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                _reportInvalidBase64(b64variant, ch, 0, null);
            }
            int decodedData = bits;
            // then second base64 char; can't get padding yet, nor ws
            if (ptr >= len) {
                _reportBase64EOF();
            }
            ch = str.charAt(ptr++);
            bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                _reportInvalidBase64(b64variant, ch, 1, null);
            }
            decodedData = (decodedData << 6) | bits;
            // third base64 char; can be padding, but not ws
            if (ptr >= len) {
                _reportBase64EOF();
            }
            ch = str.charAt(ptr++);
            bits = b64variant.decodeBase64Char(ch);

            // First branch: can get padding (-> 1 byte)
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    _reportInvalidBase64(b64variant, ch, 2, null);
                }
                // Ok, must get padding
                if (ptr >= len) {
                    _reportBase64EOF();
                }
                ch = str.charAt(ptr++);
                if (!b64variant.usesPaddingChar(ch)) {
                    _reportInvalidBase64(b64variant, ch, 3, "expected padding character '"+b64variant.getPaddingChar()+"'");
                }
                // Got 12 bits, only need 8, need to shift
                decodedData >>= 4;
                builder.append(decodedData);
                continue;
            }
            // Nope, 2 or 3 bytes
            decodedData = (decodedData << 6) | bits;
            // fourth and last base64 char; can be padding, but not ws
            if (ptr >= len) {
                _reportBase64EOF();
            }
            ch = str.charAt(ptr++);
            bits = b64variant.decodeBase64Char(ch);
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    _reportInvalidBase64(b64variant, ch, 3, null);
                }
                decodedData >>= 2;
                builder.appendTwoBytes(decodedData);
            } else {
                // otherwise, our triple is now complete
                decodedData = (decodedData << 6) | bits;
                builder.appendThreeBytes(decodedData);
            }
        }
        return builder.toByteArray();
    }

    /**
     * @param bindex Relative index within base64 character unit; between 0
     *   and 3 (as unit has exactly 4 characters)
     */
    protected void _reportInvalidBase64(Base64Variant b64variant, char ch, int bindex, String msg)
        throws JsonParseException
    {
        String base;
        if (ch <= INT_SPACE) {
            base = "Illegal white space character (code 0x"+Integer.toHexString(ch)+") as character #"+(bindex+1)+" of 4-char base64 unit: can only used between units";
        } else if (b64variant.usesPaddingChar(ch)) {
            base = "Unexpected padding character ('"+b64variant.getPaddingChar()+"') as character #"+(bindex+1)+" of 4-char base64 unit: padding only legal as 3rd or 4th character";
        } else if (!Character.isDefined(ch) || Character.isISOControl(ch)) {
            // Not sure if we can really get here... ? (most illegal xml chars are caught at lower level)
            base = "Illegal character (code 0x"+Integer.toHexString(ch)+") in base64 content";
        } else {
            base = "Illegal character '"+ch+"' (code 0x"+Integer.toHexString(ch)+") in base64 content";
        }
        if (msg != null) {
            base = base + ": " + msg;
        }
        throw new JsonParseException(base, JsonLocation.NA);
    }

    protected void _reportBase64EOF()
        throws JsonParseException
    {
        throw new JsonParseException("Unexpected end-of-String when base64 content", JsonLocation.NA);
    }
    
    /*
    /**********************************************************
    /* Numeric accessors
    /**********************************************************
     */

    @Override
    public BigInteger getBigIntegerValue() throws IOException, JsonParseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException, JsonParseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getDoubleValue() throws IOException, JsonParseException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloatValue() throws IOException, JsonParseException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getIntValue() throws IOException, JsonParseException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLongValue() throws IOException, JsonParseException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public NumberType getNumberType() throws IOException, JsonParseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number getNumberValue() throws IOException, JsonParseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
    /**********************************************************
    /* Abstract method impls for stuff from JsonParser
    /**********************************************************
     */

    /**
     * Method called when an EOF is encountered between tokens.
     * If so, it may be a legitimate EOF, but only iff there
     * is no open non-root context.
     */
    @Override
    protected void _handleEOF() throws JsonParseException
    {
        if (!_parsingContext.inRoot()) {
            _reportInvalidEOF(": expected close marker for "+_parsingContext.getTypeDesc()+" (from "+_parsingContext.getStartLocation(_ioContext.getSourceReference())+")");
        }
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected JsonLocation _extractLocation(XMLStreamLocation2 location)
    {
        if (location == null) { // just for impls that might pass null...
            return new JsonLocation(_ioContext.getSourceReference(), -1, -1, -1);
        }
        return new JsonLocation(_ioContext.getSourceReference(),
                location.getCharacterOffset(),
                location.getLineNumber(),
                location.getColumnNumber());
    }

    /**
     * Method called to release internal buffers owned by the base
     * reader. This may be called along with {@link #_closeInput} (for
     * example, when explicitly closing this reader instance), or
     * separately (if need be).
     */
    protected void _releaseBuffers() throws IOException
    {
        /*
        _textBuffer.releaseBuffers();
        char[] buf = _nameCopyBuffer;
        if (buf != null) {
            _nameCopyBuffer = null;
            _ioContext.releaseNameCopyBuffer(buf);
        }
        */
    }

    protected ByteArrayBuilder _getByteArrayBuilder()
    {
        if (_byteArrayBuilder == null) {
            _byteArrayBuilder = new ByteArrayBuilder();
        } else {
            _byteArrayBuilder.reset();
        }
        return _byteArrayBuilder;
    }

}
