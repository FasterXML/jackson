package org.codehaus.jackson.impl;

import java.io.*;
import java.math.BigDecimal;

import org.codehaus.jackson.*;
import org.codehaus.jackson.JsonGenerator.Feature;

/**
 * This base class implements part of API that a JSON generator exposes
 * to applications, adds shared internal methods that sub-classes
 * can use and adds some abstract methods sub-classes must implement.
 */
public abstract class JsonGeneratorBase
    extends JsonGenerator
{
    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    protected ObjectCodec _objectCodec;

    /**
     * Bit flag composed of bits that indicate which
     * {@link org.codehaus.jackson.JsonGenerator.Feature}s
     * are enabled.
     */
    protected int _features;

    /**
     * Flag set to indicate that implicit conversion from number
     * to JSON String is needed (as per {@link Feature#WRITE_NUMBERS_AS_STRINGS}).
     */
    protected boolean _cfgNumbersAsStrings;

    /*
    ////////////////////////////////////////////////////
    // State
    ////////////////////////////////////////////////////
     */

    /**
     * Object that keeps track of the current contextual state
     * of the generator.
     */
    protected JsonWriteContext _writeContext;

    /**
     * Flag that indicates whether generator is closed or not. Gets
     * set when it is closed by an explicit call
     * ({@link #close}).
     */
    protected boolean _closed;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected JsonGeneratorBase(int features, ObjectCodec codec)
    {
        super();
        _features = features;
        _writeContext = JsonWriteContext.createRootContext();
        _objectCodec = codec;
        _cfgNumbersAsStrings = isEnabled(Feature.WRITE_NUMBERS_AS_STRINGS);
    }

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    @Override
    public JsonGenerator enable(Feature f) {
        _features |= f.getMask();
        if (f == Feature.WRITE_NUMBERS_AS_STRINGS) {
            _cfgNumbersAsStrings = true;
        }
        return this;
    }

    @Override
    public JsonGenerator disable(Feature f) {
        _features &= ~f.getMask();
        if (f == Feature.WRITE_NUMBERS_AS_STRINGS) {
            _cfgNumbersAsStrings = false;
        }
        return this;
    }

    //public JsonGenerator configure(Feature f, boolean state) { }

    @Override
    public final boolean isEnabled(Feature f) {
        return (_features & f.getMask()) != 0;
    }

    public final JsonGenerator useDefaultPrettyPrinter()
    {
        return setPrettyPrinter(new DefaultPrettyPrinter());
    }

    public final JsonGenerator setCodec(ObjectCodec oc) {
        _objectCodec = oc;
        return this;
    }

    public final ObjectCodec getCodec() { return _objectCodec; }

    /*
    ////////////////////////////////////////////////////
    // Public API, accessors
    ////////////////////////////////////////////////////
     */

    /**
     * Note: co-variant return type.
     */
    public final JsonWriteContext getOutputContext() { return _writeContext; }

    /*
    ////////////////////////////////////////////////////
    // Public API, write methods, structural
    ////////////////////////////////////////////////////
     */

    public final void writeStartArray()
        throws IOException, JsonGenerationException
    {
        // Array is a value, need to verify it's allowed
        _verifyValueWrite("start an array");
        _writeContext = _writeContext.createChildArrayContext();
        if (_cfgPrettyPrinter != null) {
            _cfgPrettyPrinter.writeStartArray(this);
        } else {
            _writeStartArray();
        }
    }

    protected abstract void _writeStartArray()
        throws IOException, JsonGenerationException;

    public final void writeEndArray()
        throws IOException, JsonGenerationException
    {
        if (!_writeContext.inArray()) {
            _reportError("Current context not an ARRAY but "+_writeContext.getTypeDesc());
        }
        if (_cfgPrettyPrinter != null) {
            _cfgPrettyPrinter.writeEndArray(this, _writeContext.getEntryCount());
        } else {
            _writeEndArray();
        }
        _writeContext = _writeContext.getParent();
    }

    protected abstract void _writeEndArray()
        throws IOException, JsonGenerationException;

    public final void writeStartObject()
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("start an object");
        _writeContext = _writeContext.createChildObjectContext();
        if (_cfgPrettyPrinter != null) {
            _cfgPrettyPrinter.writeStartObject(this);
        } else {
            _writeStartObject();
        }
    }

    protected abstract void _writeStartObject()
        throws IOException, JsonGenerationException;

    public final void writeEndObject()
        throws IOException, JsonGenerationException
    {
        if (!_writeContext.inObject()) {
            _reportError("Current context not an object but "+_writeContext.getTypeDesc());
        }
        _writeContext = _writeContext.getParent();
        if (_cfgPrettyPrinter != null) {
            _cfgPrettyPrinter.writeEndObject(this, _writeContext.getEntryCount());
        } else {
            _writeEndObject();
        }
    }

    protected abstract void _writeEndObject()
        throws IOException, JsonGenerationException;

    public final void writeFieldName(String name)
        throws IOException, JsonGenerationException
    {
        // Object is a value, need to verify it's allowed
        int status = _writeContext.writeFieldName(name);
        if (status == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name, (status == JsonWriteContext.STATUS_OK_AFTER_COMMA));
    }

    protected abstract void _writeFieldName(String name, boolean commaBefore)
        throws IOException, JsonGenerationException;

    /*
    ////////////////////////////////////////////////////
    // Public API, write methods, textual
    ////////////////////////////////////////////////////
     */

    //public abstract void writeString(String text) throws IOException, JsonGenerationException;

    //public abstract void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException;

    //public abstract void writeRaw(String text) throws IOException, JsonGenerationException;

    //public abstract void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException;

    //public abstract void writeBinary(byte[] data, int offset, int len, boolean includeLFs) throws IOException, JsonGenerationException;

    /*
    ////////////////////////////////////////////////////
    // Public API, write methods, primitive
    ////////////////////////////////////////////////////
     */

    public abstract void writeNumber(int i)
        throws IOException, JsonGenerationException;

    public abstract void writeNumber(long l)
        throws IOException, JsonGenerationException;

    public abstract void writeNumber(double d)
        throws IOException, JsonGenerationException;

    public abstract void writeNumber(float f)
        throws IOException, JsonGenerationException;

    public abstract void writeNumber(BigDecimal dec)
        throws IOException, JsonGenerationException;

    public abstract void writeBoolean(boolean state)
        throws IOException, JsonGenerationException;

    public abstract void writeNull()
        throws IOException, JsonGenerationException;

    /*
    ////////////////////////////////////////////////////
    // Public API, write methods, POJOs, trees
    ////////////////////////////////////////////////////
     */

    public abstract void writeObject(Object value)
        throws IOException, JsonProcessingException;

    public abstract void writeTree(JsonNode rootNode)
        throws IOException, JsonProcessingException;

    /*
    ////////////////////////////////////////////////////
    // Public API, low-level output handling
    ////////////////////////////////////////////////////
     */

    public abstract void flush() throws IOException;

    public void close() throws IOException
    {
        _closed = true;
    }

    public boolean isClosed() { return _closed; }

    /*
    ////////////////////////////////////////////////////
    // Public API, copy-through methods
    ////////////////////////////////////////////////////
     */

    public final void copyCurrentEvent(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        switch(jp.getCurrentToken()) {
        case START_OBJECT:
            writeStartObject();
            break;
        case END_OBJECT:
            writeEndObject();
            break;
        case START_ARRAY:
            writeStartArray();
            break;
        case END_ARRAY:
            writeEndArray();
            break;
        case FIELD_NAME:
            writeFieldName(jp.getCurrentName());
            break;
        case VALUE_STRING:
            writeString(jp.getTextCharacters(), jp.getTextOffset(), jp.getTextLength());
            break;
        case VALUE_NUMBER_INT:
            writeNumber(jp.getIntValue());
            break;
        case VALUE_NUMBER_FLOAT:
            writeNumber(jp.getDoubleValue());
            break;
        case VALUE_TRUE:
            writeBoolean(true);
            break;
        case VALUE_FALSE:
            writeBoolean(false);
            break;
        case VALUE_NULL:
            writeNull();
            break;
        default:
            _cantHappen();
        }
    }

    public final void copyCurrentStructure(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();

        // Let's handle field-name separately first
        if (t == JsonToken.FIELD_NAME) {
            writeFieldName(jp.getCurrentName());
            t = jp.nextToken();
            // fall-through to copy the associated value
        }

        switch (t) {
        case START_ARRAY:
            writeStartArray();
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                copyCurrentStructure(jp);
            }
            writeEndArray();
            break;
        case START_OBJECT:
            writeStartObject();
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                copyCurrentStructure(jp);
            }
            writeEndObject();
            break;
        default: // others are simple:
            copyCurrentEvent(jp);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Package methods for this, sub-classes
    ////////////////////////////////////////////////////
     */

    protected abstract void _releaseBuffers();

    protected abstract void _verifyValueWrite(String typeMsg)
        throws IOException, JsonGenerationException;

    protected void _reportError(String msg)
        throws JsonGenerationException
    {
        throw new JsonGenerationException(msg);
    }

    protected void _cantHappen()
    {
        throw new RuntimeException("Internal error: should never end up through this code path");
    }
}
