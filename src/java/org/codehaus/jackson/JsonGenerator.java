/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code and binary code bundles.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.jackson;

import java.io.*;
import java.math.BigDecimal;

/**
 * Base class that defines public API for writing JSON content.
 * Instances are created using factory methods of
 * a {@link JsonFactory} instance.
 *
 * @author Tatu Saloranta
 */
public abstract class JsonGenerator
{
    /**
     * Enumeration that defines all togglable features for parsers.
     */
    public enum Feature {
        /**
         * Feature that determines whether generator will automatically
         * close underlying output target that is NOT owned by the
         * generator.
         * If disabled, calling application has to separately
         * close the underlying {@link OutputStream} and {@link Writer}
         * instances used to create the generator. If enabled, generator
         * will handle closing, as long as generator itself gets closed:
         * this happens when end-of-input is encountered, or generator
         * is closed by a call to {@link JsonGenerator#close}.
         *<p>
         * Feature is enabled by default.
         */
        AUTO_CLOSE_TARGET(true);
            ;

        final boolean _defaultState;

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
        }
        
        public boolean enabledByDefault() { return _defaultState; }
    
        public int getMask() { return (1 << ordinal()); }
    };

    // // // Configuration:

    /**
     * Object that handles pretty-printing (usually additional
     * white space to make results more human-readable) during
     * output. If null, no pretty-printing is done.
     */
    protected PrettyPrinter _cfgPrettyPrinter;

    protected JsonGenerator() { }

    /*
    ////////////////////////////////////////////////////
    // Public API, configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Method for enabling specified parser features
     * (check {@link Feature} for list of features)
     */
    public abstract void enableFeature(Feature f);

    /**
     * Method for disabling specified  features
     * (check {@link Feature} for list of features)
     */
    public abstract void disableFeature(Feature f);

    public abstract void setFeature(Feature f, boolean state);

    public abstract boolean isFeatureEnabled(Feature f);

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
        _cfgPrettyPrinter = pp;
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
    // Public API, copy-through methods
    ////////////////////////////////////////////////////
     */

    /**
     * Method for copying contents of the current event that
     * the given parser instance points to.
     * Note that the method <b>will not</b> copy any other events,
     * such as events contained within JSON Array or Object structures.
     *<p>
     * Calling this method will not advance the given
     * parser, although it may cause parser to internally process
     * more data (if it lazy loads contents of value events, for example)
     */
    public abstract void copyCurrentEvent(JsonParser jp)
        throws IOException, JsonProcessingException;

    /**
     * Method for copying contents of the current event
     * <b>and following events that it encloses</b>
     * the given parser instance points to.
     *<p>
     * So what constitutes enclosing? Here is the list of
     * events that have associated enclosed events that will
     * get copied:
     *<ul>
     * <li>{@link JsonToken#START_OBJECT}:
     *   all events up to and including matching (closing)
     *   {@link JsonToken#END_OBJECT} will be copied
     *  </li>
     * <li>{@link JsonToken#START_ARRAY}
     *   all events up to and including matching (closing)
     *   {@link JsonToken#END_ARRAY} will be copied
     *  </li>
     * <li>{@link JsonToken#FIELD_NAME} the logical value (which
     *   can consist of a single scalar value; or a sequence of related
     *   events for structured types (JSON Arrays, Objects)) will
     *   be copied along with the name itself. So essentially the
     *   whole <b>field entry</b> (name and value) will be copied.
     *  </li>
     *</ul>
     *<p>
     * After calling this method, parser will point to the
     * <b>last event</b> that was copied. This will either be
     * the event parser already pointed to (if there were no
     * enclosed events), or the last enclosed event copied.
     */
    public abstract void copyCurrentStructure(JsonParser jp)
        throws IOException, JsonProcessingException;

    /*
    ////////////////////////////////////////////////////
    // Public API, context access
    ////////////////////////////////////////////////////
     */

    /**
     * @return Context object that can give information about logical
     *   position within generated json content.
     */
    public abstract JsonWriteContext getOutputContext();

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
