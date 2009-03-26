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

import org.codehaus.jackson.type.TypeReference;

/**
 * Base class that defines public API for reading JSON content.
 * Instances are created using factory methods of
 * a {@link JsonFactory} instance.
 *
 * @author Tatu Saloranta
 */
public abstract class JsonParser
{
    /**
     * Enumeration of possible "native" (optimal) types that can be
     * used for numbers.
     */
    public enum NumberType {
        INT, LONG, BIG_INTEGER, FLOAT, DOUBLE, BIG_DECIMAL
    };

    /**
     * Enumeration that defines all togglable features for parsers.
     */
    public enum Feature {
        /**
         * Feature that determines whether parser will automatically
         * close underlying input source that is NOT owned by the
         * parser. If disabled, calling application has to separately
         * close the underlying {@link InputStream} and {@link Reader}
         * instances used to create the parser. If enabled, parser
         * will handle closing, as long as parser itself gets closed:
         * this happens when end-of-input is encountered, or parser
         * is closed by a call to {@link JsonParser#close}.
         *<p>
         * Feature is enabled by default.
         */
        AUTO_CLOSE_SOURCE(true)
            
            /**
             * Feature that determines whether parser will allow use
             * of Java/C++ style comments (both '/'+'*' and
             * '//' varieties) within parsed content or not.
             * Given that JSON specification does not mention comments,
             * this is a non-standards feature; however, in the wild
             * this is extensively used. As such, feature is
             * <b>disabled by default</b> for parsers and must be
             * explicitly enabled (via factory or parser instance).
             */
            ,ALLOW_COMMENTS(false)
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

    /*
    ////////////////////////////////////////////////////
    // Minimal configuration state
    ////////////////////////////////////////////////////
     */

    /**
     * Bit flag composed of bits that indicate which
     * {@link org.codehaus.jackson.JsonParser.Feature}s
     * are enabled.
     */
    protected int _features;

    /*
    ////////////////////////////////////////////////////
    // Minimal generic state
    ////////////////////////////////////////////////////
     */

    /**
     * Last token retrieved via {@link #nextToken}, if any.
     * Null before the first call to <code>nextToken()</code>,
     * as well as if token has been explicitly cleared
     * (by call to {@link #clearCurrentToken})
     */
    protected JsonToken _currToken;

    /**
     * Last cleared token, if any: that is, value that was in
     * effect when {@link #clearCurrentToken} was called.
     */
    protected JsonToken _lastClearedToken;

    /*
    ////////////////////////////////////////////////////
    // Construction, init
    ////////////////////////////////////////////////////
     */

    protected JsonParser() { }

    /*
    ////////////////////////////////////////////////////
    // Public API, configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Method for enabling specified parser feature
     * (check {@link Feature} for list of features)
     */
    public void enableFeature(Feature f) {
        _features |= f.getMask();
    }

    /**
     * Method for disabling specified  feature
     * (check {@link Feature} for list of features)
     */
    public void disableFeature(Feature f) {
        _features &= ~f.getMask();
    }

    /**
     * Method for enabled or disabling specified feature
     * (check {@link Feature} for list of features)
     */
    public void setFeature(Feature f, boolean state) {
        if (state) {
            enableFeature(f);
        } else {
            disableFeature(f);
        }
    }

