package org.codehaus.jackson;

import java.io.*;
import java.math.BigDecimal;

/**
 * This base class defines API for output JSON content.
 */
public abstract class JsonGenerator
{
    /**
     * Object that handles pretty-printing (usually additional
     * white space to make results more human-readable) during
     * output. If null, no pretty-printing is done.
     */
    protected PrettyPrinter mPrettyPrinter;

    protected JsonGenerator() { }

    /*
    ////////////////////////////////////////////////////
    // Configuring generator
    ////////////////////////////////////////////////////
      */

    public final void setPrettyPrinter(PrettyPrinter pp) {
        mPrettyPrinter = pp;
    }

    /**
     * Convenience method for enabling pretty-printing using
     * the default pretty printer
     * ({@link org.codehaus.jackson.impl.DefaultPrettyPrinter}).
     */
    public abstract void useDefaultPrettyPrinter();

    /*
    ////////////////////////////////////////////////////
    // Public API, write methods, structural
    ////////////////////////////////////////////////////
     */

    public abstract void writeStartArray()
        throws IOException, JsonGenerationException;

    public abstract void writeEndArray()
        throws IOException, JsonGenerationException;

    public abstract void writeStartObject()
        throws IOException, JsonGenerationException;

    public abstract void writeEndObject()
        throws IOException, JsonGenerationException;

    protected abstract void doWriteEndObject()
        throws IOException, JsonGenerationException;

    public abstract void writeFieldName(String name)
        throws IOException, JsonGenerationException;

    protected abstract void doWriteFieldName(String name, boolean commaBefore)
        throws IOException, JsonGenerationException;

    /*
    ////////////////////////////////////////////////////
    // Public API, write methods, textual/binary
    ////////////////////////////////////////////////////
     */

    public abstract void writeString(String text)
        throws IOException, JsonGenerationException;

    public abstract void writeString(char[] text, int offset, int len)
        throws IOException, JsonGenerationException;

    /**
     * Fallback method which can be used to make generator copy
     * input text verbatim with no modifications
     */
    public abstract void writeRaw(String text)
        throws IOException, JsonGenerationException;

    public abstract void writeRaw(String text, int offset, int len)
        throws IOException, JsonGenerationException;

    public abstract void writeRaw(char[] text, int offset, int len)
        throws IOException, JsonGenerationException;

    public abstract void writeRaw(char c)
        throws IOException, JsonGenerationException;

    /**
     * Method that will output given chunk of binary data as base64
     * encoded, as a complete String value (surrounded by double quotes).
     * Note: 
     */
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

    public abstract void flush()
        throws IOException;

    public abstract void close()
        throws IOException;
}
