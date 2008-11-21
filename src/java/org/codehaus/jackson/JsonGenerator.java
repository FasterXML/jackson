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

    /**
     * Method for setting a custom pretty printer, which is usually
     * used to add indentation for improved human readability.
     * By default, generator does not do pretty printing.
     *<p>
     * To use the default pretty printer that comes with core
     * Jackson distribution, call {@link #useDefaultPrettyPrinter}
     * instead.
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

    public abstract void writeFieldName(String name)
        throws IOException, JsonGenerationException;

    /*
    ////////////////////////////////////////////////////
    // Public API, write methods, textual/binary
    ////////////////////////////////////////////////////
     */

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) field value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     */
    public abstract void writeString(String text)
        throws IOException, JsonGenerationException;

    public abstract void writeString(char[] text, int offset, int len)
        throws IOException, JsonGenerationException;

    /**
     * Fallback method which can be used to make generator copy
     * input text verbatim with <b>no</b> modifications (including
     * that no quoting is done and no separators are added even
     * if context [array, object] would otherwise require such)
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
     * This method defaults
     *<p>
     * Note: because JSON Strings can not contain unescaped linefeeds,
     * if linefeeds are included (as per last argument), they must be
     * escaped. This adds overhead for decoding without improving
     * readability.
     * Alternatively if linefeeds are not included,
     * resulting String value may violate the requirement of base64
     * RFC which mandates line-length of 76 characters and use of
     * linefeeds. However, all {@link JsonParser} implementations
     * are required to accept such "long line base64"; as do
     * typical production-level base64 decoders.
     *
     * @param b64variant Base64 variant to use: defines details such as
     *   whether padding is used (and if so, using which character);
     *   what is the maximum line length before adding linefeed,
     *   and also the underlying alphabet to use.
     */
    public abstract void writeBinary(Base64Variant b64variant,
                                     byte[] data, int offset, int len)
        throws IOException, JsonGenerationException;

    /**
     * Similar to {@link #writeBinary(Base64Variant,byte[],int,int)},
     * but default to using the Jackson default Base64 variant 
     * (which is {@link Base64Variants#MIME_NO_LINEFEEDS}).
     */
    public final void writeBinary(byte[] data, int offset, int len)
        throws IOException, JsonGenerationException
    {
        writeBinary(Base64Variants.getDefaultVariant(), data, offset, len);
    }

    /**
     * Similar to {@link #writeBinary(Base64Variant,byte[],int,int)},
     * but assumes default to using the Jackson default Base64 variant 
     * (which is {@link Base64Variants#MIME_NO_LINEFEEDS}). Also
     * assumes that whole byte array is to be output.
     */
    public final void writeBinary(byte[] data)
        throws IOException, JsonGenerationException
    {
        writeBinary(Base64Variants.getDefaultVariant(), data, 0, data.length);
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, write methods, primitives
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

    /**
     * Write method that can be used for custom numeric types that can
     * not be (easily?) converted to "standard" Java number types.
     * Because numbers are not surrounded by double quotes, regular
     * {@link #writeString} method can not be used; nor
     * {@link #writeRaw} because that does not properly handle
     * value separators needed in Array or Object contexts.
     *<p>
     * Note: because of lack of type safety, some generator
     * implementations may not be able to implement this
     * method. For example, if a binary json format is used,
     * it may require type information for encoding; similarly
     * for generator-wrappers around Java objects or JSON nodes.
     * If implementation does not implement this method,
     * it needs to throw {@link UnsupportedOperationException}.
     */
    public abstract void writeNumber(String encodedValue)
        throws IOException, JsonGenerationException,
               UnsupportedOperationException;

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