    /**
     * Method for checking whether specified {@link Feature}
     * is enabled.
     */
    public final boolean isFeatureEnabled(Feature f) {
        return (_features & f.getMask()) != 0;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, traversal
    ////////////////////////////////////////////////////
     */

    /**
     * Main iteration method, which will advance stream enough
     * to determine type of the next token, if any. If none
     * remaining (stream has no content other than possible
     * white space before ending), null will be returned.
     *
     * @return Next token from the stream, if any found, or null
     *   to indicate end-of-input
     */
    public abstract JsonToken nextToken()
        throws IOException, JsonParseException;

    /**
     * Iteration method that will advance stream enough
     * to determine type of the next token that is a value type
     * (including Json Array and Object start/end markers).
     * Or put another way, nextToken() will be called once,
     * and if {@link JsonToken#FIELD_NAME} is returned, another
     * time to get the value for the field.
     * Method is most useful for iterating over value entries
     * of Json objects; field name will still be available
     * by calling {@link #getCurrentName} when parser points to
     * the value.
     *
     * @return Next non-field-name token from the stream, if any found,
     *   or null to indicate end-of-input (or, for non-blocking
     *   parsers, {@link JsonToken#NOT_AVAILABLE} if no tokens were
     *   available yet)
     *
     * @since 0.9.7
     */
    public abstract JsonToken nextValue()
        throws IOException, JsonParseException;

    /**
     * Method that will skip all child tokens of an array or
     * object token that the parser currently points to,
     * iff stream points to 
     * {@link JsonToken#START_OBJECT} or {@link JsonToken#START_ARRAY}.
     * If not, it will do nothing.
     * After skipping, stream will point to <b>matching</b>
     * {@link JsonToken#END_OBJECT} or {@link JsonToken#END_ARRAY}
     * (possibly skipping nested pairs of START/END OBJECT/ARRAY tokens
     * as well as value tokens).
     * The idea is that after calling this method, application
     * will call {@link #nextToken} to point to the next
     * available token, if any.
     */
    public abstract void skipChildren()
        throws IOException, JsonParseException;

    /**
     * Closes the parser so that no further iteration or data access
     * can be made.
     *<p>
     * Method will also close the underlying input source,
     * if parser either <b>owns</b> the input source, or feature
     * {@link Feature#AUTO_CLOSE_SOURCE} is enabled.
     * Whether parser owns the input source depends on factory
     * method that was used to construct instance (so check
     * {@link org.codehaus.jackson.JsonFactory} for details,
     * but the general
     * idea is that if caller passes in closable resource (such
     * as {@link InputStream} or {@link Reader}) parser does NOT
     * own the source; but if it passes a reference (such as
     * {@link java.io.File} or {@link java.net.URL} and creates
     * stream or reader it does own them.
     */
    public abstract void close() throws IOException;

    /**
     * Method that can be called to determine whether this parser
     * is closed or not. If it is closed, no new events can be
     * retrieved by calling {@link #nextToken} (and the underlying
     * stream may be closed). Closing may be due to an explicit
     * call to {@link #close} or because parser has encountered
     * end of input.
     *
     * @since 0.9.9-3
     */
    public abstract boolean isClosed();

    /*
    ////////////////////////////////////////////////////
    // Public API, token accessors
    ////////////////////////////////////////////////////
     */

    /**
     * @return Type of the token this parser currently points to,
     *   if any: null before any tokens have been read, and
     *   after end-of-input has been encountered, as well as
     *   if the current token has been explicitly cleared.
     */
    public final JsonToken getCurrentToken()
    {
        return _currToken;
    }

    /**
     * @return True if the parser just returned a valid
     *   token via {@link #nextToken}; false otherwise (parser
     *   was just constructed, encountered end-of-input
     *   and returned null from {@link #nextToken}, or the event
     *   has been consumed)
     */
    public final boolean hasCurrentToken()
    {
        return _currToken != null;
    }


    /**
     * Method called to "consume" the current token by effectively
     * removing it so that {@link #hasCurrentToken} returns false, and
     * {@link #getCurrentToken} null).
     * Cleared token value can still be accessed by calling
     * {@link #getLastClearedToken} (if absolutely needed), but
     * usually isn't.
     *<p>
     * Method was added to be used by the optional data binder, since
     * it has to be able to consume last token used for binding (so that
     * it will not be used again).
     */
    public final void clearCurrentToken() {
        _lastClearedToken = _currToken;
        _currToken = null;
    }

    /**
     * Method that can be called to get the name associated with
     * the current event: for {@link JsonToken#FIELD_NAME}s it will
     * be the same as what {@link #getText} returns;
     * for field values it will be preceding field name;
     * and for others (array values, root-level values) null.
     */
    public abstract String getCurrentName()
        throws IOException, JsonParseException;

    /**
     * Method that can be used to access current parsing context reader
     * is in. There are 3 different types: root, array and object contexts,
     * with slightly different available information. Contexts are
     * hierarchically nested, and can be used for example for figuring
     * out part of the input document that correspond to specific
     * array or object (for highlighting purposes, or error reporting).
     * Contexts can also be used for simple xpath-like matching of
     * input, if so desired.
     */
    public abstract JsonStreamContext getParsingContext();

    /**
     * Method that return the <b>starting</b> location of the current
     * token; that is, position of the first character from input
     * that starts the current token.
     */
    public abstract JsonLocation getTokenLocation();

    /**
     * Method that returns location of the last processed character;
     * usually for error reporting purposes.
     */
    public abstract JsonLocation getCurrentLocation();

    /**
     * Method that can be called to get the last token that was
     * cleared using {@link #clearCurrentToken}. This is not necessarily
     * the latest token read.
     * Will return null if no tokens have been cleared,
     * or if parser has been closed.
     */
    public JsonToken getLastClearedToken() {
        return _lastClearedToken;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, access to token information, text
    ////////////////////////////////////////////////////
     */

    /**
     * Method for accessing textual representation of the current event;
     * if no current event (before first call to {@link #nextToken}, or
     * after encountering end-of-input), returns null.
     * Method can be called for any event.
     */
    public abstract String getText()
        throws IOException, JsonParseException;

    /**
     * Method similar to {@link #getText}, but that will return
     * underlying (unmodifiable) character array that contains
     * textual value, instead of constructing a String object
     * to contain this information.
     * Note, however, that:
     *<ul>
     * <li>Textual contents are not guaranteed to start at
     *   index 0 (rather, call {@link #getTextOffset}) to
     *   know the actual offset
     *  </li>
     * <li>Length of textual contents may be less than the
     *  length of returned buffer: call {@link #getTextLength}
     *  for actual length of returned content.
     *  </li>
     * </ul>
     *<p>
     * Note that caller <b>MUST NOT</b> modify the returned
     * character array in any way -- doing so may corrupt
     * current parser state and render parser instance useless.
     *<p>
     * The only reason to call this method (over {@link #getText})
     * is to avoid construction of a String object (which
     * will make a copy of contents).
     */
    public abstract char[] getTextCharacters()
        throws IOException, JsonParseException;

    /**
     * @return Number of characters within buffer returned
     *   by {@link #getTextCharacters} that are part of
     *   textual content of the current token.
     */
    public abstract int getTextLength()
        throws IOException, JsonParseException;

    /**
     * @return Offset of the first character within buffer returned
     *   by {@link #getTextCharacters} that is part of
     *   textual content of the current token.
     */
    public abstract int getTextOffset()
        throws IOException, JsonParseException;

    /*
    ////////////////////////////////////////////////////
    // Public API, access to token information, numeric
    ////////////////////////////////////////////////////
     */

    /**
     * Generic number value accessor method that will work for
     * all kinds of numeric values. It will return the optimal
     * (simplest/smallest possibl) wrapper object that can
     * express the numeric value just parsed.
     */
    public abstract Number getNumberValue()
        throws IOException, JsonParseException;

    /**
     * If current event is of type 
     * {@link JsonToken#VALUE_NUMBER_INT} or
     * {@link JsonToken#VALUE_NUMBER_FLOAT}, returns
     * one of {@link NumberType} constants; otherwise returns null.
     */
    public abstract NumberType getNumberType()
        throws IOException, JsonParseException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a value of Java byte primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the resulting integer value falls outside range of
     * Java byte, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract byte getByteValue()
        throws IOException, JsonParseException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a value of Java short primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the resulting integer value falls outside range of
     * Java short, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract short getShortValue()
        throws IOException, JsonParseException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a value of Java int primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the resulting integer value falls outside range of
     * Java int, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract int getIntValue()
        throws IOException, JsonParseException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a Java long primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting to int; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the token is an integer, but its value falls
     * outside of range of Java long, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract long getLongValue()
        throws IOException, JsonParseException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} and
     * it can be expressed as a Java float primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_INT};
     * if so, it is equivalent to calling {@link #getLongValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the value falls
     * outside of range of Java float, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract float getFloatValue()
        throws IOException, JsonParseException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} and
     * it can be expressed as a Java double primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_INT};
     * if so, it is equivalent to calling {@link #getLongValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the value falls
     * outside of range of Java double, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract double getDoubleValue()
        throws IOException, JsonParseException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} or
     * {@link JsonToken#VALUE_NUMBER_INT}. No under/overflow exceptions
     * are ever thrown.
     */
    public abstract BigDecimal getDecimalValue()
        throws IOException, JsonParseException;

    /*
    ////////////////////////////////////////////////////
    // Public API, access to token information, binary
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to read (and consume -- results
     * may not be accessible using other methods after the call)
     * base64-encoded binary data
     * included in the current textual json value.
     * It works similar to getting String value via {@link #getText}
     * and decoding result (except for decoding part),
     * but should be significantly more performant.
     *<p>
     * Note that non-decoded textual contents of the current event
     * are not guaranteed to be accessible after this method
     * is called. Current implementation, for example, clears up
     * textual content during decoding.
     * Decoded binary content, however, will be retained until
     * parser is advanced to the next event.
     *
     * @param b64variant Expected variant of base64 encoded
     *   content (see {@link Base64Variants} for definitions
     *   of "standard" variants).
     *
     * @return Decoded binary data
     */
    public abstract byte[] getBinaryValue(Base64Variant b64variant) throws IOException, JsonParseException;

    /**
     * Convenience alternative to {@link #getBinaryValue(Base64Variant)}
     * that defaults to using
     * {@link Base64Variants#getDefaultVariant} as the default encoding.
     */
    public byte[] getBinaryValue() throws IOException, JsonParseException
    {
        return getBinaryValue(Base64Variants.getDefaultVariant());
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, optional data binding functionality
    ////////////////////////////////////////////////////
     */

    /**
     * Method to deserialize Json content into a non-container
     * type (it can be an array type, however): typically a bean, array
     * or a wrapper type (like {@link java.lang.Boolean}).
     * <b>Note</b>: method can only be called if the parser has
     * an object codec assigned; this is true for parsers constructed
     * by {@link org.codehaus.jackson.map.MappingJsonFactory} but
     * not for {@link JsonFactory} (unless its <code>setCodec</code>
     * method has been explicitly called).
     *<p>
     * This method may advance the evens stream, for structured types
     * the current event will be the closing end marker (END_ARRAY,
     * END_OBJECT) of the bound structure. For non-structured Json types
     * stream is not advanced.
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     */
    public abstract <T> T readValueAs(Class<T> valueType)
        throws IOException, JsonProcessingException;

    /**
     * Method to deserialize Json content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token"
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     * <b>Note</b>: method can only be called if the parser has
     * an object codec assigned; this is true for parsers constructed
     * by {@link org.codehaus.jackson.map.MappingJsonFactory} but
     * not for {@link JsonFactory} (unless its <code>setCodec</code>
     * method has been explicitly called).
     *<p>
     * This method may advance the evens stream, for structured types
     * the current event will be the closing end marker (END_ARRAY,
     * END_OBJECT) of the bound structure. For non-structured Json types
     * stream is not advanced.
     */
    public abstract <T> T readValueAs(TypeReference<?> valueTypeRef)
        throws IOException, JsonProcessingException;
}
