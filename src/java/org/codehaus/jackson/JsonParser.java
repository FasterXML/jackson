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

    protected JsonParser() { }

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
     * @return Type of the token this parser currently points to,
     *   if any: null both before any tokens have been read, and
     *   after end-of-input has been encountered.
     */
    public abstract JsonToken getCurrentToken();

    /**
     * @return True if the parser just returned a valid
     *   token via {@link #nextToken}; false otherwise (parser
     *   was just constructed, or encountered end-of-input
     *   and returned null from {@link #nextToken}.
     */
    public abstract boolean hasCurrentToken();

    /**
     * Method that can be called to get the name associated with
     * the current event: for {@link JsonToken#FIELD_NAME} it will
     * be the same as {@link #getText}, for field values Objects name
     * of matching field name; and for others (array values, root-level
     * values) null.
     */
    public abstract String getCurrentName()
        throws IOException, JsonParseException;

    /**
     * Closes the parser so that no iteration or access methods
     * can be called.
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
     * Method that can be used to access current parsing context reader
     * is in. There are 3 different types: root, array and object contexts,
     * with slightly different available information. Contexts are
     * hierarchically nested, and can be used for example for figuring
     * out part of the input document that correspond to specific
     * array or object (for highlighting purposes, or error reporting).
     * Contexts can also be used for simple xpath-like matching of
     * input, if so desired.
     */
    public abstract JsonReadContext getParsingContext();

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
     * it can be expressed as a Java int primitive type.
     *<p>
     * Note: if the token is an integer, but its value falls
     * outside of range of Java int, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract int getIntValue()
        throws IOException, JsonParseException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a Java long primitive type.
     *<p>
     * Note: if the token is an integer, but its value falls
     * outside of range of Java long, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract long getLongValue()
        throws IOException, JsonParseException;

    public abstract double getDoubleValue()
        throws IOException, JsonParseException;

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
     * Note that the contents may be consumed by this call, and thus
     * only first call to method will produce any output. Likewise,
     * calls to methods like {@link #getText} are not guaranteed
     * to return anything, although they are not prevent from returning
     * some subset of content. That is, results of those calls are
     * undefined if done after call to this method.
     *<p>
     * Main benefit of this method compared to
     * {@link #readBinaryValue()} is that this method does not hold
     * the whole value contents in memory, and can be used to avoid
     * excessive memory usage, if the base64 segment is long.
     *
     * @param b64variant Expected variant of base64 encoded
     *   content (see {@link Base64Variants} for definitions
     *   of "standard" variants).
     *
     * @return Input stream through which decoded contents can
     *   be read
     */
    /*
    public abstract InputStream readBinaryValue(Base64Variant b64variant)
        throws IOException, JsonParseException;

    public final InputStream readBinaryValue() throws IOException, JsonParseException
    {
        return readBinaryValue(Base64Variants.getDefaultVariant());
    }
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
     * Note that the contents may be consumed by this call so that
     * the textual contents may not be visible via tohers calls
     * such as {@link #getText}. However, contents will be retained
     * as long as parser is not advanced, so that multiple calls
     * to this method will return the same contents.
     *<p>
     * Compared to {@link #readBinaryValue(OutputStream)} the main
     * difference is that this method will read and retain the whole
     * decoded binary results in memory until parser is advanced.
     *
     * @param b64variant Expected variant of base64 encoded
     *   content (see {@link Base64Variants} for definitions
     *   of "standard" variants).
     *
     * @return Decoded binary data
     */
    //public abstract byte[] readBinaryValue(Base64Variant b64variant) throws IOException, JsonParseException;
    //public abstract byte[] readBinaryValue() throws IOException, JsonParseException;
}
