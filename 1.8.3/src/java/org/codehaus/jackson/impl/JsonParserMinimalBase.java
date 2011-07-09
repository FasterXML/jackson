package org.codehaus.jackson.impl;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.io.NumberInput;

/**
 * Intermediate base class used by all Jackson {@link JsonParser}
 * implementations, but does not add any additional fields that depend
 * on particular method of obtaining input.
 * 
 * @since 1.6
 *
 * @author Tatu Saloranta
 */
public abstract class JsonParserMinimalBase
    extends JsonParser
{
    // Control chars:
    protected final static int INT_TAB = '\t';
    protected final static int INT_LF = '\n';
    protected final static int INT_CR = '\r';
    protected final static int INT_SPACE = 0x0020;

    // Markup
    protected final static int INT_LBRACKET = '[';
    protected final static int INT_RBRACKET = ']';
    protected final static int INT_LCURLY = '{';
    protected final static int INT_RCURLY = '}';
    protected final static int INT_QUOTE = '"';
    protected final static int INT_BACKSLASH = '\\';
    protected final static int INT_SLASH = '/';
    protected final static int INT_COLON = ':';
    protected final static int INT_COMMA = ',';
    protected final static int INT_ASTERISK = '*';
    protected final static int INT_APOSTROPHE = '\'';

    // Letters we need
    protected final static int INT_b = 'b';
    protected final static int INT_f = 'f';
    protected final static int INT_n = 'n';
    protected final static int INT_r = 'r';
    protected final static int INT_t = 't';
    protected final static int INT_u = 'u';

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected JsonParserMinimalBase() { }
    protected JsonParserMinimalBase(int features) {
        super(features);
    }

    /*
    /**********************************************************
    /* Configuration overrides if any
    /**********************************************************
     */

    // from base class:

    //public void enableFeature(Feature f)
    //public void disableFeature(Feature f)
    //public void setFeature(Feature f, boolean state)
    //public boolean isFeatureEnabled(Feature f)

    /*
    /**********************************************************
    /* JsonParser impl
    /**********************************************************
     */

    @Override
    public abstract JsonToken nextToken() throws IOException, JsonParseException;

    //public final JsonToken nextValue()

    @Override
    public JsonParser skipChildren() throws IOException, JsonParseException
    {
        if (_currToken != JsonToken.START_OBJECT
            && _currToken != JsonToken.START_ARRAY) {
            return this;
        }
        int open = 1;

        /* Since proper matching of start/end markers is handled
         * by nextToken(), we'll just count nesting levels here
         */
        while (true) {
            JsonToken t = nextToken();
            if (t == null) {
                _handleEOF();
                /* given constraints, above should never return;
                 * however, FindBugs doesn't know about it and
                 * complains... so let's add dummy break here
                 */
                return this;
            }
            switch (t) {
            case START_OBJECT:
            case START_ARRAY:
                ++open;
                break;
            case END_OBJECT:
            case END_ARRAY:
                if (--open == 0) {
                    return this;
                }
                break;
            }
        }
    }

    /**
     * Method sub-classes need to implement
     */
    protected abstract void _handleEOF() throws JsonParseException;

    //public JsonToken getCurrentToken()

    //public boolean hasCurrentToken()

    @Override
    public abstract String getCurrentName() throws IOException, JsonParseException;
    
    @Override
    public abstract void close() throws IOException;

    @Override
    public abstract boolean isClosed();

    @Override
    public abstract JsonStreamContext getParsingContext();

//    public abstract JsonLocation getTokenLocation();

//   public abstract JsonLocation getCurrentLocation();
    
    /*
    /**********************************************************
    /* Public API, access to token information, text
    /**********************************************************
     */

    @Override
    public abstract String getText() throws IOException, JsonParseException;

    @Override
    public abstract char[] getTextCharacters() throws IOException, JsonParseException;

    @Override
    public abstract boolean hasTextCharacters();

    @Override
    public abstract int getTextLength() throws IOException, JsonParseException;

    @Override
    public abstract int getTextOffset() throws IOException, JsonParseException;  

    /*
    /**********************************************************
    /* Public API, access to token information, binary
    /**********************************************************
     */

    @Override
    public abstract byte[] getBinaryValue(Base64Variant b64variant)
        throws IOException, JsonParseException;

    /*
    /**********************************************************
    /* Public API, access with conversion/coercion
    /**********************************************************
     */

    @Override
    public boolean getValueAsBoolean(boolean defaultValue) throws IOException, JsonParseException
    {
        if (_currToken != null) {
            switch (_currToken) {
            case VALUE_NUMBER_INT:
                return getIntValue() != 0;
            case VALUE_TRUE:
                return true;
            case VALUE_FALSE:
            case VALUE_NULL:
                return false;
            case VALUE_EMBEDDED_OBJECT:
                {
                    Object value = this.getEmbeddedObject();
                    if (value instanceof Boolean) {
                        return ((Boolean) value).booleanValue();
                    }
                }
            case VALUE_STRING:
                String str = getText().trim();
                if ("true".equals(str)) {
                    return true;
                }
                break;
            }
        }
        return defaultValue;
    }
    
    @Override
    public int getValueAsInt(int defaultValue) throws IOException, JsonParseException
    {
        if (_currToken != null) {
            switch (_currToken) {
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return getIntValue();
            case VALUE_TRUE:
                return 1;
            case VALUE_FALSE:
            case VALUE_NULL:
                return 0;
            case VALUE_STRING:
                return NumberInput.parseAsInt(getText(), defaultValue);
            case VALUE_EMBEDDED_OBJECT:
                {
                    Object value = this.getEmbeddedObject();
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                }
            }
        }
        return defaultValue;
    }
    
    @Override
    public long getValueAsLong(long defaultValue) throws IOException, JsonParseException
    {
        if (_currToken != null) {
            switch (_currToken) {
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return getLongValue();
            case VALUE_TRUE:
                return 1;
            case VALUE_FALSE:
            case VALUE_NULL:
                return 0;
            case VALUE_STRING:
                return NumberInput.parseAsLong(getText(), defaultValue);
            case VALUE_EMBEDDED_OBJECT:
                {
                    Object value = this.getEmbeddedObject();
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                }
            }
        }
        return defaultValue;
    }

    @Override
    public double getValueAsDouble(double defaultValue) throws IOException, JsonParseException
    {
        if (_currToken != null) {
            switch (_currToken) {
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return getDoubleValue();
            case VALUE_TRUE:
                return 1;
            case VALUE_FALSE:
            case VALUE_NULL:
                return 0;
            case VALUE_STRING:
                return NumberInput.parseAsDouble(getText(), defaultValue);
            case VALUE_EMBEDDED_OBJECT:
                {
                    Object value = this.getEmbeddedObject();
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                }
            }
        }
        return defaultValue;
    }

    /*
    /**********************************************************
    /* Error reporting
    /**********************************************************
     */

    protected void _reportUnexpectedChar(int ch, String comment)
        throws JsonParseException
    {
        String msg = "Unexpected character ("+_getCharDesc(ch)+")";
        if (comment != null) {
            msg += ": "+comment;
        }
        _reportError(msg);
    }

    protected void _reportInvalidEOF()
        throws JsonParseException
    {
        _reportInvalidEOF(" in "+_currToken);
    }

    protected void _reportInvalidEOF(String msg)
        throws JsonParseException
    {
        _reportError("Unexpected end-of-input"+msg);
    }

    protected void _reportInvalidEOFInValue() throws JsonParseException
    {
        _reportInvalidEOF(" in a value");
    }
    
    protected void _throwInvalidSpace(int i)
        throws JsonParseException
    {
        char c = (char) i;
        String msg = "Illegal character ("+_getCharDesc(c)+"): only regular white space (\\r, \\n, \\t) is allowed between tokens";
        _reportError(msg);
    }

    /**
     * Method called to report a problem with unquoted control character.
     * Note: starting with version 1.4, it is possible to suppress
     * exception by enabling {@link Feature#ALLOW_UNQUOTED_CONTROL_CHARS}.
     */
    protected void _throwUnquotedSpace(int i, String ctxtDesc)
        throws JsonParseException
    {
        // JACKSON-208; possible to allow unquoted control chars:
        if (!isEnabled(Feature.ALLOW_UNQUOTED_CONTROL_CHARS) || i >= INT_SPACE) {
            char c = (char) i;
            String msg = "Illegal unquoted character ("+_getCharDesc(c)+"): has to be escaped using backslash to be included in "+ctxtDesc;
            _reportError(msg);
        }
    }

    protected char _handleUnrecognizedCharacterEscape(char ch) throws JsonProcessingException
    {
        // as per [JACKSON-300]
        if (isEnabled(Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
            return ch;
        }
        // and [JACKSON-548]
        if (ch == '\'' && isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
            return ch;
        }
        _reportError("Unrecognized character escape "+_getCharDesc(ch));
        return ch;
    }
    
    /*
    /**********************************************************
    /* Error reporting, generic
    /**********************************************************
     */

    protected final static String _getCharDesc(int ch)
    {
        char c = (char) ch;
        if (Character.isISOControl(c)) {
            return "(CTRL-CHAR, code "+ch+")";
        }
        if (ch > 255) {
            return "'"+c+"' (code "+ch+" / 0x"+Integer.toHexString(ch)+")";
        }
        return "'"+c+"' (code "+ch+")";
    }

    protected final void _reportError(String msg)
        throws JsonParseException
    {
        throw _constructError(msg);
    }

    protected final void _wrapError(String msg, Throwable t)
        throws JsonParseException
    {
        throw _constructError(msg, t);
    }

    protected final void _throwInternal()
    {
        throw new RuntimeException("Internal error: this code path should never get executed");
    }

    protected final JsonParseException _constructError(String msg, Throwable t)
    {
        return new JsonParseException(msg, getCurrentLocation(), t);
    }
}
