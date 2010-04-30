package org.codehaus.jackson.smile;

/**
 * Constants used by {@link SmileGenerator} and {@link SmileParser}
 * 
 * @author tatu
 */
public interface SmileConstants
{
    /*
    /****************************************************
    /* Thresholds
    /****************************************************
     */

    /**
     * Encoding has special "short" forms for Strings that can
     * be represented by 63 bytes of UTF-8 or less.
     */
    public final static int MAX_SHORT_STRING_BYTES = 64;

    /**
     * And to make encoding logic tight and simple, we can always
     * require that output buffer has this amount of space
     * available before encoding possibly short String (3 bytes since
     * longest UTF-8 encoded Java char is 3 bytes).
     * Two extra bytes need to be reserved as well; first for token indicator,
     * and second for terminating null byte (in case it's not a short String after all)
     */
    public final static int MIN_BUFFER_FOR_POSSIBLE_SHORT_STRING = 1 + (3 * MAX_SHORT_STRING_BYTES);

    /**
     * We need a byte marker to denote end of variable-length Strings. Although
     * null byte is commonly used, let's try to avoid using it since it can't
     * be embedded in WebSockets content (similarly, 0xFF can't). There are
     * multiple candidates for bytes UTF-8 can not have; 0xFE seems like a
     * good choice
     */
    public final static byte BYTE_STRING_END_MARKER = (byte) 0xFE;
    
    /*
    /****************************************************
    /* Type prefixes: 3 MSB of token byte
    /****************************************************
     */
    
    // Shared strings are back references for last 63 short (< 64 byte) string values
    // NOTE: 0x00 is reserved, not used
    public final static int TOKEN_PREFIX_SHORT_SHARED_STRING = 0x00;
    public final static int TOKEN_PREFIX_LITERAL = 0x20;
    // Small ints are 4-bit (-16 to +15) integer constants
    public final static int TOKEN_PREFIX_SMALL_INT = 0x40;

    public final static int TOKEN_PREFIX_MISC_TYPES = 0x060;
    public final static int TOKEN_PREFIX_TINY_ASCII = 0x80;
    public final static int TOKEN_PREFIX_SMALL_ASCII = 0xA0;
    public final static int TOKEN_PREFIX_TINY_UNICODE = 0xC0;
    public final static int TOKEN_PREFIX_SHORT_UNICODE = 0xE0;
    
    /*
    /****************************************************
    /* Token literals, normal mode
    /****************************************************
     */
    
    public final static byte TOKEN_LITERAL_START_OBJECT = 0x20;
    // NOTE: NO END_OBJECT in normal mode since it can only occur instead of field name.
    // Slot reserved however.
    public final static byte TOKEN_LITERAL_START_ARRAY = 0x22;
    public final static byte TOKEN_LITERAL_END_ARRAY = 0x23;
    public final static byte TOKEN_LITERAL_FALSE = 0x24;
    public final static byte TOKEN_LITERAL_TRUE = 0x25;
    public final static byte TOKEN_LITERAL_NULL = 0x26;
    public final static byte TOKEN_LITERAL_EMPTY_STRING = 0x27;

    /**
     * Stand-alone value token that represents smallest possible
     * 64-bit integer number (negative number with largest absolute
     * value). Defined separately because it has no positive counterpart,
     * and thus its handling using general VInt mechanism is tricky.
     * Using separate type is bit wasteful, but we do have individual
     * values to spare at this point.
     */
    public final static byte TOKEN_LITERAL_MIN_64BIT_LONG = 0x28;

    // and "real" END_OBJECT is chosen not to overlap, on purpose
    public final static byte TOKEN_LITERAL_END_OBJECT = 0x36;

    /*
    /****************************************************
    /* Subtype constants for "Misc types" -- 3 bit
    /****************************************************
     */
    
    /**
     * Type (for misc, other) used for
     * variable length UTF-8 encoded text, when it is known to only contain ASCII chars
     */
    public final static int TOKEN_MISC_LONG_TEXT_ASCII = 0x00;

    /**
     * Type (for misc, other) used
     * for variable length UTF-8 encoded text, when it is NOT known to only contain ASCII chars
     * (which means it MAY have multi-byte characters)
     */
    public final static int TOKEN_MISC_LONG_TEXT_UNICODE = 0x04;
    
    /**
     * Type (for misc, other) used
     * for "raw" (embedded as-is) binary data.
     */
    public final static int TOKEN_MISC_BINARY_RAW = 0x08;

    /**
     * Type (for misc, other) used
     * for "safe" (encoded by only using 7 LSB, giving 8/7 expansion ratio).
     * This is usually done to ensure that certain bytes are never included
     * in encoded data (like 0xFF)
     */
    public final static int TOKEN_MISC_BINARY_7BIT = 0x0C;

    /**
     * Type (for misc, other) used
     * for regular integral types (byte/short/int/long)
     */
    public final static int TOKEN_MISC_INTEGER = 0x10;

    /**
     * Type (for misc, other) used 
     * for regular floating-point types (float, double)
     */
    public final static int TOKEN_MISC_FLOATING_POINT = 0x14;
    
    /**
     * Type (for misc, other) used for "big" numeric types
     * ({@link java.math.BigDecimal}, {@link java.math.BigInteger})
     */
    public final static int TOKEN_MISC_BIG_NUMBER = 0x18;

    // Note: subtype with code 0x1C is reserved for future use

    /*
    /****************************************************
    /* Modifiers for misc entries
    /****************************************************
     */

    /**
     * LSB of token type for {@link #TOKEN_MISC_INTEGER} is
     * used to indicate sign; if set (1) number is negative,
     * if unset (0), positive.
     */
    public final static int TOKEN_MISC_INTEGER_SIGN = 0x01;
    
    /**
     * LSB of token type for {@link #TOKEN_MISC_FLOATING_POINT}
     * is used to indicate precisions: if set (1) it is
     * double-precision (64-bit IEEE) value, if unset (0),
     * single-precision (32-bit IEEE).
     */
    public final static int TOKEN_MISC_FP_DOUBLE_PRECISION = 0x01;

    /**
     * LSB of token type for {@link #TOKEN_MISC_BIG_NUMBER}
     * is used to indicate whether it is a
     * {@link java.math.BigDecimal} or not ({@link java.math.BigInteger}).
     * If set (1) value is <code>BigDecimal</code>, if unset (0),
     * <code>BigInteger</code>
     */
    public final static int TOKEN_MISC_BIG_NUMBER_DECIMAL = 0x01;
    
    /*
    /****************************************************
    /* Token types for keys
    /****************************************************
     */

    public final static byte TOKEN_KEY_LONG_STRING = 0x34;

    public final static byte TOKEN_KEY_EMPTY_STRING = 0x35;
    
    /*
    /****************************************************
    /* Compression indicator suffix (2 LSB)
    /****************************************************
     */

    public final static int TOKEN_COMP_TYPE_NONE = 0x00;
    public final static int TOKEN_COMP_TYPE_LZF = 0x01;
}

