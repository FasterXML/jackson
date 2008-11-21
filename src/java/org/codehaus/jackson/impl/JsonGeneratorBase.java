package org.codehaus.jackson.impl;

import java.io.*;
import java.math.BigDecimal;

import org.codehaus.jackson.*;

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
    // State
    ////////////////////////////////////////////////////
     */

    protected JsonWriteContext mWriteContext;


    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected JsonGeneratorBase()
    {
        super();
        mWriteContext = JsonWriteContext.createRootContext();
    }

    public final void useDefaultPrettyPrinter()
    {
        setPrettyPrinter(new DefaultPrettyPrinter());
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, write methods, structural
    ////////////////////////////////////////////////////
     */

    public final void writeStartArray()
        throws IOException, JsonGenerationException
    {
        // Array is a value, need to verify it's allowed
        verifyValueWrite("start an array");
        mWriteContext = mWriteContext.createChildArrayContext();
        if (mPrettyPrinter != null) {
            mPrettyPrinter.writeStartArray(this);
        } else {
            doWriteStartArray();
        }
    }

    protected abstract void doWriteStartArray()
        throws IOException, JsonGenerationException;

    public final void writeEndArray()
        throws IOException, JsonGenerationException
    {
        if (!mWriteContext.inArray()) {
            _reportError("Current context not an array but "+mWriteContext.getType());
        }
        if (mPrettyPrinter != null) {
            mPrettyPrinter.writeEndArray(this, mWriteContext.getEntryCount());
        } else {
            doWriteEndArray();
        }
        mWriteContext = mWriteContext.getParent();
    }

    protected abstract void doWriteEndArray()
        throws IOException, JsonGenerationException;

    public final void writeStartObject()
        throws IOException, JsonGenerationException
    {
        verifyValueWrite("start an object");
        mWriteContext = mWriteContext.createChildObjectContext();
        if (mPrettyPrinter != null) {
            mPrettyPrinter.writeStartObject(this);
        } else {
            doWriteStartObject();
        }
    }

    protected abstract void doWriteStartObject()
        throws IOException, JsonGenerationException;

    public final void writeEndObject()
        throws IOException, JsonGenerationException
    {
        if (!mWriteContext.inObject()) {
            _reportError("Current context not an object but "+mWriteContext.getType());
        }
        mWriteContext = mWriteContext.getParent();
        if (mPrettyPrinter != null) {
            mPrettyPrinter.writeEndObject(this, mWriteContext.getEntryCount());
        } else {
            doWriteEndObject();
        }
    }

    protected abstract void doWriteEndObject()
        throws IOException, JsonGenerationException;

    public final void writeFieldName(String name)
        throws IOException, JsonGenerationException
    {
        // Object is a value, need to verify it's allowed
        int status = mWriteContext.writeFieldName(name);
        if (status == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        doWriteFieldName(name, (status == JsonWriteContext.STATUS_OK_AFTER_COMMA));
    }

    public abstract void doWriteFieldName(String name, boolean commaBefore)
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
    // Public API, buffer handling
    ////////////////////////////////////////////////////
     */

    protected abstract void releaseBuffers();

    public abstract void flush()
        throws IOException;

    public abstract void close()
        throws IOException;

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

        JsonToken endMarker;

        switch (t) {
        case START_ARRAY:
            endMarker = JsonToken.END_ARRAY;
            break;
        case START_OBJECT:
            endMarker = JsonToken.END_OBJECT;
            break;
        default: // others are simple:
            copyCurrentEvent(jp);
            return;
        }

        while (jp.nextToken() != endMarker) {
            copyCurrentStructure(jp);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Package methods for this, sub-classes
    ////////////////////////////////////////////////////
     */

    protected abstract void verifyValueWrite(String typeMsg)
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
