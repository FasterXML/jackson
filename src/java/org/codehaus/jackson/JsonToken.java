package org.codehaus.jackson;

/**
 * This enumeration defines basic token types used for returning results
 * of parsing JSON content.
 */
public enum JsonToken
{
    /**
     * START_OBJECT is returned when encountering '{'
     * which signals starting of an Object value
     */
    START_OBJECT("{"),
        
    /**
     * START_OBJECT is returned when encountering '}'
     * which signals ending of an Object value
     */
        END_OBJECT("}"),
        
    /**
     * START_OBJECT is returned when encountering '['
     * which signals starting of an Array value
     */
        START_ARRAY("["),

    /**
     * START_OBJECT is returned when encountering ']'
     * which signals ending of an Array value
     */
        END_ARRAY("]"),
        
     /**
      * FIELD_NAME is returned when a String token is encountered
      * as a field name (same lexical value, different function)
      */
        FIELD_NAME(null),
        
     /**
      * VALUE_STRING is returned when a String token is encountered
      * in value context (array element, field value, or root-level
      * stand-alone value)
      */
        VALUE_STRING(null),

     /**
      * VALUE_NUMBER_INT is returned when an integer numeric token is
      * encountered in value context: that is, a number that does
      * not have floating point or exponent marker in it (consists
      * only of an optional sign, followed by one or more digits)
      */
        VALUE_NUMBER_INT(null),

    /**
     * VALUE_NUMBER_INT is returned when a numeric token other
     * that is not an integer is encountered: that is, a number that does
     * have floating point or exponent marker in it, in addition
     * to one or more digits.
     */
        VALUE_NUMBER_FLOAT(null),

    /**
     * VALUE_TRUE is returned when encountering literal "true" in
     * value context
     */
        VALUE_TRUE("true"),

    /**
     * VALUE_FALSE is returned when encountering literal "false" in
     * value context
     */
        VALUE_FALSE("false"),

    /**
     * VALUE_NULL is returned when encountering literal "null" in
     * value context
     */
        VALUE_NULL("null");

    final String mSerialized;

    final char[] mSerializedChars;

    final byte[] mSerializedBytes;

    /**
     * @param Textual representation for this token, if there is a
     *   single static representation; null otherwise
     */
    JsonToken(String token)
    {
        if (token == null) {
            mSerialized = null;
            mSerializedChars = null;
            mSerializedBytes = null;
        } else {
            mSerialized = token;
            mSerializedChars = token.toCharArray();
            // It's all in ascii, can just case...
            int len = mSerializedChars.length;
            mSerializedBytes = new byte[len];
            for (int i = 0; i < len; ++i) {
                mSerializedBytes[i] = (byte) mSerializedChars[i];
            }
        }
    }

    public String asString() { return mSerialized; }
    public char[] asCharArray() { return mSerializedChars; }
    public byte[] asByteArray() { return mSerializedBytes; }

    public boolean isNumeric() {
        return (this == VALUE_NUMBER_INT) || (this == VALUE_NUMBER_FLOAT);
    }
}
