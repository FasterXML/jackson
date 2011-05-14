package org.codehaus.jackson.smile;

import static org.codehaus.jackson.smile.SmileConstants.BYTE_MARKER_END_OF_STRING;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.StreamBasedParserBase;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.sym.BytesToNameCanonicalizer;
import org.codehaus.jackson.sym.Name;

public class SmileParser
    extends StreamBasedParserBase
{
    /**
     * Enumeration that defines all togglable features for Smile generators.
     */
    public enum Feature {
        /**
         * Feature that determines whether 4-byte Smile header is mandatory in input,
         * or optional. If enabled, it means that only input that starts with the header
         * is accepted as valid; if disabled, header is optional. In latter case,
         * settings for content are assumed to be defaults.
         */
        REQUIRE_HEADER(true)
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

    private final static int[] NO_INTS = new int[0];

    private final static String[] NO_STRINGS = new String[0];
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */
    
    /**
     * Codec used for data binding when (if) requested.
     */
    protected ObjectCodec _objectCodec;

    /**
     * Flag that indicates whether content can legally have raw (unquoted)
     * binary data. Since this information is included both in header and
     * in actual binary data blocks there is redundancy, and we want to
     * ensure settings are compliant. Using application may also want to
     * know this setting in case it does some direct (random) access.
     */
    protected boolean _mayContainRawBinary;

    /**
     * Helper object used for low-level recycling of Smile-generator
     * specific buffers.
     * 
     * @since 1.7
     */
    final protected SmileBufferRecycler<String> _smileBufferRecycler;
    
    /*
    /**********************************************************
    /* Additional parsing state
    /**********************************************************
     */

    /**
     * Flag that indicates that the current token has not yet
     * been fully processed, and needs to be finished for
     * some access (or skipped to obtain the next token)
     */
    protected boolean _tokenIncomplete = false;
    
    /**
     * Type byte of the current token
     */
    protected int _typeByte;

    /**
     * Specific flag that is set when we encountered a 32-bit
     * floating point value; needed since numeric super classes do
     * not track distinction between float and double, but Smile
     * format does, and we want to retain that separation.
     */
    protected boolean _got32BitFloat;

    /*
    /**********************************************************
    /* Symbol handling, decoding
    /**********************************************************
     */

    /**
     * Symbol table that contains field names encountered so far
     */
    final protected BytesToNameCanonicalizer _symbols;
    
    /**
     * Temporary buffer used for name parsing.
     */
    protected int[] _quadBuffer = NO_INTS;

    /**
     * Quads used for hash calculation
     */
    protected int _quad1, _quad2;
     
    /**
     * Array of recently seen field names, which may be back referenced
     * by later fields.
     * Defaults set to enable handling even if no header found.
     */
    protected String[] _seenNames = NO_STRINGS;

    protected int _seenNameCount = 0;

    /**
     * Array of recently seen field names, which may be back referenced
     * by later fields
     * Defaults set to disable handling if no header found.
     */
    protected String[] _seenStringValues = null;

    protected int _seenStringValueCount = -1;
    
    /*
    /**********************************************************
    /* Thread-local recycling
    /**********************************************************
     */
    
    /**
     * <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftRerefence}
     * to a buffer recycler used to provide a low-cost
     * buffer recycling for Smile-specific buffers.
     */
    final protected static ThreadLocal<SoftReference<SmileBufferRecycler<String>>> _smileRecyclerRef
        = new ThreadLocal<SoftReference<SmileBufferRecycler<String>>>();
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public SmileParser(IOContext ctxt, int parserFeatures, int smileFeatures,
            ObjectCodec codec,
            BytesToNameCanonicalizer sym,
            InputStream in, byte[] inputBuffer, int start, int end,
            boolean bufferRecyclable)
    {
        super(ctxt, parserFeatures, in, inputBuffer, start, end, bufferRecyclable);
        _objectCodec = codec;
        _symbols = sym;
        _tokenInputRow = -1;
        _tokenInputCol = -1;
        _smileBufferRecycler = _smileBufferRecycler();
    }

    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

    /**
     * Helper method called when it looks like input might contain the signature;
     * and it is necessary to detect and handle signature to get configuration
     * information it might have.
     * 
     * @return True if valid signature was found and handled; false if not
     */
    protected boolean handleSignature(boolean consumeFirstByte, boolean throwException)
        throws IOException, JsonParseException
    {
        if (consumeFirstByte) {
            ++_inputPtr;
        }
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        if (_inputBuffer[_inputPtr] != SmileConstants.HEADER_BYTE_2) {
            if (throwException) {
            	_reportError("Malformed content: signature not valid, starts with 0x3a but followed by 0x"
            			+Integer.toHexString(_inputBuffer[_inputPtr])+", not 0x29");
            }
            return false;
        }
        if (++_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();        	
        }
        if (_inputBuffer[_inputPtr] != SmileConstants.HEADER_BYTE_3) {
            if (throwException) {
            	_reportError("Malformed content: signature not valid, starts with 0x3a, 0x29, but followed by 0x"
            			+Integer.toHexString(_inputBuffer[_inputPtr])+", not 0xA");
            }
            return false;
        }
    	// Good enough; just need version info from 4th byte...
        if (++_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();        	
        }
        int ch = _inputBuffer[_inputPtr++];
        int versionBits = (ch >> 4) & 0x0F;
        // but failure with version number is fatal, can not ignore
        if (versionBits != SmileConstants.HEADER_VERSION_0) {
            _reportError("Header version number bits (0x"+Integer.toHexString(versionBits)+") indicate unrecognized version; only 0x0 handled by parser");
        }

        // can avoid tracking names, if explicitly disabled
        if ((ch & SmileConstants.HEADER_BIT_HAS_SHARED_NAMES) == 0) {
            _seenNames = null;
            _seenNameCount = -1;
        }
        // conversely, shared string values must be explicitly enabled
        if ((ch & SmileConstants.HEADER_BIT_HAS_SHARED_STRING_VALUES) != 0) {
            _seenStringValues = NO_STRINGS;
            _seenStringValueCount = 0;
        }
        _mayContainRawBinary = ((ch & SmileConstants.HEADER_BIT_HAS_RAW_BINARY) != 0);
        return true;
    }

    /**
     * @since 1.7
     */
    protected final static SmileBufferRecycler<String> _smileBufferRecycler()
    {
        SoftReference<SmileBufferRecycler<String>> ref = _smileRecyclerRef.get();
        SmileBufferRecycler<String> br = (ref == null) ? null : ref.get();

        if (br == null) {
            br = new SmileBufferRecycler<String>();
            _smileRecyclerRef.set(new SoftReference<SmileBufferRecycler<String>>(br));
        }
        return br;
    }
    
    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    @Override
    protected void _finishString() throws IOException, JsonParseException
    {
        // should never be called; but must be defined for superclass
        _throwInternal();
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        // Merge found symbols, if any:
        _symbols.release();
    }

    @Override
    protected void _releaseBuffers() throws IOException
    {
        super._releaseBuffers();
        {
            String[] nameBuf = _seenNames;
            if (nameBuf != null && nameBuf.length > 0) {
                _seenNames = null;
                // Note: while not mandatory, it's probably good idea to clear up cruft to reduce memory retention
                Arrays.fill(nameBuf, 0, _seenNameCount, null);
                _smileBufferRecycler.releaseSeenNamesBuffer(nameBuf);
            }
        }
        {
            String[] valueBuf = _seenStringValues;
            if (valueBuf != null && valueBuf.length > 0) {
                _seenStringValues = null;
                // Note: while not mandatory, it's probably good idea to clear up cruft to reduce memory retention
                Arrays.fill(valueBuf, 0, _seenStringValueCount, null);
                _smileBufferRecycler.releaseSeenStringValuesBuffer(valueBuf);
            }
        }
    }
    
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    public boolean mayContainRawBinary() {
        return _mayContainRawBinary;
    }
    
    /*
    /**********************************************************
    /* JsonParser impl
    /**********************************************************
     */

    @Override
    public JsonToken nextToken() throws IOException, JsonParseException
    {
        // For longer tokens (text, binary), we'll only read when requested
        if (_tokenIncomplete) {
            _skipIncomplete();
        }
        _tokenInputTotal = _currInputProcessed + _inputPtr - 1;
        // also: clear any data retained so far
        _binaryValue = null;
        // Two main modes: values, and field names.
        if (_parsingContext.inObject() && _currToken != JsonToken.FIELD_NAME) {
            return (_currToken = _handleFieldName());
        }
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                _handleEOF();
                /* NOTE: here we can and should close input, release buffers,
                 * since this is "hard" EOF, not a boundary imposed by
                 * header token.
                 */
                close();
                return (_currToken = null);
            }
        }
        int ch = _inputBuffer[_inputPtr++];
        _typeByte = ch;
        switch ((ch >> 5) & 0x7) {
        case 0: // short shared string value reference
            if (ch == 0) { // important: this is invalid, don't accept
                _reportError("Invalid token byte 0x00");
            }
            return _handleSharedString(ch-1);

        case 1: // simple literals, numbers
            {
                int typeBits = ch & 0x1F;
                if (typeBits < 4) {
                    switch (typeBits) {
                    case 0x00:
                        _textBuffer.resetWithEmpty();
                        return (_currToken = JsonToken.VALUE_STRING);
                    case 0x01:
                        return (_currToken = JsonToken.VALUE_NULL);
                    case 0x02: // false
                        return (_currToken = JsonToken.VALUE_FALSE);
                    default: // 0x03 == true
                        return (_currToken = JsonToken.VALUE_TRUE);
                    }
                }
                // next 3 bytes define subtype
                if (typeBits < 8) { // VInt (zigzag), BigInteger
                    if ((typeBits & 0x3) <= 0x2) { // 0x3 reserved (should never occur)
                        _tokenIncomplete = true;
                        _numTypesValid = 0;
                        return (_currToken = JsonToken.VALUE_NUMBER_INT);
                    }
                    break;
                }
                if (typeBits < 12) { // floating-point
                    int subtype = typeBits & 0x3;
                    if (subtype <= 0x2) { // 0x3 reserved (should never occur)
                        _tokenIncomplete = true;
                        _numTypesValid = 0;
                        _got32BitFloat = (subtype == 0);
                        return (_currToken = JsonToken.VALUE_NUMBER_FLOAT);
                    }
                    break;
                }
                if (typeBits == 0x1A) { // == 0x3A == ':' -> possibly header signature for next chunk?
                    if (handleSignature(false, false)) {
                        /* Ok, now; end-marker and header both imply doc boundary and a
                         * 'null token'; but if both are seen, they are collapsed.
                         * We can check this by looking at current token; if it's null,
                         * need to get non-null token
                         */
                        if (_currToken == null) {
                            return nextToken();
                        }
                        return (_currToken = null);
                    }
            	}
            	_reportError("Unrecognized token byte 0x3A (malformed segment header?");
            }
            // and everything else is reserved, for now
            break;
        case 2: // tiny ASCII
            // fall through            
        case 3: // short ASCII
            // fall through
        case 4: // tiny Unicode
            // fall through
        case 5: // short Unicode
            // No need to decode, unless we have to keep track of back-references (for shared string values)
            _currToken = JsonToken.VALUE_STRING;
            if (_seenStringValueCount >= 0) { // shared text values enabled
                _addSeenStringValue();
            } else {
                _tokenIncomplete = true;
            }
            return _currToken;
        case 6: // small integers; zigzag encoded
            _numberInt = SmileUtil.zigzagDecode(ch & 0x1F);
            _numTypesValid = NR_INT;
            return (_currToken = JsonToken.VALUE_NUMBER_INT);
        case 7: // binary/long-text/long-shared/start-end-markers
            switch (ch & 0x1F) {
            case 0x00: // long variable length ASCII
            case 0x04: // long variable length unicode
                _tokenIncomplete = true;
                return (_currToken = JsonToken.VALUE_STRING);
            case 0x08: // binary, 7-bit
                _tokenIncomplete = true;
                return (_currToken = JsonToken.VALUE_EMBEDDED_OBJECT);
            case 0x0C: // long shared string
            case 0x0D:
            case 0x0E:
            case 0x0F:
                if (_inputPtr >= _inputEnd) {
                    loadMoreGuaranteed();
                }
                return _handleSharedString(((ch & 0x3) << 8) + (_inputBuffer[_inputPtr++] & 0xFF));
            case 0x18: // START_ARRAY
                _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
                return (_currToken = JsonToken.START_ARRAY);
            case 0x19: // END_ARRAY
                if (!_parsingContext.inArray()) {
                    _reportMismatchedEndMarker(']', '}');
                }
                _parsingContext = _parsingContext.getParent();
                return (_currToken = JsonToken.END_ARRAY);
            case 0x1A: // START_OBJECT
                _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
                return (_currToken = JsonToken.START_OBJECT);
            case 0x1B: // not used in this mode; would be END_OBJECT
                _reportError("Invalid type marker byte 0xFB in value mode (would be END_OBJECT in key mode)");
            case 0x1D: // binary, raw
                _tokenIncomplete = true;
                return (_currToken = JsonToken.VALUE_EMBEDDED_OBJECT);
            case 0x1F: // 0xFF, end of content
                return (_currToken = null);
            }
            break;
        }
        // If we get this far, type byte is corrupt
        _reportError("Invalid type marker byte 0x"+Integer.toHexString(ch & 0xFF)+" for expected value token");
        return null;
    }

    private final JsonToken _handleSharedString(int index)
        throws IOException, JsonParseException
    {
        if (index >= _seenStringValueCount) {
            _reportInvalidSharedStringValue(index);
        }
        _textBuffer.resetWithString(_seenStringValues[index]);
        return (_currToken = JsonToken.VALUE_STRING);
    }

    private final void _addSeenStringValue()
        throws IOException, JsonParseException
    {
        _finishToken();
        if (_seenStringValueCount < _seenStringValues.length) {
            // !!! TODO: actually only store char[], first time around?
            _seenStringValues[_seenStringValueCount++] = _textBuffer.contentsAsString();
            return;
        }
        _expandSeenStringValues();
    }
    
    private final void _expandSeenStringValues()
    {
        String[] oldShared = _seenStringValues;
        int len = oldShared.length;
        String[] newShared;
        if (len == 0) {
            newShared = _smileBufferRecycler.allocSeenStringValuesBuffer();
            if (newShared == null) {
                newShared = new String[SmileBufferRecycler.DEFAULT_STRING_VALUE_BUFFER_LENGTH];
            }
        } else if (len == SmileConstants.MAX_SHARED_STRING_VALUES) { // too many? Just flush...
           newShared = oldShared;
           _seenStringValueCount = 0; // could also clear, but let's not yet bother
        } else {
            int newSize = (len == SmileBufferRecycler.DEFAULT_NAME_BUFFER_LENGTH) ? 256 : SmileConstants.MAX_SHARED_STRING_VALUES;
            newShared = new String[newSize];
            System.arraycopy(oldShared, 0, newShared, 0, oldShared.length);
        }
        _seenStringValues = newShared;
        _seenStringValues[_seenStringValueCount++] = _textBuffer.contentsAsString();
    }

    @Override
    public String getCurrentName() throws IOException, JsonParseException
    {
        return _parsingContext.getCurrentName();
    }

    @Override
    public NumberType getNumberType()
        throws IOException, JsonParseException
    {
    	if (_got32BitFloat) {
    	    return NumberType.FLOAT;
    	}
    	return super.getNumberType();
    }
    
    /*
    /**********************************************************
    /* Public API, access to token information, text
    /**********************************************************
     */

    /**
     * Method for accessing textual representation of the current event;
     * if no current event (before first call to {@link #nextToken}, or
     * after encountering end-of-input), returns null.
     * Method can be called for any event.
     */
    @Override    
    public String getText()
        throws IOException, JsonParseException
    {
        if (_tokenIncomplete) {
            _tokenIncomplete = false;
            // Let's inline part of "_finishToken", common case
            int tb = _typeByte;
            int type = (tb >> 5) & 0x7;
            if (type == 2 || type == 3) { // tiny & short ASCII
                _decodeShortAsciiValue(1 + (tb & 0x3F));
                return _textBuffer.contentsAsString();
            }
            if (type == 4 || type == 5) { // tiny & short Unicode
                 // short unicode; note, lengths 2 - 65  (off-by-one compared to ASCII)
                _decodeShortUnicodeValue(2 + (tb & 0x3F));
                return _textBuffer.contentsAsString();
            }
            _finishToken();
        }
        if (_currToken == JsonToken.VALUE_STRING) {
            return _textBuffer.contentsAsString();
        }
        JsonToken t = _currToken;
        if (t == null) { // null only before/after document
            return null;
        }
        if (t == JsonToken.FIELD_NAME) {
            return _parsingContext.getCurrentName();
        }
        if (t.isNumeric()) {
            // TODO: optimize?
            return getNumberValue().toString();
        }
        return _currToken.asString();
    }

    @Override
    public char[] getTextCharacters()
        throws IOException, JsonParseException
    {
        if (_currToken != null) { // null only before/after document
            if (_tokenIncomplete) {
                _finishToken();
            }
            switch (_currToken) {                
            case VALUE_STRING:
                return _textBuffer.getTextBuffer();
            case FIELD_NAME:
                if (!_nameCopied) {
                    String name = _parsingContext.getCurrentName();
                    int nameLen = name.length();
                    if (_nameCopyBuffer == null) {
                        _nameCopyBuffer = _ioContext.allocNameCopyBuffer(nameLen);
                    } else if (_nameCopyBuffer.length < nameLen) {
                        _nameCopyBuffer = new char[nameLen];
                    }
                    name.getChars(0, nameLen, _nameCopyBuffer, 0);
                    _nameCopied = true;
                }
                return _nameCopyBuffer;

                // fall through
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                // TODO: optimize
                return getNumberValue().toString().toCharArray();
                
            default:
                return _currToken.asCharArray();
            }
        }
        return null;
    }

    @Override    
    public int getTextLength()
        throws IOException, JsonParseException
    {
        if (_currToken != null) { // null only before/after document
            if (_tokenIncomplete) {
                _finishToken();
            }
            switch (_currToken) {
            case VALUE_STRING:
                return _textBuffer.size();                
            case FIELD_NAME:
                return _parsingContext.getCurrentName().length();
                // fall through
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                // TODO: optimize
                return getNumberValue().toString().length();
                
            default:
                return _currToken.asCharArray().length;
            }
        }
        return 0;
    }

    @Override
    public int getTextOffset() throws IOException, JsonParseException
    {
        return 0;
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
        if (_tokenIncomplete) {
            _finishToken();
        }
        if (_currToken != JsonToken.VALUE_EMBEDDED_OBJECT ) {
            // Todo, maybe: support base64 for text?
            _reportError("Current token ("+_currToken+") not VALUE_EMBEDDED_OBJECT, can not access as binary");
        }
        return _binaryValue;
    }

    @Override
    protected byte[] _decodeBase64(Base64Variant b64variant)
        throws IOException, JsonParseException
    {
        // Should never get called, but must be defined for base class
        _throwInternal();
        return null;
    }
    
    /*
    /**********************************************************
    /* Internal methods, field name parsing
    /**********************************************************
     */

    /**
     * Method that handles initial token type recognition for token
     * that has to be either FIELD_NAME or END_OBJECT.
     */
    protected final JsonToken _handleFieldName() throws IOException, JsonParseException
    {    	
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        int ch = _inputBuffer[_inputPtr++];
        // is this needed?
        _typeByte = ch;
        switch ((ch >> 6) & 3) {
        case 0: // misc, including end marker
            switch (ch) {
            case 0x20: // empty String as name, legal if unusual
                _parsingContext.setCurrentName("");
                return JsonToken.FIELD_NAME;
            case 0x30: // long shared
            case 0x31:
            case 0x32:
            case 0x33:
                {
                    if (_inputPtr >= _inputEnd) {
                        loadMoreGuaranteed();
	            }
	            int index = ((ch & 0x3) << 8) + (_inputBuffer[_inputPtr++] & 0xFF);
                    if (index >= _seenNameCount) {
                        _reportInvalidSharedName(index);
                    }
	            _parsingContext.setCurrentName(_seenNames[index]);
	        }
                return JsonToken.FIELD_NAME;
            case 0x34: // long ASCII/Unicode name
                _handleLongFieldName();
                return JsonToken.FIELD_NAME;            	
            }
            break;
        case 1: // short shared, can fully process
            {
                int index = (ch & 0x3F);
                if (index >= _seenNameCount) {
                    _reportInvalidSharedName(index);
                }
                _parsingContext.setCurrentName(_seenNames[index]);
            }
            return JsonToken.FIELD_NAME;
        case 2: // short ASCII
	    {
	        int len = 1 + (ch & 0x3f);
        	String name;
        	Name n = _findDecodedFromSymbols(len);
        	if (n != null) {
        	    name = n.getName();
        	    _inputPtr += len;
        	} else {
        	    name = _decodeShortAsciiName(len);
        	    name = _addDecodedToSymbols(len, name);
        	}
                if (_seenNames != null) {
                   if (_seenNameCount >= _seenNames.length) {
   	               _seenNames = _expandSeenNames(_seenNames);
                   }
                   _seenNames[_seenNameCount++] = name;
                }
                _parsingContext.setCurrentName(name);
	    }
	    return JsonToken.FIELD_NAME;                
        case 3: // short Unicode
            // all valid, except for 0xFF
            {
                int len = (ch & 0x3F);
                if (len > 0x37) {
                    if (len == 0x3B) {
                        if (!_parsingContext.inObject()) {
                            _reportMismatchedEndMarker('}', ']');
                        }
                        _parsingContext = _parsingContext.getParent();
                        return JsonToken.END_OBJECT;
                    }
                } else {
                    len += 2; // values from 2 to 57...
                    String name;
                    Name n = _findDecodedFromSymbols(len);
                    if (n != null) {
                        name = n.getName();
                        _inputPtr += len;
                    } else {
                        name = _decodeShortUnicodeName(len);
                        name = _addDecodedToSymbols(len, name);
                    }
                    if (_seenNames != null) {
                        if (_seenNameCount >= _seenNames.length) {
    	                    _seenNames = _expandSeenNames(_seenNames);
                        }
                        _seenNames[_seenNameCount++] = name;
                    }
                    _parsingContext.setCurrentName(name);
                    return JsonToken.FIELD_NAME;                
                }
            }
            break;
        }
        // Other byte values are illegal
        _reportError("Invalid type marker byte 0x"+Integer.toHexString(ch)+" for expected field name (or END_OBJECT marker)");
        return null;
    }

    /**
     * Method called to try to expand shared name area to fit one more potentially
     * shared String. If area is already at its biggest size, will just clear
     * the area (by setting next-offset to 0)
     */
    private final String[] _expandSeenNames(String[] oldShared)
    {
        int len = oldShared.length;
        String[] newShared;
        if (len == 0) {
            newShared = _smileBufferRecycler.allocSeenNamesBuffer();
            if (newShared == null) {
                newShared = new String[SmileBufferRecycler.DEFAULT_NAME_BUFFER_LENGTH];                
            }
        } else if (len == SmileConstants.MAX_SHARED_NAMES) { // too many? Just flush...
      	   newShared = oldShared;
      	   _seenNameCount = 0; // could also clear, but let's not yet bother
        } else {
            int newSize = (len == SmileBufferRecycler.DEFAULT_STRING_VALUE_BUFFER_LENGTH) ? 256 : SmileConstants.MAX_SHARED_NAMES;
            newShared = new String[newSize];
            System.arraycopy(oldShared, 0, newShared, 0, oldShared.length);
        }
        return newShared;
    }
    
    private final String _addDecodedToSymbols(int len, String name)
    {
        if (len < 5) {
            return _symbols.addName(name, _quad1, 0).getName();
        }
	if (len < 9) {
    	    return _symbols.addName(name, _quad1, _quad2).getName();
	}
	int qlen = (len + 3) >> 2;
	return _symbols.addName(name, _quadBuffer, qlen).getName();
    }

    private final String _decodeShortAsciiName(int len)
        throws IOException, JsonParseException
    {
        // note: caller ensures we have enough bytes available
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int outPtr = 0;
        final byte[] inBuf = _inputBuffer;
        int inPtr = _inputPtr;
        
        // loop unrolling seems to help here:
        for (int inEnd = inPtr + len - 3; inPtr < inEnd; ) {
            outBuf[outPtr++] = (char) inBuf[inPtr++];            
            outBuf[outPtr++] = (char) inBuf[inPtr++];            
            outBuf[outPtr++] = (char) inBuf[inPtr++];            
            outBuf[outPtr++] = (char) inBuf[inPtr++];            
        }
        int left = (len & 3);
        if (left > 0) {
            outBuf[outPtr++] = (char) inBuf[inPtr++];
            if (left > 1) {
                outBuf[outPtr++] = (char) inBuf[inPtr++];
                if (left > 2) {
                    outBuf[outPtr++] = (char) inBuf[inPtr++];
                }
            }
        } 
        _inputPtr = inPtr;
        _textBuffer.setCurrentLength(len);
        return _textBuffer.contentsAsString();
    }
    
    /**
     * Helper method used to decode short Unicode string, length for which actual
     * length (in bytes) is known
     * 
     * @param len Length between 1 and 64
     */
    private final String _decodeShortUnicodeName(int len)
        throws IOException, JsonParseException
    {
        // note: caller ensures we have enough bytes available
        int outPtr = 0;
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int inPtr = _inputPtr;
        _inputPtr += len;
        final int[] codes = SmileConstants.sUtf8UnitLengths;
        final byte[] inBuf = _inputBuffer;
        for (int end = inPtr + len; inPtr < end; ) {
            int i = inBuf[inPtr++] & 0xFF;
            int code = codes[i];
            if (code != 0) {
                // trickiest one, need surrogate handling
                switch (code) {
                case 1:
                    i = ((i & 0x1F) << 6) | (inBuf[inPtr++] & 0x3F);
                    break;
                case 2:
                    i = ((i & 0x0F) << 12)
                        | ((inBuf[inPtr++] & 0x3F) << 6)
                        | (inBuf[inPtr++] & 0x3F);
                    break;
                case 3:
                    i = ((i & 0x07) << 18)
                    | ((inBuf[inPtr++] & 0x3F) << 12)
                    | ((inBuf[inPtr++] & 0x3F) << 6)
                    | (inBuf[inPtr++] & 0x3F);
                    // note: this is the codepoint value; need to split, too
                    i -= 0x10000;
                    outBuf[outPtr++] = (char) (0xD800 | (i >> 10));
                    i = 0xDC00 | (i & 0x3FF);
                    break;
                default: // invalid
                    _reportError("Invalid byte "+Integer.toHexString(i)+" in short Unicode text block");
                }
            }
            outBuf[outPtr++] = (char) i;
        }
        _textBuffer.setCurrentLength(outPtr);
        return _textBuffer.contentsAsString();
    }

    // note: slightly edited copy of UTF8StreamParser.addName()
    private final Name _decodeLongUnicodeName(int[] quads, int byteLen, int quadLen)
        throws IOException, JsonParseException
    {
        int lastQuadBytes = byteLen & 3;
        // Ok: must decode UTF-8 chars. No other validation SHOULD be needed (except bounds checks?)
        /* Note: last quad is not correctly aligned (leading zero bytes instead
         * need to shift a bit, instead of trailing). Only need to shift it
         * for UTF-8 decoding; need revert for storage (since key will not
         * be aligned, to optimize lookup speed)
         */
        int lastQuad;
    
        if (lastQuadBytes < 4) {
            lastQuad = quads[quadLen-1];
            // 8/16/24 bit left shift
            quads[quadLen-1] = (lastQuad << ((4 - lastQuadBytes) << 3));
        } else {
            lastQuad = 0;
        }

        char[] cbuf = _textBuffer.emptyAndGetCurrentSegment();
        int cix = 0;
    
        for (int ix = 0; ix < byteLen; ) {
            int ch = quads[ix >> 2]; // current quad, need to shift+mask
            int byteIx = (ix & 3);
            ch = (ch >> ((3 - byteIx) << 3)) & 0xFF;
            ++ix;
    
            if (ch > 127) { // multi-byte
                int needed;
                if ((ch & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
                    ch &= 0x1F;
                    needed = 1;
                } else if ((ch & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
                    ch &= 0x0F;
                    needed = 2;
                } else if ((ch & 0xF8) == 0xF0) { // 4 bytes; double-char with surrogates and all...
                    ch &= 0x07;
                    needed = 3;
                } else { // 5- and 6-byte chars not valid chars
                    _reportInvalidInitial(ch);
                    needed = ch = 1; // never really gets this far
                }
                if ((ix + needed) > byteLen) {
                    _reportInvalidEOF(" in long field name");
                }
                
                // Ok, always need at least one more:
                int ch2 = quads[ix >> 2]; // current quad, need to shift+mask
                byteIx = (ix & 3);
                ch2 = (ch2 >> ((3 - byteIx) << 3));
                ++ix;
                
                if ((ch2 & 0xC0) != 0x080) {
                    _reportInvalidOther(ch2);
                }
                ch = (ch << 6) | (ch2 & 0x3F);
                if (needed > 1) {
                    ch2 = quads[ix >> 2];
                    byteIx = (ix & 3);
                    ch2 = (ch2 >> ((3 - byteIx) << 3));
                    ++ix;
                    
                    if ((ch2 & 0xC0) != 0x080) {
                        _reportInvalidOther(ch2);
                    }
                    ch = (ch << 6) | (ch2 & 0x3F);
                    if (needed > 2) { // 4 bytes? (need surrogates on output)
                        ch2 = quads[ix >> 2];
                        byteIx = (ix & 3);
                        ch2 = (ch2 >> ((3 - byteIx) << 3));
                        ++ix;
                        if ((ch2 & 0xC0) != 0x080) {
                            _reportInvalidOther(ch2 & 0xFF);
                        }
                        ch = (ch << 6) | (ch2 & 0x3F);
                    }
                }
                if (needed > 2) { // surrogate pair? once again, let's output one here, one later on
                    ch -= 0x10000; // to normalize it starting with 0x0
                    if (cix >= cbuf.length) {
                        cbuf = _textBuffer.expandCurrentSegment();
                    }
                    cbuf[cix++] = (char) (0xD800 + (ch >> 10));
                    ch = 0xDC00 | (ch & 0x03FF);
                }
            }
            if (cix >= cbuf.length) {
                cbuf = _textBuffer.expandCurrentSegment();
            }
            cbuf[cix++] = (char) ch;
        }

        // Ok. Now we have the character array, and can construct the String
        String baseName = new String(cbuf, 0, cix);
        // And finally, un-align if necessary
        if (lastQuadBytes < 4) {
            quads[quadLen-1] = lastQuad;
        }
        return _symbols.addName(baseName, quads, quadLen);
    }
    
    private final void _handleLongFieldName() throws IOException, JsonParseException
    {
        // First: gather quads we need, looking for end marker
        final byte[] inBuf = _inputBuffer;
        int quads = 0;
        int bytes = 0;
        int q = 0;

        while (true) {
            byte b = inBuf[_inputPtr++];
            if (BYTE_MARKER_END_OF_STRING == b) {
                bytes = 0;
                break;
            }
            q = ((int) b) & 0xFF;
            b = inBuf[_inputPtr++];
            if (BYTE_MARKER_END_OF_STRING == b) {
                bytes = 1;
                break;
            }
            q = (q << 8) | (b & 0xFF);
            b = inBuf[_inputPtr++];
            if (BYTE_MARKER_END_OF_STRING == b) {
                bytes = 2;
                break;
            }
            q = (q << 8) | (b & 0xFF);
            b = inBuf[_inputPtr++];
            if (BYTE_MARKER_END_OF_STRING == b) {
                bytes = 3;
                break;
            }
            q = (q << 8) | (b & 0xFF);
            if (quads >= _quadBuffer.length) {
                _quadBuffer = _growArrayTo(_quadBuffer, _quadBuffer.length + 256); // grow by 1k
            }
            _quadBuffer[quads++] = q;
        }
        // and if we have more bytes, append those too
        int byteLen = (quads << 2);
        if (bytes > 0) {
            if (quads >= _quadBuffer.length) {
                _quadBuffer = _growArrayTo(_quadBuffer, _quadBuffer.length + 256);
            }
            _quadBuffer[quads++] = q;
            byteLen += bytes;
        }
        
        // Know this name already?
        String name;
        Name n = _symbols.findName(_quadBuffer, quads);
        if (n != null) {
            name = n.getName();
        } else {
            name = _decodeLongUnicodeName(_quadBuffer, byteLen, quads).getName();
        }
        if (_seenNames != null) {
           if (_seenNameCount >= _seenNames.length) {
               _seenNames = _expandSeenNames(_seenNames);
           }
           _seenNames[_seenNameCount++] = name;
        }
        _parsingContext.setCurrentName(name);
    }
    
    /**
     * Helper method for trying to find specified encoded UTF-8 byte sequence
     * from symbol table; if successful avoids actual decoding to String
     */
    private final Name _findDecodedFromSymbols(int len)
    	throws IOException, JsonParseException
    {
        if ((_inputEnd - _inputPtr) < len) {
            _loadToHaveAtLeast(len);
        }
	// First: maybe we already have this name decoded?
	if (len < 5) {
	    int inPtr = _inputPtr;
	    final byte[] inBuf = _inputBuffer;
	    int q = inBuf[inPtr] & 0xFF;
	    if (--len > 0) {
	        q = (q << 8) + (inBuf[++inPtr] & 0xFF);
	        if (--len > 0) {
	            q = (q << 8) + (inBuf[++inPtr] & 0xFF);
	            if (--len > 0) {
	                q = (q << 8) + (inBuf[++inPtr] & 0xFF);
	            }
	        }
	    }
	    _quad1 = q;
	    return _symbols.findName(q);
	}
        if (len < 9) {
            int inPtr = _inputPtr;
            final byte[] inBuf = _inputBuffer;
            // First quadbyte is easy
            int q1 = inBuf[inPtr++] << 8;
            q1 += (inBuf[inPtr++] & 0xFF);
            q1 <<= 8;
            q1 += (inBuf[inPtr++] & 0xFF);
            q1 <<= 8;
            q1 += (inBuf[inPtr++] & 0xFF);
            int q2 = (inBuf[inPtr++] & 0xFF);
            len -= 5;
            if (len > 0) {
                q2 = (q2 << 8) + (inBuf[inPtr++] & 0xFF);
                if (--len >= 0) {
                    q2 = (q2 << 8) + (inBuf[inPtr++] & 0xFF);
                    if (--len >= 0) {
                        q2 = (q2 << 8) + (inBuf[inPtr++] & 0xFF);
                    }
                }
            }
            _quad1 = q1;
            _quad2 = q2;
            return _symbols.findName(q1, q2);
        }
        return _findDecodedMedium(len);
    }

    /**
     * Method for locating names longer than 8 bytes (in UTF-8)
     */
    private final Name _findDecodedMedium(int len)
        throws IOException, JsonParseException
    {
    	// first, need enough buffer to store bytes as ints:
        {
            int bufLen = (len + 3) >> 2;
            if (bufLen > _quadBuffer.length) {
                _quadBuffer = _growArrayTo(_quadBuffer, bufLen);
            }
    	}
    	// then decode, full quads first
    	int offset = 0;
    	int inPtr = _inputPtr;
    	final byte[] inBuf = _inputBuffer;
        do {
            int q = (inBuf[inPtr++] & 0xFF) << 8;
            q |= inBuf[inPtr++] & 0xFF;
            q <<= 8;
            q |= inBuf[inPtr++] & 0xFF;
            q <<= 8;
            q |= inBuf[inPtr++] & 0xFF;
            _quadBuffer[offset++] = q;
        } while ((len -= 4) > 3);
        // and then leftovers
        if (len > 0) {
            int q = inBuf[inPtr++] & 0xFF;
            if (--len >= 0) {
                q = (q << 8) + (inBuf[inPtr++] & 0xFF);
                if (--len >= 0) {
                    q = (q << 8) + (inBuf[inPtr++] & 0xFF);
                }
            }
            _quadBuffer[offset++] = q;
        }
        return _symbols.findName(_quadBuffer, offset);
    }
    
    private static int[] _growArrayTo(int[] arr, int minSize)
    {
    	int[] newArray = new int[minSize + 4];
    	if (arr != null) {
            // !!! TODO: JDK 1.6, Arrays.copyOf
            System.arraycopy(arr, 0, newArray, 0, arr.length);
        }
        return newArray;
    }
    
    /*
    /**********************************************************
    /* Internal methods, secondary parsing
    /**********************************************************
     */

    @Override
    protected void _parseNumericValue(int expType)
    	throws IOException, JsonParseException
    {
    	if (_tokenIncomplete) {
    	    int tb = _typeByte;
    	    // ensure we got a numeric type with value that is lazily parsed
            if (((tb >> 5) & 0x7) != 1) {
                _reportError("Current token ("+_currToken+") not numeric, can not use numeric value accessors");
            }
            _tokenIncomplete = false;
            _finishNumberToken(tb);
    	}
    }
    
    /**
     * Method called to finish parsing of a token so that token contents
     * are retriable
     */
    protected void _finishToken()
    	throws IOException, JsonParseException
    {
        _tokenIncomplete = false;
    	int tb = _typeByte;

    	int type = ((tb >> 5) & 0x7);
        if (type == 1) { // simple literals, numbers
            _finishNumberToken(tb);
            return;
        }
        if (type <= 3) { // tiny & short ASCII
            _decodeShortAsciiValue(1 + (tb & 0x3F));
            return;
    	}
        if (type <= 5) { // tiny & short Unicode
             // short unicode; note, lengths 2 - 65  (off-by-one compared to ASCII)
            _decodeShortUnicodeValue(2 + (tb & 0x3F));
            return;
    	}
        if (type == 7) {
            tb &= 0x1F;
            // next 3 bytes define subtype
            switch (tb >> 2) {
            case 0: // long variable length ASCII
            	_decodeLongAscii();
            	return;
            case 1: // long variable length unicode
            	_decodeLongUnicode();
            	return;
            case 2: // binary, 7-bit
                _binaryValue = _read7BitBinaryWithLength();
                return;
            case 7: // binary, raw
                _finishRawBinary();
                return;
            }
        }
        // sanity check
    	_throwInternal();
    }

    protected final void _finishNumberToken(int tb)
        throws IOException, JsonParseException
    {
        tb &= 0x1F;
        int type = (tb >> 2);
        if (type == 1) { // VInt (zigzag) or BigDecimal
            int subtype = tb & 0x03;
            if (subtype == 0) { // (v)int
                _finishInt();
            } else if (subtype == 1) { // (v)long
                _finishLong();
            } else if (subtype == 2) {
                _finishBigInteger();
            } else {
                _throwInternal();
            }
            return;
        }
        if (type == 2) { // other numbers
            switch (tb & 0x03) {
            case 0: // float
                _finishFloat();
                return;
            case 1: // double
                _finishDouble();
                return;
            case 2: // big-decimal
                _finishBigDecimal();
                return;
            }
        }
        _throwInternal();
    }
    
    /*
    /**********************************************************
    /* Internal methods, secondary Number parsing
    /**********************************************************
     */
    
    private final void _finishInt() throws IOException, JsonParseException
    {
    	if (_inputPtr >= _inputEnd) {
    	    loadMoreGuaranteed();
    	}
    	int value = _inputBuffer[_inputPtr++];
    	int i;
    	if (value < 0) { // 6 bits
    		value &= 0x3F;
    	} else {
    	    if (_inputPtr >= _inputEnd) {
    	        loadMoreGuaranteed();
    	    }
    	    i = _inputBuffer[_inputPtr++];
    	    if (i >= 0) { // 13 bits
    	        value = (value << 7) + i;
    	        if (_inputPtr >= _inputEnd) {
    	            loadMoreGuaranteed();
    	        }
    	        i = _inputBuffer[_inputPtr++];
    	        if (i >= 0) {
    	            value = (value << 7) + i;
    	            if (_inputPtr >= _inputEnd) {
    	                loadMoreGuaranteed();
    	            }
    	            i = _inputBuffer[_inputPtr++];
    	            if (i >= 0) {
    	                value = (value << 7) + i;
    	                // and then we must get negative
    	                if (_inputPtr >= _inputEnd) {
    	                    loadMoreGuaranteed();
    	                }
    	                i = _inputBuffer[_inputPtr++];
    	                if (i >= 0) {
    	                    _reportError("Corrupt input; 32-bit VInt extends beyond 5 data bytes");
    	                }
    	            }
    	        }
    	    }
    	    value = (value << 6) + (i & 0x3F);
    	}
        _numberInt = SmileUtil.zigzagDecode(value);
    	_numTypesValid = NR_INT;
    }

    private final void  _finishLong()
        throws IOException, JsonParseException
    {
	// Ok, first, will always get 4 full data bytes first; 1 was already passed
	long l = (long) _fourBytesToInt();
    	// and loop for the rest
    	while (true) {
    	    if (_inputPtr >= _inputEnd) {
    	        loadMoreGuaranteed();
    	    }
    	    int value = _inputBuffer[_inputPtr++];
    	    if (value < 0) {
    	        l = (l << 6) + (value & 0x3F);
    	        _numberLong = SmileUtil.zigzagDecode(l);
    	        _numTypesValid = NR_LONG;
    	        return;
    	    }
    	    l = (l << 7) + value;
    	}
    }

    private final void _finishBigInteger()
	throws IOException, JsonParseException
    {
        byte[] raw = _read7BitBinaryWithLength();
        _numberBigInt = new BigInteger(raw);
        _numTypesValid = NR_BIGINT;
    }

    private final void _finishFloat()
        throws IOException, JsonParseException
    {
        // just need 5 bytes to get int32 first; all are unsigned
	int i = _fourBytesToInt();
    	if (_inputPtr >= _inputEnd) {
    		loadMoreGuaranteed();
    	}
    	i = (i << 7) + _inputBuffer[_inputPtr++];
    	float f = Float.intBitsToFloat(i);
	_numberDouble = (double) f;
	_numTypesValid = NR_DOUBLE;
    }

    private final void _finishDouble()
	throws IOException, JsonParseException
    {
        // ok; let's take two sets of 4 bytes (each is int)
	long hi = _fourBytesToInt();
	long value = (hi << 28) + (long) _fourBytesToInt();
	// and then remaining 2 bytes
	if (_inputPtr >= _inputEnd) {
	    loadMoreGuaranteed();
	}
	value = (value << 7) + _inputBuffer[_inputPtr++];
	if (_inputPtr >= _inputEnd) {
	    loadMoreGuaranteed();
	}
	value = (value << 7) + _inputBuffer[_inputPtr++];
	_numberDouble = Double.longBitsToDouble(value);
	_numTypesValid = NR_DOUBLE;
    }

    private final int _fourBytesToInt() 
        throws IOException, JsonParseException
    {
	if (_inputPtr >= _inputEnd) {
		loadMoreGuaranteed();
	}
	int i = _inputBuffer[_inputPtr++]; // first 7 bits
	if (_inputPtr >= _inputEnd) {
		loadMoreGuaranteed();
	}
	i = (i << 7) + _inputBuffer[_inputPtr++]; // 14 bits
	if (_inputPtr >= _inputEnd) {
		loadMoreGuaranteed();
	}
	i = (i << 7) + _inputBuffer[_inputPtr++]; // 21
	if (_inputPtr >= _inputEnd) {
		loadMoreGuaranteed();
	}
	return (i << 7) + _inputBuffer[_inputPtr++];
    }
	
    private final void _finishBigDecimal()
        throws IOException, JsonParseException
    {
        int scale = SmileUtil.zigzagDecode(_readUnsignedVInt());
        byte[] raw = _read7BitBinaryWithLength();
        _numberBigDecimal = new BigDecimal(new BigInteger(raw), scale);
        _numTypesValid = NR_BIGDECIMAL;
    }

    private final int _readUnsignedVInt()
        throws IOException, JsonParseException
    {
        int value = 0;
        while (true) {
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            int i = _inputBuffer[_inputPtr++];
            if (i < 0) { // last byte
                value = (value << 6) + (i & 0x3F);
                return value;
            }
            value = (value << 7) + i;
        }
    }

    private final byte[] _read7BitBinaryWithLength()
        throws IOException, JsonParseException
    {
        int byteLen = _readUnsignedVInt();
        byte[] result = new byte[byteLen];
        int ptr = 0;
        int lastOkPtr = byteLen - 7;
        
        // first, read all 7-by-8 byte chunks
        while (ptr <= lastOkPtr) {
            if ((_inputEnd - _inputPtr) < 8) {
                _loadToHaveAtLeast(8);
            }
            int i1 = (_inputBuffer[_inputPtr++] << 25)
                + (_inputBuffer[_inputPtr++] << 18)
                + (_inputBuffer[_inputPtr++] << 11)
                + (_inputBuffer[_inputPtr++] << 4);
            int x = _inputBuffer[_inputPtr++];
            i1 += x >> 3;
            int i2 = ((x & 0x7) << 21)
                + (_inputBuffer[_inputPtr++] << 14)
                + (_inputBuffer[_inputPtr++] << 7)
                + _inputBuffer[_inputPtr++];
            // Ok: got our 7 bytes, just need to split, copy
            result[ptr++] = (byte)(i1 >> 24);
            result[ptr++] = (byte)(i1 >> 16);
            result[ptr++] = (byte)(i1 >> 8);
            result[ptr++] = (byte)i1;
            result[ptr++] = (byte)(i2 >> 16);
            result[ptr++] = (byte)(i2 >> 8);
            result[ptr++] = (byte)i2;
        }
        // and then leftovers: n+1 bytes to decode n bytes
        int toDecode = (result.length - ptr);
        if (toDecode > 0) {
            if ((_inputEnd - _inputPtr) < (toDecode+1)) {
                _loadToHaveAtLeast(toDecode+1);
            }
            int value = _inputBuffer[_inputPtr++];
            for (int i = 1; i < toDecode; ++i) {
                value = (value << 7) + _inputBuffer[_inputPtr++];
                result[ptr++] = (byte) (value >> (7 - i));
            }
            // last byte is different, has remaining 1 - 6 bits, right-aligned
            value <<= toDecode;
            result[ptr] = (byte) (value + _inputBuffer[_inputPtr++]);
        }
        return result;
    }
    
    /*
    /**********************************************************
    /* Internal methods, secondary String parsing
    /**********************************************************
     */

    protected final void _decodeShortAsciiValue(int len)
        throws IOException, JsonParseException
    {
        if ((_inputEnd - _inputPtr) < len) {
            _loadToHaveAtLeast(len);
        }
        // Note: we count on fact that buffer must have at least 'len' (<= 64) empty char slots
	final char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int outPtr = 0;
        final byte[] inBuf = _inputBuffer;
	int inPtr = _inputPtr;
	
        // loop unrolling SHOULD be faster (as with _decodeShortAsciiName), but somehow
	// is NOT; as per testing, benchmarking... very weird.
	/*
        for (int inEnd = inPtr + len - 3; inPtr < inEnd; ) {
            outBuf[outPtr++] = (char) inBuf[inPtr++];            
            outBuf[outPtr++] = (char) inBuf[inPtr++];            
            outBuf[outPtr++] = (char) inBuf[inPtr++];            
            outBuf[outPtr++] = (char) inBuf[inPtr++];            
        }
        int left = (len & 3);
        if (left > 0) {
            outBuf[outPtr++] = (char) inBuf[inPtr++];
            if (left > 1) {
                outBuf[outPtr++] = (char) inBuf[inPtr++];
                if (left > 2) {
                    outBuf[outPtr++] = (char) inBuf[inPtr++];
                }
            }
        }
        */

	// meaning: regular tight loop is no slower, typically faster here:
	for (final int end = inPtr + len; inPtr < end; ++inPtr) {
            outBuf[outPtr++] = (char) inBuf[inPtr];            
        }
	
        _inputPtr = inPtr;
	_textBuffer.setCurrentLength(len);
    }

    protected final void _decodeShortUnicodeValue(int len)
        throws IOException, JsonParseException
    {
        if ((_inputEnd - _inputPtr) < len) {
	    _loadToHaveAtLeast(len);
	}
        int outPtr = 0;
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int inPtr = _inputPtr;
        _inputPtr += len;
        final int[] codes = SmileConstants.sUtf8UnitLengths;
        final byte[] inputBuf = _inputBuffer;
        for (int end = inPtr + len; inPtr < end; ) {
            int i = inputBuf[inPtr++] & 0xFF;
            int code = codes[i];
            if (code != 0) {
                // trickiest one, need surrogate handling
                switch (code) {
                case 1:
                    i = ((i & 0x1F) << 6) | (inputBuf[inPtr++] & 0x3F);
                    break;
	        case 2:
	            i = ((i & 0x0F) << 12)
	                  | ((inputBuf[inPtr++] & 0x3F) << 6)
	                  | (inputBuf[inPtr++] & 0x3F);
	            break;
	        case 3:
	            i = ((i & 0x07) << 18)
	                | ((inputBuf[inPtr++] & 0x3F) << 12)
	                | ((inputBuf[inPtr++] & 0x3F) << 6)
	                | (inputBuf[inPtr++] & 0x3F);
	            // note: this is the codepoint value; need to split, too
	            i -= 0x10000;
	            outBuf[outPtr++] = (char) (0xD800 | (i >> 10));
	            i = 0xDC00 | (i & 0x3FF);
	            break;
	        default: // invalid
	            _reportError("Invalid byte "+Integer.toHexString(i)+" in short Unicode text block");
                }
	    }
	    outBuf[outPtr++] = (char) i;
        }        
        _textBuffer.setCurrentLength(outPtr);
    }

    private final void _decodeLongAscii()
        throws IOException, JsonParseException
    {
        int outPtr = 0;
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        main_loop:
        while (true) {
            if (_inputPtr >= _inputEnd) {
                loadMoreGuaranteed();
            }
            int inPtr = _inputPtr;
            int left = _inputEnd - inPtr;
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            left = Math.min(left, outBuf.length - outPtr);
            do {
                byte b = _inputBuffer[inPtr++];
                if (b == SmileConstants.BYTE_MARKER_END_OF_STRING) {
                    _inputPtr = inPtr;
                    break main_loop;
                }
                outBuf[outPtr++] = (char) b;	    		
            } while (--left > 0);
            _inputPtr = inPtr;
        }
        _textBuffer.setCurrentLength(outPtr);
    }

    private final void _decodeLongUnicode()
        throws IOException, JsonParseException
    {
	int outPtr = 0;
	char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
	final int[] codes = SmileConstants.sUtf8UnitLengths;
        int c;
        final byte[] inputBuffer = _inputBuffer;

        main_loop:
        while (true) {
            // First the tight ASCII loop:
            ascii_loop:
            while (true) {
                int ptr = _inputPtr;
                if (ptr >= _inputEnd) {
                    loadMoreGuaranteed();
                    ptr = _inputPtr;
                }
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                int max = _inputEnd;
                {
                    int max2 = ptr + (outBuf.length - outPtr);
                    if (max2 < max) {
                        max = max2;
                    }
                }
                while (ptr < max) {
                    c = (int) inputBuffer[ptr++] & 0xFF;
                    if (codes[c] != 0) {
                        _inputPtr = ptr;
                        break ascii_loop;
                    }
                    outBuf[outPtr++] = (char) c;
                }
                _inputPtr = ptr;
            }
            // Ok: end marker, escape or multi-byte?
            if (c == SmileConstants.INT_MARKER_END_OF_STRING) {
                break main_loop;
            }

            switch (codes[c]) {
            case 1: // 2-byte UTF
                c = _decodeUtf8_2(c);
                break;
            case 2: // 3-byte UTF
                if ((_inputEnd - _inputPtr) >= 2) {
                    c = _decodeUtf8_3fast(c);
                } else {
                    c = _decodeUtf8_3(c);
                }
                break;
            case 4: // 4-byte UTF
                c = _decodeUtf8_4(c);
                // Let's add first part right away:
                outBuf[outPtr++] = (char) (0xD800 | (c >> 10));
                if (outPtr >= outBuf.length) {
                    outBuf = _textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                c = 0xDC00 | (c & 0x3FF);
                // And let the other char output down below
                break;
            default:
                // Is this good enough error message?
                _reportInvalidChar(c);
            }
            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = (char) c;
        }
        _textBuffer.setCurrentLength(outPtr);
    }

    private final void _finishRawBinary()
        throws IOException, JsonParseException
    {
        int byteLen = _readUnsignedVInt();
        _binaryValue = new byte[byteLen];
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        int ptr = 0;
        while (true) {
            int toAdd = Math.min(byteLen, _inputEnd - _inputPtr);
            System.arraycopy(_inputBuffer, _inputPtr, _binaryValue, ptr, toAdd);
            _inputPtr += toAdd;
            ptr += toAdd;
            byteLen -= toAdd;
            if (byteLen <= 0) {
                return;
            }
            loadMoreGuaranteed();
        }
    }

    /*
    /**********************************************************
    /* Internal methods, skipping
    /**********************************************************
     */

    /**
     * Method called to skip remainders of an incomplete token, when
     * contents themselves will not be needed any more
     */
    protected void _skipIncomplete() throws IOException, JsonParseException
    {
    	_tokenIncomplete = false;
    	int tb = _typeByte;
        switch ((tb >> 5) & 0x7) {
        case 1: // simple literals, numbers
            tb &= 0x1F;
            // next 3 bytes define subtype
            switch (tb >> 2) {
            case 1: // VInt (zigzag)
                // easy, just skip until we see sign bit... (should we try to limit damage?)
                switch (tb & 0x3) {
                case 1: // vlong
                        _skipBytes(4); // min 5 bytes
                        // fall through
                case 0: // vint
                    while (true) {
                        final int end = _inputEnd;
                        final byte[] buf = _inputBuffer;
                        while (_inputPtr < end) {
                                if (buf[_inputPtr++] < 0) {
                                        return;
                                }
                        }
                        loadMoreGuaranteed();                           
                    }
                case 2: // big-int
                    // just has binary data
                    _skip7BitBinary();
                    return;
                }
                break;
            case 2: // other numbers
                switch (tb & 0x3) {
                case 0: // float
                    _skipBytes(5);
                    return;
                case 1: // double
                    _skipBytes(10);
                    return;
                case 2: // big-decimal
                    // first, skip scale
                    _readUnsignedVInt();
                    // then length-prefixed binary serialization
                    _skip7BitBinary();
                    return;
                }
                break;
            }
            break;
        case 2: // tiny ASCII
            // fall through
        case 3: // short ASCII
            _skipBytes(1 + (tb & 0x3F));
            return;
        case 4: // tiny unicode
            // fall through
        case 5: // short unicode
            _skipBytes(2 + (tb & 0x3F));
            return;
        case 7:
            tb &= 0x1F;
            // next 3 bytes define subtype
            switch (tb >> 2) {
            case 0: // long variable length ASCII
            case 1: // long variable length unicode
            	/* Doesn't matter which one, just need to find the end marker
            	 * (note: can potentially skip invalid UTF-8 too)
            	 */
            	while (true) {
            	    final int end = _inputEnd;
            	    final byte[] buf = _inputBuffer;
            	    while (_inputPtr < end) {
            	        if (buf[_inputPtr++] == BYTE_MARKER_END_OF_STRING) {
            	            return;
            	        }
            	    }
            	    loadMoreGuaranteed();
            	}
            	// never gets here
            case 2: // binary, 7-bit
                _skip7BitBinary();
                return;
            case 7: // binary, raw
                _skipBytes(_readUnsignedVInt());
                return;
            }
        }
    	_throwInternal();
    }

    protected void _skipBytes(int len)
        throws IOException, JsonParseException
    {
        while (true) {
            int toAdd = Math.min(len, _inputEnd - _inputPtr);
            _inputPtr += toAdd;
            len -= toAdd;
            if (len <= 0) {
                return;
            }
            loadMoreGuaranteed();
        }
    }

    /**
     * Helper method for skipping length-prefixed binary data
     * section
     */
    protected void _skip7BitBinary()
        throws IOException, JsonParseException
    {
        int origBytes = _readUnsignedVInt();
        // Ok; 8 encoded bytes for 7 payload bytes first
        int chunks = origBytes / 7;
        int encBytes = chunks * 8;
        // and for last 0 - 6 bytes, last+1 (except none if no leftovers)
        origBytes -= 7 * chunks;
        if (origBytes > 0) {
            encBytes += 1 + origBytes;
        }
        _skipBytes(encBytes);
    }
    
    /*
    /**********************************************************
    /* Internal methods, UTF8 decoding
    /**********************************************************
     */

    private final int _decodeUtf8_2(int c)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        return ((c & 0x1F) << 6) | (d & 0x3F);
    }

    private final int _decodeUtf8_3(int c1)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        c1 &= 0x0F;
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        int c = (c1 << 6) | (d & 0x3F);
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = (c << 6) | (d & 0x3F);
        return c;
    }

    private final int _decodeUtf8_3fast(int c1)
        throws IOException, JsonParseException
    {
        c1 &= 0x0F;
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        int c = (c1 << 6) | (d & 0x3F);
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = (c << 6) | (d & 0x3F);
        return c;
    }

    /**
     * @return Character value <b>minus 0x10000</c>; this so that caller
     *    can readily expand it to actual surrogates
     */
    private final int _decodeUtf8_4(int c)
        throws IOException, JsonParseException
    {
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        int d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = ((c & 0x07) << 6) | (d & 0x3F);

        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }
        c = (c << 6) | (d & 0x3F);
        if (_inputPtr >= _inputEnd) {
            loadMoreGuaranteed();
        }
        d = (int) _inputBuffer[_inputPtr++];
        if ((d & 0xC0) != 0x080) {
            _reportInvalidOther(d & 0xFF, _inputPtr);
        }

        /* note: won't change it to negative here, since caller
         * already knows it'll need a surrogate
         */
        return ((c << 6) | (d & 0x3F)) - 0x10000;
    }
    
    /*
    /**********************************************************
    /* Internal methods, error reporting
    /**********************************************************
     */

    protected void _reportInvalidSharedName(int index) throws IOException
    {
        if (_seenNames == null) {
            _reportError("Encountered shared name reference, even though document header explicitly declared no shared name references are included");
        }
       _reportError("Invalid shared name reference "+index+"; only got "+_seenNameCount+" names in buffer (invalid content)");
    }

    protected void _reportInvalidSharedStringValue(int index) throws IOException
    {
        if (_seenStringValues == null) {
            _reportError("Encountered shared text value reference, even though document header did not declared shared text value references may be included");
        }
       _reportError("Invalid shared text value reference "+index+"; only got "+_seenStringValueCount+" names in buffer (invalid content)");
    }
    
    protected void _reportInvalidChar(int c) throws JsonParseException
    {
        // Either invalid WS or illegal UTF-8 start char
        if (c < ' ') {
            _throwInvalidSpace(c);
        }
        _reportInvalidInitial(c);
    }
	
    protected void _reportInvalidInitial(int mask)
        throws JsonParseException
    {
        _reportError("Invalid UTF-8 start byte 0x"+Integer.toHexString(mask));
    }
	
    protected void _reportInvalidOther(int mask)
        throws JsonParseException
    {
        _reportError("Invalid UTF-8 middle byte 0x"+Integer.toHexString(mask));
    }
	
    protected void _reportInvalidOther(int mask, int ptr)
        throws JsonParseException
    {
        _inputPtr = ptr;
        _reportInvalidOther(mask);
    }
}
    
