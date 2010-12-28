package org.codehaus.jackson.impl;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.io.NumberOutput;
import org.codehaus.jackson.io.SerializedString;
import org.codehaus.jackson.util.CharTypes;

public class Utf8Generator
    extends JsonGeneratorBase
{
    private final static byte BYTE_u = (byte) 'u';

    private final static byte BYTE_0 = (byte) '0';
    
    private final static byte BYTE_LBRACKET = (byte) '[';
    private final static byte BYTE_RBRACKET = (byte) ']';
    private final static byte BYTE_LCURLY = (byte) '{';
    private final static byte BYTE_RCURLY = (byte) '}';
 
    private final static byte BYTE_BACKSLASH = (byte) '\\';
    private final static byte BYTE_SPACE = (byte) ' ';
    private final static byte BYTE_COMMA = (byte) ',';
    private final static byte BYTE_COLON = (byte) ':';
    private final static byte BYTE_QUOTE = (byte) '"';

    protected final static int SURR1_FIRST = 0xD800;
    protected final static int SURR1_LAST = 0xDBFF;
    protected final static int SURR2_FIRST = 0xDC00;
    protected final static int SURR2_LAST = 0xDFFF;

    // intermediate copies only made up to certain length...
    private final static int MAX_BYTES_TO_BUFFER = 512;
    
    final static byte[] HEX_CHARS = CharTypes.copyHexBytes();

    private final static byte[] NULL_BYTES = { 'n', 'u', 'l', 'l' };
    private final static byte[] TRUE_BYTES = { 't', 'r', 'u', 'e' };
    private final static byte[] FALSE_BYTES = { 'f', 'a', 'l', 's', 'e' };

    private final static int[] sOutputEscapes = CharTypes.getOutputEscapes();
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected IOContext _ioContext;

    final protected OutputStream _outputStream;
    
    /*
    /**********************************************************
    /* Output buffering
    /**********************************************************
     */

    /**
     * Intermediate buffer in which contents are buffered before
     * being written using {@link #_outputStream}.
     */
    protected byte[] _outputBuffer;

    /**
     * Pointer to the position right beyond the last character to output
     * (end marker; may be past the buffer)
     */
    protected int _outputTail = 0;

    /**
     * End marker of the output buffer; one past the last valid position
     * within the buffer.
     */
    protected final int _outputEnd;

    /**
     * Maximum number of <code>char</code>s that we know will always fit
     * in the output buffer after escaping
     */
    protected final int _outputMaxContiguous;
    
    /**
     * Intermediate buffer in which characters of a String are copied
     * before being encoded.
     */
    protected char[] _charBuffer;
    
    /**
     * Length of <code>_charBuffer</code>
     */
    protected final int _charBufferLength;
    
    /**
     * 6 character temporary buffer allocated if needed, for constructing
     * escape sequences
     */
    protected byte[] _entityBuffer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public Utf8Generator(IOContext ctxt, int features, ObjectCodec codec,
            OutputStream out)
    {
        
        super(features, codec);
        _ioContext = ctxt;
        _outputStream = out;
        _outputBuffer = ctxt.allocWriteEncodingBuffer();
        _outputEnd = _outputBuffer.length;
        /* To be exact, each char can take up to 6 bytes when escaped (Unicode
         * escape with bacslash, 'u' and 4 hex digits); but to avoid fluctuation,
         * we will actually round down to only do up to 1/8 number of chars
         */
        _outputMaxContiguous = _outputEnd >> 3;
        _charBuffer = ctxt.allocConcatBuffer();
        _charBufferLength = _charBuffer.length;
    }

    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */
    
    @Override
    public final void writeFieldName(SerializedString name)
        throws IOException, JsonGenerationException
    {
        // Object is a value, need to verify it's allowed
        int status = _writeContext.writeFieldName(name.getValue());
        if (status == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name, (status == JsonWriteContext.STATUS_OK_AFTER_COMMA));
    }

    @Override
    public final void writeFieldName(SerializableString name)
        throws IOException, JsonGenerationException
    {
        // Object is a value, need to verify it's allowed
        int status = _writeContext.writeFieldName(name.getValue());
        if (status == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name, (status == JsonWriteContext.STATUS_OK_AFTER_COMMA));
    }

    /*
    /**********************************************************
    /* Output method implementations, structural
    /**********************************************************
     */

    @Override
    protected void _writeStartArray()
        throws IOException, JsonGenerationException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_LBRACKET;
    }

    @Override
    protected void _writeEndArray()
        throws IOException, JsonGenerationException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_RBRACKET;
    }

    @Override
    protected void _writeStartObject()
        throws IOException, JsonGenerationException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_LCURLY;
    }

    @Override
    protected void _writeEndObject()
        throws IOException, JsonGenerationException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_RCURLY;
    }

    @Override
    protected void _writeFieldName(String name, boolean commaBefore)
        throws IOException, JsonGenerationException
    {
        if (_cfgPrettyPrinter != null) {
            _writePPFieldName(name, commaBefore);
            return;
        }
        // for fast+std case, need to output up to 2 chars, comma, dquote
        if ((_outputTail + 1) >= _outputEnd) {
            _flushBuffer();
        }
        if (commaBefore) {
            _outputBuffer[_outputTail++] = BYTE_COMMA;
        }

        /* To support [JACKSON-46], we'll do this:
         * (Question: should quoting of spaces (etc) still be enabled?)
         */
        if (!isEnabled(Feature.QUOTE_FIELD_NAMES)) {
            _writeStringSegments(name);
            return;
        }

        // we know there's room for at least one more char
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        // The beef:
        final int len = name.length();
        if (len <= _charBufferLength) { // yes, fits right in
            name.getChars(0, len, _charBuffer, 0);
            // But as one segment, or multiple?
            if (len <= _outputMaxContiguous) {
                _writeStringSegment(_charBuffer, 0, len);
            } else {
                _writeStringSegments(_charBuffer, 0, len);
            }
        } else {
            _writeStringSegments(name);
        }

        // and closing quotes; need room for one more char:
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
    }

    protected void _writeFieldName(SerializableString name, boolean commaBefore)
        throws IOException, JsonGenerationException
    {
        if (_cfgPrettyPrinter != null) {
            _writePPFieldName(name, commaBefore);
            return;
        }
        if ((_outputTail + 1) >= _outputEnd) {
            _flushBuffer();
        }
        if (commaBefore) {
            _outputBuffer[_outputTail++] = BYTE_COMMA;
        }
        byte[] raw = name.asQuotedUTF8();
        if (!isEnabled(Feature.QUOTE_FIELD_NAMES)) {
            _writeBytes(raw);
            return;
        }

        // we know there's room for at least one more char
        _outputBuffer[_outputTail++] = BYTE_QUOTE;

        // Can do it all in buffer?
        final int len = raw.length;
        if ((_outputTail + len + 1) < _outputEnd) { // yup
            System.arraycopy(raw, 0, _outputBuffer, _outputTail, len);
            _outputTail += len;
            _outputBuffer[_outputTail++] = BYTE_QUOTE;
        } else {
            _writeBytes(raw);
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            _outputBuffer[_outputTail++] = BYTE_QUOTE;
        }
    }    
    
    /**
     * Specialized version of <code>_writeFieldName</code>, off-lined
     * to keep the "fast path" as simple (and hopefully fast) as possible.
     */
    protected final void _writePPFieldName(String name, boolean commaBefore)
        throws IOException, JsonGenerationException
    {
        if (commaBefore) {
            _cfgPrettyPrinter.writeObjectEntrySeparator(this);
        } else {
            _cfgPrettyPrinter.beforeObjectEntries(this);
        }

        if (isEnabled(Feature.QUOTE_FIELD_NAMES)) { // standard
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            _outputBuffer[_outputTail++] = BYTE_QUOTE;
            final int len = name.length();
            if (len <= _charBufferLength) { // yes, fits right in
                name.getChars(0, len, _charBuffer, 0);
                // But as one segment, or multiple?
                if (len <= _outputMaxContiguous) {
                    _writeStringSegment(_charBuffer, 0, len);
                } else {
                    _writeStringSegments(_charBuffer, 0, len);
                }
            } else {
                _writeStringSegments(name);
            }
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            _outputBuffer[_outputTail++] = BYTE_QUOTE;
        } else { // non-standard, omit quotes
            _writeStringSegments(name);
        }
    }

    protected final void _writePPFieldName(SerializableString name, boolean commaBefore)
        throws IOException, JsonGenerationException
    {
        if (commaBefore) {
            _cfgPrettyPrinter.writeObjectEntrySeparator(this);
        } else {
            _cfgPrettyPrinter.beforeObjectEntries(this);
        }

        boolean addQuotes = isEnabled(Feature.QUOTE_FIELD_NAMES); // standard
        if (addQuotes) {
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            _outputBuffer[_outputTail++] = BYTE_QUOTE;
        }
        _writeBytes(name.asQuotedUTF8());
        if (addQuotes) {
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            _outputBuffer[_outputTail++] = BYTE_QUOTE;
        }
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write text value");
        if (text == null) {
            _writeNull();
            return;
        }
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        final int len = text.length();
        if (len <= _charBufferLength) { // yes, fits right in
            text.getChars(0, len, _charBuffer, 0);
            // But as one segment, or multiple?
            if (len <= _outputMaxContiguous) {
                _writeStringSegment(_charBuffer, 0, len);
            } else {
                _writeStringSegments(_charBuffer, 0, len);
            }
        } else {
            _writeStringSegments(text);
        }
        // And finally, closing quotes
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
    }

    @Override
    public void writeString(char[] text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write text value");
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        // One or multiple segments?
        if (len <= _outputMaxContiguous) {
            _writeStringSegment(text, offset, len);
        } else {
            _writeStringSegments(text, offset, len);
        }
        // And finally, closing quotes
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
    }

    @Override
    public final void writeString(SerializableString text)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write text value");
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        _writeBytes(text.asQuotedUTF8());
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write text value");
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        _writeBytes(text, offset, length);
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length)
        throws IOException, JsonGenerationException
    {
        // @TODO
        _reportUnsupportedOperation();
    }
    
    /*
    /**********************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************
     */

    @Override
    public void writeRaw(String text)
        throws IOException, JsonGenerationException
    {
        int start = 0;
        int len = text.length();
        while (len > 0) {
            char[] buf = _charBuffer;
            final int blen = buf.length;
            final int len2 = (len < blen) ? len : blen;
            text.getChars(start, start+len2, buf, 0);
            writeRaw(buf, 0, len2);
            start += len2;
            len -= len2;
        }
    }

    @Override
    public void writeRaw(String text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        while (len > 0) {
            char[] buf = _charBuffer;
            final int blen = buf.length;
            final int len2 = (len < blen) ? len : blen;
            text.getChars(offset, offset+len2, buf, 0);
            writeRaw(buf, 0, len2);
            offset += len2;
            len -= len2;
        }
    }

    // @TODO: rewrite for speed...
    @Override
    public final void writeRaw(char[] cbuf, int offset, int len)
        throws IOException, JsonGenerationException
    {
        // First: if we have 3 x charCount spaces, we know it'll fit just fine
        {
            int len3 = len+len+len;
            if ((_outputTail + len3) > _outputEnd) {
                // maybe we could flush?
                if (_outputEnd < len3) { // wouldn't be enough...
                    _writeSegmentedRaw(cbuf, offset, len);
                    return;
                }
                // yes, flushing brings enough space
                _flushBuffer();
            }
        }
        len += offset; // now marks the end

        // Note: here we know there is enough room, hence no output boundary checks
        main_loop:
        while (offset < len) {
            inner_loop:
            while (true) {
                int ch = (int) cbuf[offset];
                if (ch > 0x7F) {
                    break inner_loop;
                }
                _outputBuffer[_outputTail++] = (byte) ch;
                if (++offset >= len) {
                    break main_loop;
                }
            }
            char ch = cbuf[offset++];
            if (ch < 0x800) { // 2-byte?
                _outputBuffer[_outputTail++] = (byte) (0xc0 | (ch >> 6));
                _outputBuffer[_outputTail++] = (byte) (0x80 | (ch & 0x3f));
            } else {
                _outputRawMultiByteChar(ch, cbuf, offset, len);
            }
        }
    }

    @Override
    public void writeRaw(char ch)
        throws IOException, JsonGenerationException
    {
        if ((_outputTail + 3) >= _outputEnd) {
            _flushBuffer();
        }
        final byte[] bbuf = _outputBuffer;
        if (ch <= 0x7F) {
            bbuf[_outputTail++] = (byte) ch;
        } else  if (ch < 0x800) { // 2-byte?
            bbuf[_outputTail++] = (byte) (0xc0 | (ch >> 6));
            bbuf[_outputTail++] = (byte) (0x80 | (ch & 0x3f));
        } else {
            _outputRawMultiByteChar(ch, null, 0, 0);
        }
    }

    /**
     * Helper method called when it is possible that output of raw section
     * to output may cross buffer boundary
     */
    private final void _writeSegmentedRaw(char[] cbuf, int offset, int len)
        throws IOException, JsonGenerationException
    {
        final int end = _outputEnd;
        final byte[] bbuf = _outputBuffer;
        
        main_loop:
        while (offset < len) {
            inner_loop:
            while (true) {
                int ch = (int) cbuf[offset];
                if (ch >= 0x80) {
                    break inner_loop;
                }
                // !!! TODO: fast(er) writes (roll input, output checks in one)
                if (_outputTail >= end) {
                    _flushBuffer();
                }
                bbuf[_outputTail++] = (byte) ch;
                if (++offset >= len) {
                    break main_loop;
                }
            }
            if ((_outputTail + 3) >= _outputEnd) {
                _flushBuffer();
            }
            char ch = cbuf[offset++];
            if (ch < 0x800) { // 2-byte?
                bbuf[_outputTail++] = (byte) (0xc0 | (ch >> 6));
                bbuf[_outputTail++] = (byte) (0x80 | (ch & 0x3f));
            } else {
                _outputRawMultiByteChar(ch, cbuf, offset, len);
            }
        }
    }
    
    /*
    /**********************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write binary value");
        // Starting quotes
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        _writeBinary(b64variant, data, offset, offset+len);
        // and closing quotes
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
    }
    
    /*
    /**********************************************************
    /* Output method implementations, primitive
    /**********************************************************
     */

    @Override
    public void writeNumber(int i)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        // up to 10 digits and possible minus sign
        if ((_outputTail + 11) >= _outputEnd) {
            _flushBuffer();
        }
        if (_cfgNumbersAsStrings) {
            _writeQuotedInt(i);
            return;
        }
        _outputTail = NumberOutput.outputInt(i, _outputBuffer, _outputTail);
    }

    private final void _writeQuotedInt(int i) throws IOException {
        if ((_outputTail + 13) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        _outputTail = NumberOutput.outputInt(i, _outputBuffer, _outputTail);
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
    }    

    @Override
    public void writeNumber(long l)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        if (_cfgNumbersAsStrings) {
            _writeQuotedLong(l);
            return;
        }
        if ((_outputTail + 21) >= _outputEnd) {
            // up to 20 digits, minus sign
            _flushBuffer();
        }
        _outputTail = NumberOutput.outputLong(l, _outputBuffer, _outputTail);
    }

    private final void _writeQuotedLong(long l) throws IOException {
        if ((_outputTail + 23) >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        _outputTail = NumberOutput.outputLong(l, _outputBuffer, _outputTail);
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
    }

    @Override
    public void writeNumber(BigInteger value)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        if (value == null) {
            _writeNull();
        } else if (_cfgNumbersAsStrings) {
            _writeQuotedRaw(value);
        } else {
            writeRaw(value.toString());
        }
    }

    
    @Override
    public void writeNumber(double d)
        throws IOException, JsonGenerationException
    {
        if (_cfgNumbersAsStrings ||
            // [JACKSON-139]
            (((Double.isNaN(d) || Double.isInfinite(d))
                && isEnabled(Feature.QUOTE_NON_NUMERIC_NUMBERS)))) {
            writeString(String.valueOf(d));
            return;
        }
        // What is the max length for doubles? 40 chars?
        _verifyValueWrite("write number");
        writeRaw(String.valueOf(d));
    }

    @Override
    public void writeNumber(float f)
        throws IOException, JsonGenerationException
    {
        if (_cfgNumbersAsStrings ||
            // [JACKSON-139]
            (((Float.isNaN(f) || Float.isInfinite(f))
                && isEnabled(Feature.QUOTE_NON_NUMERIC_NUMBERS)))) {
            writeString(String.valueOf(f));
            return;
        }
        // What is the max length for floats?
        _verifyValueWrite("write number");
        writeRaw(String.valueOf(f));
    }

    @Override
    public void writeNumber(BigDecimal value)
        throws IOException, JsonGenerationException
    {
        // Don't really know max length for big decimal, no point checking
        _verifyValueWrite("write number");
        if (value == null) {
            _writeNull();
        } else if (_cfgNumbersAsStrings) {
            _writeQuotedRaw(value);
        } else {
            writeRaw(value.toString());
        }
    }

    @Override
    public void writeNumber(String encodedValue)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        if (_cfgNumbersAsStrings) {
            _writeQuotedRaw(encodedValue);            
        } else {
            writeRaw(encodedValue);
        }
    }

    private final void _writeQuotedRaw(Object value) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        writeRaw(value.toString());
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
    }
    
    @Override
    public void writeBoolean(boolean state)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write boolean value");
        if ((_outputTail + 5) >= _outputEnd) {
            _flushBuffer();
        }
        byte[] keyword = state ? TRUE_BYTES : FALSE_BYTES;
        int len = keyword.length;
        System.arraycopy(keyword, 0, _outputBuffer, _outputTail, len);
        _outputTail += len;
    }

    @Override
    public void writeNull()
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write null value");
        _writeNull();
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
        if (_cfgPrettyPrinter == null) {
            byte b;
            switch (status) {
            case JsonWriteContext.STATUS_OK_AFTER_COMMA:
                b = BYTE_COMMA;
                break;
            case JsonWriteContext.STATUS_OK_AFTER_COLON:
                b = BYTE_COLON;
                break;
            case JsonWriteContext.STATUS_OK_AFTER_SPACE:
                b = BYTE_SPACE;
                break;
            case JsonWriteContext.STATUS_OK_AS_IS:
            default:
                return;
            }
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            _outputBuffer[_outputTail] = b;
            ++_outputTail;
            return;
        }
        // Otherwise, pretty printer knows what to do...
        _verifyPrettyValueWrite(typeMsg, status);
    }

    protected final void _verifyPrettyValueWrite(String typeMsg, int status)
        throws IOException, JsonGenerationException
    {
        // If we have a pretty printer, it knows what to do:
        switch (status) {
        case JsonWriteContext.STATUS_OK_AFTER_COMMA: // array
            _cfgPrettyPrinter.writeArrayValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AFTER_COLON:
            _cfgPrettyPrinter.writeObjectFieldValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AFTER_SPACE:
            _cfgPrettyPrinter.writeRootValueSeparator(this);
            break;
        case JsonWriteContext.STATUS_OK_AS_IS:
            // First entry, but of which context?
            if (_writeContext.inArray()) {
                _cfgPrettyPrinter.beforeArrayValues(this);
            } else if (_writeContext.inObject()) {
                _cfgPrettyPrinter.beforeObjectEntries(this);
            }
            break;
        default:
            _cantHappen();
            break;
        }
    }

    /*
    /**********************************************************
    /* Low-level output handling
    /**********************************************************
     */

    @Override
    public final void flush()
        throws IOException
    {
        _flushBuffer();
        if (_outputStream != null) {
            if (isEnabled(Feature.FLUSH_PASSED_TO_STREAM)) {
                _outputStream.flush();
            }
        }
    }

    @Override
    public void close()
        throws IOException
    {
        super.close();

        /* 05-Dec-2008, tatu: To add [JACKSON-27], need to close open
         *   scopes.
         */
        // First: let's see that we still have buffers...
        if (_outputBuffer != null
            && isEnabled(Feature.AUTO_CLOSE_JSON_CONTENT)) {
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
        _flushBuffer();

        /* 25-Nov-2008, tatus: As per [JACKSON-16] we are not to call close()
         *   on the underlying Reader, unless we "own" it, or auto-closing
         *   feature is enabled.
         *   One downside: when using UTF8Writer, underlying buffer(s)
         *   may not be properly recycled if we don't close the writer.
         */
        if (_ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_TARGET)) {
            _outputStream.close();
        } else {
            // If we can't close it, we should at least flush
            _outputStream.flush();
        }
        // Internal buffer(s) generator has can now be released as well
        _releaseBuffers();
    }

    @Override
    protected void _releaseBuffers()
    {
        byte[] buf = _outputBuffer;
        if (buf != null) {
            _outputBuffer = null;
            _ioContext.releaseWriteEncodingBuffer(buf);
        }
        char[] cbuf = _charBuffer;
        if (cbuf != null) {
            _charBuffer = null;
            _ioContext.releaseConcatBuffer(cbuf);
        }
    }

    /*
    /**********************************************************
    /* Internal methods, low-level writing
    /**********************************************************
     */

    private final void _writeBytes(byte[] bytes) throws IOException
    {
        final int len = bytes.length;
        if ((_outputTail + len) > _outputEnd) {
            _flushBuffer();
            // still not enough?
            if (len > MAX_BYTES_TO_BUFFER) {
                _outputStream.write(bytes, 0, len);
                return;
            }
        }
        System.arraycopy(bytes, 0, _outputBuffer, _outputTail, len);
        _outputTail += len;
    }

    private final void _writeBytes(byte[] bytes, int offset, int len) throws IOException
    {
        if ((_outputTail + len) > _outputEnd) {
            _flushBuffer();
            // still not enough?
            if (len > MAX_BYTES_TO_BUFFER) {
                _outputStream.write(bytes, offset, len);
                return;
            }
        }
        System.arraycopy(bytes, offset, _outputBuffer, _outputTail, len);
        _outputTail += len;
    }

    /**
     * Method called when String to write is long enough not to fit
     * completely in temporary copy buffer. If so, we will actually
     * copy it in small enough chunks so it can be directly fed
     * to single-segment writes (instead of maximum slices that
     * would fit in copy buffer)
     */
    private final void _writeStringSegments(String text)
        throws IOException, JsonGenerationException
    {
        int left = text.length();
        int offset = 0;
        final char[] cbuf = _charBuffer;

        while (left > 0) {
            int len = Math.min(_outputMaxContiguous, left);
            text.getChars(offset, offset+len, cbuf, 0);
            _writeStringSegment(cbuf, 0, len);
            offset += len;
            left -= len;
        }
    }

    /**
     * Method called when character sequence to write is long enough that
     * its maximum encoded and escaped form is not guaranteed to fit in
     * the output buffer. If so, we will need to choose smaller output
     * chunks to write at a time.
     */
    private final void _writeStringSegments(char[] cbuf, int offset, int totalLen)
        throws IOException, JsonGenerationException
    {
        do {
            int len = Math.min(_outputMaxContiguous, totalLen);
            _writeStringSegment(cbuf, offset, len);
            offset += len;
            totalLen -= len;
        } while (totalLen > 0);
    }

    /**
     * This method called when the string content is already in
     * a char buffer, and its maximum total encoded and escaped length
     * can not exceed size of the output buffer.
     */
    private final void _writeStringSegment(char[] cbuf, int offset, int len)
        throws IOException, JsonGenerationException
    {
        int outputPtr = _outputTail;
        // First: assuming ASCII-only, do we have enough room?
        if ((outputPtr + len) > _outputEnd) { // not yet
            _flushBuffer(); // but yes once we flush (caller guarantees length restriction)
            outputPtr = _outputTail;
        }

        /* And then fast+tight loop for ASCII-only, no-escaping-needed
         * output.
         */
        len += offset; // becomes end marker, then

        final int[] escCodes = sOutputEscapes;
        final byte[] outputBuffer = _outputBuffer;

        while (offset < len) {
            int ch = cbuf[offset];
            if (ch > 0x7F || escCodes[ch] != 0) {
                _outputTail = outputPtr;
                _writeStringSegment2(cbuf, offset, len, outputPtr);
                return;
            }
            outputBuffer[outputPtr++] = (byte) ch;
            ++offset;
        }
        _outputTail = outputPtr;
    }

    /**
     * Secondary method called when content contains characters to escape,
     * and/or multi-byte UTF-8 characters.
     */
    private final void _writeStringSegment2(final char[] cbuf, int offset, final int end,
            int outputPtr)
        throws IOException, JsonGenerationException
    {
        // Ok: caller guarantees buffer can have room; but that may require flushing:
        {
            int maxLen = 6 * ((end - offset) + 1);
            if ((outputPtr + maxLen) > _outputEnd) {
                _flushBuffer();
            }
            outputPtr = _outputTail;
        }

        final byte[] outputBuffer = _outputBuffer;
        final int[] escCodes = sOutputEscapes;
        
        while (offset < end) {
            int ch = cbuf[offset++];
            if (ch <= 0x7F) {
                 if (escCodes[ch] == 0) {
                     outputBuffer[outputPtr++] = (byte) ch;
                     continue;
                 }
                int escape = sOutputEscapes[ch];
                if (escape > 0) { // 2-char escape, fine
                    outputBuffer[outputPtr++] = BYTE_BACKSLASH;
                    outputBuffer[outputPtr++] = (byte) escape;
                } else {
                    // ctrl-char, 6-byte escape...
                    outputPtr = _writeEscapedControlChar(escape, outputPtr);
                }
                continue;
            }
            if (ch <= 0x7FF) { // fine, just needs 2 byte output
                outputBuffer[outputPtr++] = (byte) (0xc0 | (ch >> 6));
                outputBuffer[outputPtr++] = (byte) (0x80 | (ch & 0x3f));
            } else {
                outputPtr = _outputMultiByteChar(ch, outputPtr);
            }
        }
        _outputTail = outputPtr;
    }

    protected void _writeBinary(Base64Variant b64variant, byte[] input, int inputPtr, final int inputEnd)
        throws IOException, JsonGenerationException
    {
        // Encoding is by chunks of 3 input, 4 output chars, so:
        int safeInputEnd = inputEnd - 3;
        // Let's also reserve room for possible (and quoted) lf char each round
        int safeOutputEnd = _outputEnd - 6;
        int chunksBeforeLF = b64variant.getMaxLineLength() >> 2;

        // Ok, first we loop through all full triplets of data:
        while (inputPtr <= safeInputEnd) {
            if (_outputTail > safeOutputEnd) { // need to flush
                _flushBuffer();
            }
            // First, mash 3 bytes into lsb of 32-bit int
            int b24 = ((int) input[inputPtr++]) << 8;
            b24 |= ((int) input[inputPtr++]) & 0xFF;
            b24 = (b24 << 8) | (((int) input[inputPtr++]) & 0xFF);
            _outputTail = b64variant.encodeBase64Chunk(b24, _outputBuffer, _outputTail);
            if (--chunksBeforeLF <= 0) {
                // note: must quote in JSON value
                _outputBuffer[_outputTail++] = '\\';
                _outputBuffer[_outputTail++] = 'n';
                chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
            }
        }

        // And then we may have 1 or 2 leftover bytes to encode
        int inputLeft = inputEnd - inputPtr; // 0, 1 or 2
        if (inputLeft > 0) { // yes, but do we have room for output?
            if (_outputTail > safeOutputEnd) { // don't really need 6 bytes but...
                _flushBuffer();
            }
            int b24 = ((int) input[inputPtr++]) << 16;
            if (inputLeft == 2) {
                b24 |= (((int) input[inputPtr++]) & 0xFF) << 8;
            }
            _outputTail = b64variant.encodeBase64Partial(b24, inputLeft, _outputBuffer, _outputTail);
        }
    }

    /**
     * Method called to output a character that is beyond range of
     * 1- and 2-byte UTF-8 encodings, when outputting "raw" 
     * text (meaning it is not to be escaped or quoted)
     */
    private final int _outputRawMultiByteChar(int ch, char[] cbuf, int inputOffset, int inputLen)
        throws IOException
    {
        // Let's handle surrogates gracefully (as 4 byte output):
        if (ch >= SURR1_FIRST) {
            if (ch <= SURR2_LAST) { // yes, outside of BMP
                // Do we have second part?
                if (inputOffset >= inputLen) { // nope... have to note down
                    _reportError("Split surrogate on writeRaw() input (last character)");
                }
                _outputSurrogates(ch, cbuf[inputOffset]);
                return (inputOffset+1);
            }
        }
        final byte[] bbuf = _outputBuffer;
        bbuf[_outputTail++] = (byte) (0xe0 | (ch >> 12));
        bbuf[_outputTail++] = (byte) (0x80 | ((ch >> 6) & 0x3f));
        bbuf[_outputTail++] = (byte) (0x80 | (ch & 0x3f));
        return inputOffset;
    }

    protected final void _outputSurrogates(int surr1, int surr2)
        throws IOException
    {
        int c = _decodeSurrogate(surr1, surr2);
        if ((_outputTail + 4) > _outputEnd) {
            _flushBuffer();
        }
        final byte[] bbuf = _outputBuffer;
        bbuf[_outputTail++] = (byte) (0xf0 | (c >> 18));
        bbuf[_outputTail++] = (byte) (0x80 | ((c >> 12) & 0x3f));
        bbuf[_outputTail++] = (byte) (0x80 | ((c >> 6) & 0x3f));
        bbuf[_outputTail++] = (byte) (0x80 | (c & 0x3f));
    }
    
    /**
     * 
     * @param ch
     * @param outputPtr Position within output buffer to append multi-byte in
     * 
     * @return New output position after appending
     * 
     * @throws IOException
     */
    private final int _outputMultiByteChar(int ch, int outputPtr)
        throws IOException
    {
        byte[] bbuf = _outputBuffer;
        if (ch >= SURR1_FIRST && ch <= SURR2_LAST) { // yes, outside of BMP; add an escape
            bbuf[outputPtr++] = BYTE_BACKSLASH;
            bbuf[outputPtr++] = BYTE_u;
            
            bbuf[outputPtr++] = HEX_CHARS[(ch >> 12) & 0xF];
            bbuf[outputPtr++] = HEX_CHARS[(ch >> 8) & 0xF];
            bbuf[outputPtr++] = HEX_CHARS[(ch >> 4) & 0xF];
            bbuf[outputPtr++] = HEX_CHARS[ch & 0xF];
        } else {
            bbuf[outputPtr++] = (byte) (0xe0 | (ch >> 12));
            bbuf[outputPtr++] = (byte) (0x80 | ((ch >> 6) & 0x3f));
            bbuf[outputPtr++] = (byte) (0x80 | (ch & 0x3f));
        }
        return outputPtr;
    }

    protected final int _decodeSurrogate(int surr1, int surr2) throws IOException
    {
        // First is known to be valid, but how about the other?
        if (surr2 < SURR2_FIRST || surr2 > SURR2_LAST) {
            String msg = "Incomplete surrogate pair: first char 0x"+Integer.toHexString(surr1)+", second 0x"+Integer.toHexString(surr2);
            _reportError(msg);
        }
        int c = 0x10000 + ((surr1 - SURR1_FIRST) << 10) + (surr2 - SURR2_FIRST);
        return c;
    }
    
    private final void _writeNull() throws IOException
    {
        if ((_outputTail + 4) >= _outputEnd) {
            _flushBuffer();
        }
        System.arraycopy(NULL_BYTES, 0, _outputBuffer, _outputTail, 4);
        _outputTail += 4;
    }
        
    /**
     * @param escCode Character code for escape sequence (\C); or -1
     *   to indicate a generic (\\uXXXX) sequence.
     */
    private int _writeEscapedControlChar(int escCode, int outputPtr)
        throws IOException
    {
        if ((outputPtr + 6) >= _outputEnd) {
            _outputTail = outputPtr;
            _flushBuffer();
            outputPtr = _outputTail;
        }
        final byte[] bbuf = _outputBuffer;
        bbuf[outputPtr++] = BYTE_BACKSLASH;
        int value = -(escCode + 1);
        bbuf[outputPtr++] = BYTE_u;
        bbuf[outputPtr++] = BYTE_0;
        bbuf[outputPtr++] = BYTE_0;
        // We know it's a control char, so only the last 2 chars are non-0
        bbuf[outputPtr++] = HEX_CHARS[value >> 4];
        bbuf[outputPtr++] = HEX_CHARS[value & 0xF];
        return outputPtr;
    }

    protected final void _flushBuffer() throws IOException
    {
        int len = _outputTail;
        if (len > 0) {
            _outputTail = 0;
            _outputStream.write(_outputBuffer, 0, len);
        }
    }
}
