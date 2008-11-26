package org.codehaus.jackson.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.io.NumberInput;

/**
 * Another intermediate base class used by all Jackson {@link JsonParser}
 * implementations. Contains shared functionality for dealing with
 * number parsing aspects, independent of input source decoding.
 *
 * @author Tatu Saloranta
 */
public abstract class JsonNumericParserBase
    extends JsonParserBase
{
    /* Additionally we need to be able to distinguish between
     * various numeric representations, since we try to use
     * the fastest one that works for given textual representation.
     */

    final protected static int NR_UNKNOWN = 0;

    // First, integer types

    final protected static int NR_INT = 0x0001;
    final protected static int NR_LONG = 0x0002;
    final protected static int NR_BIGINT = 0x0004;

    // And then floating point types

    final protected static int NR_DOUBLE = 0x008;
    final protected static int NR_BIGDECIMAL = 0x0010;

    // Also, we need some numeric constants

    final static BigDecimal BD_MIN_LONG = new BigDecimal(Long.MIN_VALUE);
    final static BigDecimal BD_MAX_LONG = new BigDecimal(Long.MAX_VALUE);

    final static BigDecimal BD_MIN_INT = new BigDecimal(Long.MIN_VALUE);
    final static BigDecimal BD_MAX_INT = new BigDecimal(Long.MAX_VALUE);

    // These are not very accurate, but have to do...
    // (note: non-final to prevent inlining)

    static double MIN_LONG_D = (double) Long.MIN_VALUE;
    static double MAX_LONG_D = (double) Long.MAX_VALUE;

    static double MIN_INT_D = (double) Integer.MIN_VALUE;
    static double MAX_INT_D = (double) Integer.MAX_VALUE;

    // Digits, numeric
    final protected static int INT_0 = '0';
    final protected static int INT_1 = '1';
    final protected static int INT_2 = '2';
    final protected static int INT_3 = '3';
    final protected static int INT_4 = '4';
    final protected static int INT_5 = '5';
    final protected static int INT_6 = '6';
    final protected static int INT_7 = '7';
    final protected static int INT_8 = '8';
    final protected static int INT_9 = '9';

    final protected static int INT_MINUS = '-';
    final protected static int INT_PLUS = '-';
    final protected static int INT_DECIMAL_POINT = '.';

    final protected static int INT_e = 'e';
    final protected static int INT_E = 'E';

    final protected static char CHAR_NULL = '\0';

    /*
    ////////////////////////////////////////////////////
    // Numeric value holders: multiple fields used for
    // for efficiency
    ////////////////////////////////////////////////////
     */

    /**
     * Bitfield that indicates which numeric representations
     * have been calculated for the current type
     */
    protected int mNumTypesValid = NR_UNKNOWN;

    // First primitives

    protected int mNumberInt;

    protected long mNumberLong;

    protected double mNumberDouble;

    // And then object types

    protected BigInteger mNumberBigInt;

    protected BigDecimal mNumberBigDecimal;

    // And then other information about value itself

    /**
     * Flag that indicates whether numeric value has a negative
     * value. That is, whether its textual representation starts
     * with minus character.
     */
    protected boolean mNumberNegative;

    /**
     * Length of integer part of the number, in characters
     */
    protected int mIntLength;

    /**
     * Length of the fractional part (not including decimal
     * point or exponent), in characters.
     * Not used for  pure integer values.
     */
    protected int mFractLength;

    /**
     * Length of the exponent part of the number, if any, not
     * including 'e' marker or sign, just digits. 
     * Not used for  pure integer values.
     */
    protected int mExpLength;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected JsonNumericParserBase(IOContext ctxt, int features)
    {
        super(ctxt, features);
    }

    protected final JsonToken reset(boolean negative, int intLen, int fractLen, int expLen)
    {
        mNumberNegative = negative;
        mIntLength = intLen;
        mFractLength = fractLen;
        mExpLength = expLen;
        mNumTypesValid = NR_UNKNOWN; // to force parsing
        if (fractLen < 1 && expLen < 1) { // integer
            return JsonToken.VALUE_NUMBER_INT;
        }
        // Nope, floating point
        return JsonToken.VALUE_NUMBER_FLOAT;
    }

    /*
    ////////////////////////////////////////////////////
    // Additional methods for sub-classes to implement
    ////////////////////////////////////////////////////
     */

    protected abstract JsonToken parseNumberText(int ch)
        throws IOException, JsonParseException;

    /*
    ////////////////////////////////////////////////////
    // Numeric accessors of public API
    ////////////////////////////////////////////////////
     */

    public Number getNumberValue()
        throws IOException, JsonParseException
    {
        if (mNumTypesValid == NR_UNKNOWN) {
            parseNumericValue(NR_UNKNOWN); // will also check event type
        }
        // Separate types for int types
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((mNumTypesValid & NR_INT) != 0) {
                return Integer.valueOf(mNumberInt);
            }
            if ((mNumTypesValid & NR_LONG) != 0) {
                return Long.valueOf(mNumberLong);
            }
            if ((mNumTypesValid & NR_BIGINT) != 0) {
                return mNumberBigInt;
            }
            // Shouldn't get this far but if we do
            return mNumberBigDecimal;
        }

        /* And then floating point types. But here optimal type
         * needs to be big decimal, to avoid losing any data?
         */
        if ((mNumTypesValid & NR_BIGDECIMAL) != 0) {
            return mNumberBigDecimal;
        }
        if ((mNumTypesValid & NR_DOUBLE) == 0) { // sanity check
            _throwInternal();
        }
        return Double.valueOf(mNumberDouble);
    }

    public NumberType getNumberType()
        throws IOException, JsonParseException
    {
        if (mNumTypesValid == NR_UNKNOWN) {
            parseNumericValue(NR_UNKNOWN); // will also check event type
        }
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((mNumTypesValid & NR_INT) != 0) {
                return NumberType.INT;
            }
            if ((mNumTypesValid & NR_LONG) != 0) {
                return NumberType.LONG;
            }
            return NumberType.BIG_INTEGER;
        }

        /* And then floating point types. Here optimal type
         * needs to be big decimal, to avoid losing any data?
         * However... using BD is slow, so let's allow returning
         * double as type if no explicit call has been made to access
         * data as BD?
         */
        if ((mNumTypesValid & NR_BIGDECIMAL) != 0) {
            return NumberType.BIG_DECIMAL;
        }
        return NumberType.DOUBLE;
    }

    public int getIntValue()
        throws IOException, JsonParseException
    {
        if ((mNumTypesValid & NR_INT) == 0) {
            if (mNumTypesValid == NR_UNKNOWN) { // not parsed at all
                parseNumericValue(NR_INT); // will also check event type
            }
            if ((mNumTypesValid & NR_INT) == 0) { // wasn't an int natively?
                convertNumberToInt(); // let's make it so, if possible
            }
        }
        return mNumberInt;
    }

    public long getLongValue()
        throws IOException, JsonParseException
    {
        if ((mNumTypesValid & NR_LONG) == 0) {
            if (mNumTypesValid == NR_UNKNOWN) {
                parseNumericValue(NR_LONG);
            }
            if ((mNumTypesValid & NR_LONG) == 0) {
                convertNumberToLong();
            }
        }
        return mNumberLong;
    }

    public double getDoubleValue()
        throws IOException, JsonParseException
    {
        if ((mNumTypesValid & NR_DOUBLE) == 0) {
            if (mNumTypesValid == NR_UNKNOWN) {
                parseNumericValue(NR_DOUBLE);
            }
            if ((mNumTypesValid & NR_DOUBLE) == 0) {
                convertNumberToDouble();
            }
        }
        return mNumberDouble;
    }

    public BigDecimal getDecimalValue()
        throws IOException, JsonParseException
    {
        if ((mNumTypesValid & NR_BIGDECIMAL) == 0) {
            if (mNumTypesValid == NR_UNKNOWN) {
                parseNumericValue(NR_BIGDECIMAL);
            }
            if ((mNumTypesValid & NR_BIGDECIMAL) == 0) {
                convertNumberToBigDecimal();
            }
        }
        return mNumberBigDecimal;
    }


    /*
    ////////////////////////////////////////////////////
    // Conversion from textual to numeric representation
    ////////////////////////////////////////////////////
     */

    /**
     * Method that will parse actual numeric value out of a syntactically
     * valid number value. Type it will parse into depends on whether
     * it is a floating point number, as well as its magnitude: smallest
     * legal type (of ones available) is used for efficiency.
     *
     * @param expType Numeric type that we will immediately need, if any;
     *   mostly necessary to optimize handling of floating point numbers
     */
    protected final void parseNumericValue(int expType)
        throws JsonParseException
    {
        // First things first: must be a numeric event
        if (_currToken == null || !_currToken.isNumeric()) {
            _reportError("Current token ("+_currToken+") not numeric, can not use numeric value accessors");
        }
        try {
            // Int or float?
            if (_currToken == JsonToken.VALUE_NUMBER_INT) {
                char[] buf = _textBuffer.getTextBuffer();
                int offset = _textBuffer.getTextOffset();
                if (mNumberNegative) {
                    ++offset;
                }
                if (mIntLength <= 9) { // definitely fits in int
                    int i = NumberInput.parseInt(buf, offset, mIntLength);
                    mNumberInt = mNumberNegative ? -i : i;
                    mNumTypesValid = NR_INT;
                    return;
                }
                if (mIntLength <= 18) { // definitely fits AND is easy to parse using 2 int parse calls
                    long l = NumberInput.parseLong(buf, offset, mIntLength);
                    mNumberLong = mNumberNegative ? -l : l;
                    mNumTypesValid = NR_LONG;
                    return;
                }
                // nope, need the heavy guns... (rare case)
                BigInteger bi = new BigInteger(_textBuffer.contentsAsString());
                mNumberBigDecimal = new BigDecimal(bi);
                mNumTypesValid = NR_BIGDECIMAL;
                return;
            }

            /* Nope: floating point. Here we need to be careful to get
             * optimal parsing strategy: choice is between accurate but
             * slow (BigDecimal) and lossy but fast (Double). For now
             * let's only use BD when explicitly requested -- it can
             * still be constructed correctly at any point since we do
             * retain textual representation
             */
            if (expType == NR_BIGDECIMAL) {
                mNumberBigDecimal = _textBuffer.contentsAsDecimal();
                mNumTypesValid = NR_BIGDECIMAL;
            } else {
                // Otherwise double has to do
                mNumberDouble = _textBuffer.contentsAsDouble();
                mNumTypesValid = NR_DOUBLE;
            }
        } catch (NumberFormatException nex) {
            // Can this ever occur? Due to overflow, maybe?
            _wrapError("Malformed numeric value '"+_textBuffer.contentsAsString()+"'", nex);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Conversions
    ////////////////////////////////////////////////////
     */    

    protected void convertNumberToInt()
        throws IOException, JsonParseException
    {
        // First, converting from long ought to be easy
        if ((mNumTypesValid & NR_LONG) != 0) {
            // Let's verify it's lossless conversion by simple roundtrip
            int result = (int) mNumberLong;
            if (((long) result) != mNumberLong) {
                _reportError("Numeric value ("+getText()+") out of range of int");
            }
            mNumberInt = result;
        } else if ((mNumTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (mNumberDouble < MIN_INT_D || mNumberDouble > MAX_INT_D) {
                reportOverflowInt();
            }
            mNumberInt = (int) mNumberDouble;
        } else if ((mNumTypesValid & NR_BIGDECIMAL) != 0) {
            if (BD_MIN_INT.compareTo(mNumberBigDecimal) > 0 
                || BD_MAX_INT.compareTo(mNumberBigDecimal) < 0) {
                reportOverflowInt();
            }
            mNumberInt = mNumberBigDecimal.intValue();
        } else {
            _throwInternal(); // should never get here
        }

        mNumTypesValid |= NR_INT;
    }

    protected void convertNumberToLong()
        throws IOException, JsonParseException
    {
        if ((mNumTypesValid & NR_INT) != 0) {
            mNumberLong = (long) mNumberInt;
        } else if ((mNumTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (mNumberDouble < MIN_LONG_D || mNumberDouble > MAX_LONG_D) {
                reportOverflowLong();
            }
            mNumberLong = (long) mNumberDouble;
        } else if ((mNumTypesValid & NR_BIGDECIMAL) != 0) {
            if (BD_MIN_LONG.compareTo(mNumberBigDecimal) > 0 
                || BD_MAX_LONG.compareTo(mNumberBigDecimal) < 0) {
                reportOverflowLong();
            }
            mNumberLong = mNumberBigDecimal.longValue();
        } else {
            _throwInternal(); // should never get here
        }

        mNumTypesValid |= NR_LONG;
    }

    protected void convertNumberToDouble()
        throws IOException, JsonParseException
    {
        /* 05-Aug-2008, tatus: Important note: this MUST start with
         *   more accurate representations, since we don't know which
         *   value is the original one (others get generated when
         *   requested)
         */

        if ((mNumTypesValid & NR_BIGDECIMAL) != 0) {
            mNumberDouble = mNumberBigDecimal.doubleValue();
        } else if ((mNumTypesValid & NR_LONG) != 0) {
            mNumberDouble = (double) mNumberLong;
        } else if ((mNumTypesValid & NR_INT) != 0) {
            mNumberDouble = (double) mNumberInt;
        } else {
            _throwInternal(); // should never get here
        }

        mNumTypesValid |= NR_DOUBLE;
    }

    protected void convertNumberToBigDecimal()
        throws IOException, JsonParseException
    {
        /* 05-Aug-2008, tatus: Important note: this MUST start with
         *   more accurate representations, since we don't know which
         *   value is the original one (others get generated when
         *   requested)
         */

        if ((mNumTypesValid & NR_DOUBLE) != 0) {
            /* Let's actually parse from String representation,
             * to avoid rounding errors that non-decimal floating operations
             * would incur
             */
            mNumberBigDecimal = new BigDecimal(getText());
        } else if ((mNumTypesValid & NR_LONG) != 0) {
            mNumberBigDecimal = BigDecimal.valueOf(mNumberLong);
        } else if ((mNumTypesValid & NR_INT) != 0) {
            mNumberBigDecimal = BigDecimal.valueOf((long) mNumberInt);
        } else {
            _throwInternal(); // should never get here
        }
        mNumTypesValid |= NR_BIGDECIMAL;
    }

    /*
    ////////////////////////////////////////////////////
    // Exception reporting
    ////////////////////////////////////////////////////
     */

    protected void reportUnexpectedNumberChar(int ch, String comment)
        throws JsonParseException
    {
        String msg = "Unexpected character ("+_getCharDesc(ch)+") in numeric value";
        if (comment != null) {
            msg += ": "+comment;
        }
        _reportError(msg);
    }

    protected void reportInvalidNumber(String msg)
        throws JsonParseException
    {
        _reportError("Invalid numeric value: "+msg);
    }


    protected void reportOverflowInt()
        throws IOException, JsonParseException
    {
        _reportError("Numeric value ("+getText()+") out of range of int ("+Integer.MIN_VALUE+" - "+Integer.MAX_VALUE+")");
    }

    protected void reportOverflowLong()
        throws IOException, JsonParseException
    {
        _reportError("Numeric value ("+getText()+") out of range of long ("+Long.MIN_VALUE+" - "+Long.MAX_VALUE+")");
    }

}
