package org.codehaus.jackson.impl;

import java.io.*;
import java.math.BigDecimal;

import org.codehaus.jackson.*;

/**
 * This base class defines API that a JSON generator exposes
 * to applications, as well as internal API that sub-classes
 * have to implement.
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

    public void writeStartArray()
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

    public void writeEndArray()
        throws IOException, JsonGenerationException
    {
        if (!mWriteContext.inArray()) {
            reportError("Current context not an array but "+mWriteContext.getType());
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

    public void writeStartObject()
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

    public void writeEndObject()
        throws IOException, JsonGenerationException
    {
        if (!mWriteContext.inObject()) {
            reportError("Current context not an object but "+mWriteContext.getType());
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

    public void writeFieldName(String name)
        throws IOException, JsonGenerationException
    {
        // Object is a value, need to verify it's allowed
        int status = mWriteContext.writeFieldName(name);
        if (status == JsonWriteContext.STATUS_EXPECT_VALUE) {
            reportError("Can not write a field name, expecting a value");
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

    public abstract void writeString(String text)
        throws IOException, JsonGenerationException;

    public abstract void writeString(char[] text, int offset, int len)
        throws IOException, JsonGenerationException;

    public abstract void writeRaw(String text)
        throws IOException, JsonGenerationException;

    public abstract void writeRaw(char[] text, int offset, int len)
        throws IOException, JsonGenerationException;

    public abstract void writeBinary(byte[] data, int offset, int len)
        throws IOException, JsonGenerationException;

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
    // Package methods for this, sub-classes
    ////////////////////////////////////////////////////
     */

    protected abstract void verifyValueWrite(String typeMsg)
        throws IOException, JsonGenerationException;

    protected void reportError(String msg)
        throws JsonGenerationException
    {
        throw new JsonGenerationException(msg);
    }
}
