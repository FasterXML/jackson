package org.codehaus.jackson.smile;

import static org.codehaus.jackson.smile.SmileConstants.BYTE_MARKER_END_OF_STRING;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

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

    /**
     * Minimum number of shared names to buffer initially
     */
    private final static int MIN_SHARED_NAMES = 32;

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */
    
    /**
     * Codec used for data binding when (if) requested.
     */
    protected ObjectCodec _objectCodec;
    
    /*
    /**********************************************************
    /* Additional parsing state
    /**********************************************************
     */

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
     * by later fields
     */
    protected String[] _sharedNames = NO_STRINGS;

    protected int _sharedNameCount = 0;

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
    }

    public ObjectCodec getCodec() {
        return _objectCodec;
    }

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
	protected boolean handleSignature(boolean throwException)
		throws IOException, JsonParseException
	{
        if (++_inputPtr >= _inputEnd) {
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
        int versionBits = (ch >> 6) & 0x03;
        // but failure with version number is fatal, can not ignore
        if (versionBits != SmileConstants.HEADER_VERSION_00) {
        	_reportError("Header version number bits (0x"+Integer.toHexString(versionBits)+") indicate unrecognized version; only 0x0 handled by parser");
        }

        // can avoid tracking names, if explicitly disabled
        if ((ch & SmileConstants.HEADER_BIT_HAS_SHARED_NAMES) == 0) {
            _sharedNames = null;
        }
        /*
        if ((ch & SmileConstants.HEADER_BIT_HAS_SHARED_STRING_VALUES) == 0) {
        }
        */
        return true;
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

        // finally: clear any data retained so far
        _binaryValue = null;
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
        // Two main modes: values, and field names.
        if (_parsingContext.inObject() && _currToken != JsonToken.FIELD_NAME) {
            return (_currToken = _handleFieldName());
        }

        switch ((ch >> 5) & 0x7) {
        case 0: // short shared string value reference
        case 1: // misc literals
            switch (ch & 0x1F) {
            case 0x0: // START_OBJECT
                _parsingContext = _parsingContext.createChildObjectContext(_tokenInputRow, _tokenInputCol);
                return (_currToken = JsonToken.START_OBJECT);
            //case 0x0: // not used
            case 0x2: // START_ARRAY
                _parsingContext = _parsingContext.createChildArrayContext(_tokenInputRow, _tokenInputCol);
                return (_currToken = JsonToken.START_ARRAY);
            case 0x3: // END_ARRAY
                if (!_parsingContext.inArray()) {
                    _reportMismatchedEndMarker(']', '}');
                }
                _parsingContext = _parsingContext.getParent();
                return (_currToken = JsonToken.END_ARRAY);
            case 0x4: // false
                return (_currToken = JsonToken.VALUE_FALSE);
            case 0x5: // true
                return (_currToken = JsonToken.VALUE_TRUE);
            case 0x6:
                return (_currToken = JsonToken.VALUE_NULL);
            case 0x7:
                _textBuffer.resetWithEmpty();
                return (_currToken = JsonToken.VALUE_STRING);
            case 0x1A: // == 0x3A == ':' -> possibly header signature for next chunk?
            	if (handleSignature(false)) {
            		// Mark current token as empty, to return; but don't close input to allow more parsing
            		return (_currToken = null);
            	}
            	_reportError("Unrecognized token byte 0x3A (malformed segment header?");
            }
            // and everything else is reserved, for now
            break;
        case 2: // tiny ascii
            // fall through
        case 3: // short ascii
            // fall through
        case 4: // tiny unicode
            // fall through
        case 5: // short unicode
            // all fine; but no need to decode quite yet
            _tokenIncomplete = true;
            return (_currToken = JsonToken.VALUE_STRING);
        case 6: // small integers; zig zag encoded
            _numberInt = SmileUtil.zigzagDecode(ch & 0x1F);
            _numTypesValid = NR_INT;
            return (_currToken = JsonToken.VALUE_NUMBER_INT);
        default: // binary/longtext/var-numbers
            {
                int typeBits = ch & 0x1F;
                _tokenIncomplete = true; // none of these is fully handled yet
                // next 3 bytes define subtype
                switch (typeBits >> 2) {
                case 0: // long variable length ascii
                case 1: // long variable length unicode
                    _tokenIncomplete = true;
                    return (_currToken = JsonToken.VALUE_STRING);
                case 2: // binary, raw
                case 3: // binary, 7-bit
                    _tokenIncomplete = true;
                    return (_currToken = JsonToken.VALUE_EMBEDDED_OBJECT);
                case 4: // VInt (zigzag), BigInteger
                    if ((typeBits & 0x3) <= 0x2) { // 0x3 reserved (should never occur)
    	            _tokenIncomplete = true;
    	            _numTypesValid = 0;
    	            return (_currToken = JsonToken.VALUE_NUMBER_INT);
                    }
                    break;
                case 5: // floating-point
                    {
                    	int subtype = typeBits & 0x3;
    	                if (subtype <= 0x2) { // 0x3 reserved (should never occur)
    		                _tokenIncomplete = true;
    		                _numTypesValid = 0;
    		                _got32BitFloat = (subtype == 0);
    		                return (_currToken = JsonToken.VALUE_NUMBER_FLOAT);
    	                }
                    }
                    break;
                default: // 6 and 7 not used (to reserve 0xF8 - 0xFF)
                }
            }
            break;
        }
        // If we get this far, type byte is corrupt
        _reportError("Invalid type marker byte 0x"+Integer.toHexString(ch)+" for expected value token");
        return null;
    }

    @Override
    public String getCurrentName() throws IOException, JsonParseException
    {
        return _parsingContext.getCurrentName();
    }

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
        if (_currToken != null) { // null only before/after document
            if (_tokenIncomplete) {
                _finishToken();
            }
            switch (_currToken) {
            case VALUE_STRING:
                return _textBuffer.contentsAsString();
            case FIELD_NAME:
                return _parsingContext.getCurrentName();
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                // TODO: optimize
                return getNumberValue().toString();
                
            default:
                return _currToken.asString();
            }
        }
        return null;
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
    public int getTextOffset()
        throws IOException, JsonParseException
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
        int ch = _typeByte;
        switch ((ch >> 6) & 3) {
        case 0: // misc, including end marker
            switch (ch) {
            case 0x30: // long shared
            case 0x31:
            case 0x32:
            case 0x33:
                {
                    if (_inputPtr >= _inputEnd) {
                        loadMoreGuaranteed();
	            }
	            int index = ((ch & 0x3) << 8) + (_inputBuffer[_inputPtr++] & 0xFF);
                    if (index >= _sharedNameCount) {
                        _reportInvalidSharedName(index);
                    }
	            _parsingContext.setCurrentName(_sharedNames[index]);
	        }
                return JsonToken.FIELD_NAME;
            case 0x34: // long ascii/unicode name
                if (true) _throwInternal();
                return JsonToken.FIELD_NAME;            	
            case 0x35: // empty String as name, legal if unusual
                _parsingContext.setCurrentName("");
                return JsonToken.FIELD_NAME;
            case 0x36:
                if (!_parsingContext.inObject()) {
                    _reportMismatchedEndMarker('}', ']');
                }
                _parsingContext = _parsingContext.getParent();
                return JsonToken.END_OBJECT;

            }
            break;
        case 1: // short shared, can fully process
            {
                int index = (ch & 0x3F);
                if (index >= _sharedNameCount) {
                    _reportInvalidSharedName(index);
                }
                _parsingContext.setCurrentName(_sharedNames[index]);
            }
            return JsonToken.FIELD_NAME;
        case 2: // short ASCII
	    {
	        int len = (ch & 0x3f) + 1;
        	String name;
	        	Name n = _findDecodedFromSymbols(len);
	        	if (n != null) {
	        		name = n.getName();
	        		_inputPtr += len;
	        	} else {
	        		name = _decodeShortAsciiName(len);
	        		name = _addDecodedToSymbols(len, name);
	        	}
                if (_sharedNames != null) {
                   if (_sharedNameCount >= _sharedNames.length) {
   	               _sharedNames = _expandSharedNames(_sharedNames);
                   }
                   _sharedNames[_sharedNameCount++] = name;
                }
	            _parsingContext.setCurrentName(name);
	    }
	    return JsonToken.FIELD_NAME;                
        case 3: // short Unicode
            // all valid, except for 0xBF and 0xFF
            if ((ch & 0x3F) != 0x3F) {
                int len = (ch & 0x3f) + 1;
	        String name;
	        Name n = _findDecodedFromSymbols(len);
	        if (n != null) {
	        	name = n.getName();
	        	_inputPtr += len;
	        } else {
	        	name = _decodeShortUnicodeName(len);
	        	name = _addDecodedToSymbols(len, name);
	        }
                if (_sharedNames != null) {
                    if (_sharedNameCount >= _sharedNames.length) {
	                    _sharedNames = _expandSharedNames(_sharedNames);
                    }
                    _sharedNames[_sharedNameCount++] = name;
	             }
	            _parsingContext.setCurrentName(name);
	            return JsonToken.FIELD_NAME;                
            }
            break;
        }
        // Other byte values are illegal
        _reportError("Invalid type marker byte 0x"+Integer.toHexString(ch)+" for expected field name (or END_OBJECT marker)");
        return null;
    }

    private final static String[] _expandSharedNames(String[] oldShared)
    {
        int len = oldShared.length;
        String[] newShared;
        if (len == 0) {
        	newShared = new String[MIN_SHARED_NAMES];
        } else if (len == SmileConstants.MAX_SHARED_NAMES) { // too many? Just flush...
      	   newShared = oldShared;
        } else {
            int newSize = (len == MIN_SHARED_NAMES) ? 128 : SmileConstants.MAX_SHARED_NAMES;
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
        int outPtr = 0;
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int inPtr = _inputPtr;
        _inputPtr += len;
        for (int end = inPtr + len; inPtr < end; ) {
            outBuf[outPtr++] = (char) _inputBuffer[inPtr++];
        }
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
        for (int end = inPtr + len; inPtr < end; ) {
            int i = _inputBuffer[inPtr++] & 0xFF;
            int code = codes[i];
            if (code != 0) {
                // trickiest one, need surrogate handling
                switch (code) {
                case 1:
                    i = ((i & 0x1F) << 6) | (_inputBuffer[inPtr++] & 0x3F);
                    break;
                case 2:
                    i = ((i & 0x0F) << 12)
                        | ((_inputBuffer[inPtr++] & 0x3F) << 6)
                        | (_inputBuffer[inPtr++] & 0x3F);
                    break;
                case 3:
                    i = ((i & 0x07) << 18)
                    | ((_inputBuffer[inPtr++] & 0x3F) << 12)
                    | ((_inputBuffer[inPtr++] & 0x3F) << 6)
                    | (_inputBuffer[inPtr++] & 0x3F);
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

    /**
     * Helper method for trying to find specified encoded UTF-8 byte sequence
     * from symbol table; if succesfull avoids actual decoding to String
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
	    int q = inBuf[inPtr];
	    if (--len > 0) {
	        q = (q << 8) + inBuf[++inPtr];
	        if (--len > 0) {
	            q = (q << 8) + inBuf[++inPtr];
	            if (--len > 0) {
	                q = (q << 8) + inBuf[++inPtr];
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
            q1 += inBuf[inPtr++];
            q1 <<= 8;
            q1 += inBuf[inPtr++];
            q1 <<= 8;
            q1 += inBuf[inPtr++];
            int q2 = inBuf[inPtr++];
            len -= 5;
            if (len > 0) {
                q2 = (q2 << 8) + inBuf[inPtr++];				
                if (--len >= 0) {
                    q2 = (q2 << 8) + inBuf[inPtr++];				
                    if (--len >= 0) {
                        q2 = (q2 << 8) + inBuf[inPtr++];				
                    }
                }
            }
            _quad1 = q1;
            _quad2 = q2;
            return _symbols.findName(q1, q2);
        }
        return _findDecodedLong(len);
    }

    private final Name _findDecodedLong(int len)
        throws IOException, JsonParseException
    {
    	// first, need enough buffer to store bytes as ints:
    	{
			int bufLen = (len  + 3) >> 2;
	        if (bufLen > _quadBuffer.length) {
	            _quadBuffer = _growArrayTo(_quadBuffer, bufLen);
	        }
    	}
    	// then decode, full quads first
    	int offset = 0;
    	int inPtr = _inputPtr;
    	final byte[] inBuf = _inputBuffer;
        do {
			int q = inBuf[inPtr++] << 8;
			q |= inBuf[inPtr++];
			q <<= 8;
			q |= inBuf[inPtr++];
			q <<= 8;
			q |= inBuf[inPtr++];
			_quadBuffer[offset++] = q;
        } while ((len -= 4) > 3);
        // and then leftovers
        if (len > 0) {
			int q = inBuf[inPtr++];
			if (--len >= 0) {
				q = (q << 8) + inBuf[inPtr++];				
				if (--len >= 0) {
					q = (q << 8) + inBuf[inPtr++];				
				}
			}
			_quadBuffer[offset++] = q;
        }
        return _symbols.findName(_quadBuffer, offset);
	}
    
    public static int[] _growArrayTo(int[] arr, int minSize)
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
    	    _finishToken();
    	}
    }
    
    /**
     * Method called to finish parsing of a token so that token contents
     * are retrieable
     */
    protected void _finishToken()
    	throws IOException, JsonParseException
    {
        _tokenIncomplete = false;
    	int tb = _typeByte;

        switch ((tb >> 5) & 0x7) {
        case 2: // tiny ascii
            // fall through
        case 3: // short ascii
            _decodeShortAsciiValue(tb - 0x3F);
            return;

        case 4: // tiny unicode
            // fall through
        case 5: // short unicode; note, lengths 2 - 65  (off-by-one compared to ascii)
            _decodeShortUnicodeValue(tb - 0x7E);
            return;

        case 7:
            tb &= 0x1F;
            // next 3 bytes define subtype
            switch (tb >> 2) {
            case 0: // long variable length ascii
            	_decodeLongAscii();
            	return;
            case 1: // long variable length unicode
            	_decodeLongUnicode();
            	return;
            case 2: // binary, raw
                _finishRawBinary();
                return;
            case 3: // binary, 7-bit
                _binaryValue = _read7BitBinaryWithLength();
                return;
            case 4: // VInt (zigzag) or BigDecimal
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
            case 5: // other numbers
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
            	break;
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
        int outPtr = 0;
        // Note: we count on fact that buffer must have at least 'len' (<= 64) empty char slots
	char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
	int inPtr = _inputPtr;
	_inputPtr += len;
if (_inputPtr > _inputEnd) {
    throw new Error("Bad stuff; ptr now "+_inputPtr+"...");
}
	for (int end = inPtr + len; inPtr < end; ) {
	    outBuf[outPtr++] = (char) _inputBuffer[inPtr++];
	}
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
	    for (int end = inPtr + len; inPtr < end; ) {
	        int i = _inputBuffer[inPtr++] & 0xFF;
	        int code = codes[i];
	        if (code != 0) {
	            // trickiest one, need surrogate handling
	            switch (code) {
	            case 1:
	                i = ((i & 0x1F) << 6) | (_inputBuffer[inPtr++] & 0x3F);
	                break;
	            case 2:
	                i = ((i & 0x0F) << 12)
	                    | ((_inputBuffer[inPtr++] & 0x3F) << 6)
	                    | (_inputBuffer[inPtr++] & 0x3F);
	                break;
	            case 3:
	                i = ((i & 0x07) << 18)
	                | ((_inputBuffer[inPtr++] & 0x3F) << 12)
	                | ((_inputBuffer[inPtr++] & 0x3F) << 6)
	                | (_inputBuffer[inPtr++] & 0x3F);
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
            int left = _inputEnd - _inputPtr;
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            left = Math.min(left, outBuf.length - outPtr);
            do {
                byte b = _inputBuffer[_inputPtr++];
                if (b == SmileConstants.BYTE_MARKER_END_OF_STRING) {
                    break main_loop;
                }
                outBuf[outPtr++] = (char) b;	    		
            } while (--left > 0);
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
            // First the tight ascii loop:
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
        case 2: // tiny ascii
            // fall through
        case 3: // short ascii
            _skipBytes(tb - 0x3F);
            return;
        case 4: // tiny unicode
            // fall through
        case 5: // short unicode
            _skipBytes(tb - 0x7F);
            return;
        case 7:
            tb &= 0x1F;
            // next 3 bytes define subtype
            switch (tb >> 2) {
            case 0: // long variable length ascii
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

            	//
            case 2: // binary, raw
                _skipBytes(_readUnsignedVInt());
                return;
            case 3: // binary, 7-bit
                _skip7BitBinary();
                return;
            case 4: // VInt (zigzag)
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
            case 5: // other numbers
                switch (tb & 0x3) {
                case 0: // float
            	    _skipBytes(5);
            	    return;
            	case 1: // double
            	    _skipBytes(11);
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
        if (_sharedNames == null) {
            _reportError("Encountered shared name reference, even though document header explicitly declared no shared name references are included");
        }
       _reportError("Invalid shared name reference "+index+"; only got "+_sharedNameCount+" names in buffer (invalid content)");
    }
    
    protected void _reportInvalidChar(int c)
	    throws JsonParseException
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
