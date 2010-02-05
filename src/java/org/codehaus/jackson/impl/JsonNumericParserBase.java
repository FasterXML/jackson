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

    final static long MIN_INT_L = (long) Integer.MIN_VALUE;
    final static long MAX_INT_L = (long) Integer.MAX_VALUE;

    // These are not very accurate, but have to do... (for bounds checks)

    final static double MIN_LONG_D = (double) Long.MIN_VALUE;
    final static double MAX_LONG_D = (double) Long.MAX_VALUE;

    final static double MIN_INT_D = (double) Integer.MIN_VALUE;
    final static double MAX_INT_D = (double) Integer.MAX_VALUE;
    
    
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
    final protected static int INT_PLUS = '+';
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
    protected int _numTypesValid = NR_UNKNOWN;

    // First primitives

    protected int _numberInt;

    protected long _numberLong;

    protected double _numberDouble;

    // And then object types

    protected BigInteger _numberBigInt;

    protected BigDecimal _numberBigDecimal;

    // And then other information about value itself

    /**
     * Flag that indicates whether numeric value has a negative
     * value. That is, whether its textual representation starts
     * with minus character.
     */
    protected boolean _numberNegative;

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
        _numberNegative = negative;
        mIntLength = intLen;
        mFractLength = fractLen;
        mExpLength = expLen;
        _numTypesValid = NR_UNKNOWN; // to force parsing
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
        if (_numTypesValid == NR_UNKNOWN) {
            parseNumericValue(NR_UNKNOWN); // will also check event type
        }
        // Separate types for int types
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((_numTypesValid & NR_INT) != 0) {
                return Integer.valueOf(_numberInt);
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return Long.valueOf(_numberLong);
            }
            if ((_numTypesValid & NR_BIGINT) != 0) {
                return _numberBigInt;
            }
            // Shouldn't get this far but if we do
            return _numberBigDecimal;
        }

        /* And then floating point types. But here optimal type
         * needs to be big decimal, to avoid losing any data?
         */
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return _numberBigDecimal;
        }
        if ((_numTypesValid & NR_DOUBLE) == 0) { // sanity check
            _throwInternal();
        }
        return Double.valueOf(_numberDouble);
    }

    public NumberType getNumberType()
        throws IOException, JsonParseException
    {
        if (_numTypesValid == NR_UNKNOWN) {
            parseNumericValue(NR_UNKNOWN); // will also check event type
        }
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((_numTypesValid & NR_INT) != 0) {
                return NumberType.INT;
            }
            if ((_numTypesValid & NR_LONG) != 0) {
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
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return NumberType.BIG_DECIMAL;
        }
        return NumberType.DOUBLE;
    }

    public int getIntValue()
        throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_INT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) { // not parsed at all
                parseNumericValue(NR_INT); // will also check event type
            }
            if ((_numTypesValid & NR_INT) == 0) { // wasn't an int natively?
                convertNumberToInt(); // let's make it so, if possible
            }
        }
        return _numberInt;
    }

    public long getLongValue()
        throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_LONG) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                parseNumericValue(NR_LONG);
            }
            if ((_numTypesValid & NR_LONG) == 0) {
                convertNumberToLong();
            }
        }
        return _numberLong;
    }

    public BigInteger getBigIntegerValue()
        throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_BIGINT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                parseNumericValue(NR_BIGINT);
            }
            if ((_numTypesValid & NR_BIGINT) == 0) {
                convertNumberToBigInteger();
            }
        }
        return _numberBigInt;
    }

    public float getFloatValue()
        throws IOException, JsonParseException
    {
        double value = getDoubleValue();
        /* 22-Jan-2009, tatu: Bounds/range checks would be tricky
         *   here, so let's not bother even trying...
         */
        /*
        if (value < -Float.MAX_VALUE || value > MAX_FLOAT_D) {
            _reportError("Numeric value ("+getText()+") out of range of Java float");
        }
        */
        return (float) value;
    }

    public double getDoubleValue()
        throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_DOUBLE) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                parseNumericValue(NR_DOUBLE);
            }
            if ((_numTypesValid & NR_DOUBLE) == 0) {
                convertNumberToDouble();
            }
        }
        return _numberDouble;
    }

    public BigDecimal getDecimalValue()
        throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                parseNumericValue(NR_BIGDECIMAL);
            }
            if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
                convertNumberToBigDecimal();
            }
        }
        return _numberBigDecimal;
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
                int len = mIntLength;
                if (_numberNegative) {
                    ++offset;
                }
                if (len <= 9) { // definitely fits in int
                    int i = NumberInput.parseInt(buf, offset, len);
                    _numberInt = _numberNegative ? -i : i;
                    _numTypesValid = NR_INT;
                    return;
                }
                if (len <= 18) { // definitely fits AND is easy to parse using 2 int parse calls
                    long l = NumberInput.parseLong(buf, offset, len);
                    if (_numberNegative) {
                        l = -l;
                    }
                    // [JACKSON-230] Could still fit in int, need to check
                    if (len == 10) {
                        if (_numberNegative) {
                            if (l >= MIN_INT_L) {
                                _numberInt = (int) l;
                                _numTypesValid = NR_INT;
                                return;
                            }
                        } else {
                            if (l <= MAX_INT_L) {
                                _numberInt = (int) l;
                                _numTypesValid = NR_INT;
                                return;
                            }
                        }
                    }
                    _numberLong = l;
                    _numTypesValid = NR_LONG;
                    return;
                }
                String numStr = _textBuffer.contentsAsString();
                // [JACKSON-230] Some long cases still...
                if (NumberInput.inLongRange(buf, offset, len, _numberNegative)) {
                    // Probably faster to construct a String, call parse, than to use BigInteger
                    _numberLong = Long.parseLong(numStr);
                    _numTypesValid = NR_LONG;
                    return;
                }
                // nope, need the heavy guns... (rare case)
                _numberBigInt = new BigInteger(numStr);
                _numTypesValid = NR_BIGINT;
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
                _numberBigDecimal = _textBuffer.contentsAsDecimal();
                _numTypesValid = NR_BIGDECIMAL;
            } else {
                // Otherwise double has to do
                _numberDouble = _textBuffer.contentsAsDouble();
                _numTypesValid = NR_DOUBLE;
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
        if ((_numTypesValid & NR_LONG) != 0) {
            // Let's verify it's lossless conversion by simple roundtrip
            int result = (int) _numberLong;
            if (((long) result) != _numberLong) {
                _reportError("Numeric value ("+getText()+") out of range of int");
            }
            _numberInt = result;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            // !!! Should check for range...
            _numberInt = _numberBigInt.intValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (_numberDouble < MIN_INT_D || _numberDouble > MAX_INT_D) {
                reportOverflowInt();
            }
            _numberInt = (int) _numberDouble;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            if (BD_MIN_INT.compareTo(_numberBigDecimal) > 0 
                || BD_MAX_INT.compareTo(_numberBigDecimal) < 0) {
                reportOverflowInt();
            }
            _numberInt = _numberBigDecimal.intValue();
        } else {
            _throwInternal(); // should never get here
        }

        _numTypesValid |= NR_INT;
    }

    protected void convertNumberToLong()
        throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_INT) != 0) {
            _numberLong = (long) _numberInt;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            // !!! Should check for range...
            _numberLong = _numberBigInt.longValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (_numberDouble < MIN_LONG_D || _numberDouble > MAX_LONG_D) {
                reportOverflowLong();
            }
            _numberLong = (long) _numberDouble;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            if (BD_MIN_LONG.compareTo(_numberBigDecimal) > 0 
                || BD_MAX_LONG.compareTo(_numberBigDecimal) < 0) {
                reportOverflowLong();
            }
            _numberLong = _numberBigDecimal.longValue();
        } else {
            _throwInternal(); // should never get here
        }

        _numTypesValid |= NR_LONG;
    }

    protected void convertNumberToBigInteger()
        throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            // here it'll just get truncated, no exceptions thrown
            _numberBigInt = _numberBigDecimal.toBigInteger();
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberInt);
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            _numberBigInt = BigDecimal.valueOf(_numberDouble).toBigInteger();
        } else {
            _throwInternal(); // should never get here
        }
        _numTypesValid |= NR_BIGINT;
    }

    protected void convertNumberToDouble()
        throws IOException, JsonParseException
    {
        /* 05-Aug-2008, tatus: Important note: this MUST start with
         *   more accurate representations, since we don't know which
         *   value is the original one (others get generated when
         *   requested)
         */

        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            _numberDouble = _numberBigDecimal.doubleValue();
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            _numberDouble = _numberBigInt.doubleValue();
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberDouble = (double) _numberLong;
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberDouble = (double) _numberInt;
        } else {
            _throwInternal(); // should never get here
        }

        _numTypesValid |= NR_DOUBLE;
    }

    protected void convertNumberToBigDecimal()
        throws IOException, JsonParseException
    {
        /* 05-Aug-2008, tatus: Important note: this MUST start with
         *   more accurate representations, since we don't know which
         *   value is the original one (others get generated when
         *   requested)
         */

        if ((_numTypesValid & NR_DOUBLE) != 0) {
            /* Let's actually parse from String representation,
             * to avoid rounding errors that non-decimal floating operations
             * would incur
             */
            _numberBigDecimal = new BigDecimal(getText());
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            _numberBigDecimal = new BigDecimal(_numberBigInt);
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigDecimal = BigDecimal.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigDecimal = BigDecimal.valueOf((long) _numberInt);
        } else {
            _throwInternal(); // should never get here
        }
        _numTypesValid |= NR_BIGDECIMAL;
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
