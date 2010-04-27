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
    public final static int MAX_SHORT_STRING_BYTES = 63;

    /**
     * And to make encoding logic tight and simple, we can always
     * require that output buffer has this amount of space
     * available before encoding possibly short String (3 bytes since
     * longest UTF-8 encoded Java char is 3 bytes).
     * Two extra bytes need to be reserved as well; first for token indicator,
     * and second for terminating null byte (in case it's not a short String after all)
     */
    public final static int MIN_BUFFER_FOR_POSSIBLE_SHORT_STRING = 1 + (3 * MAX_SHORT_STRING_BYTES);

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

    public final static int TOKEN_PREFIX_VAR_LENGTH_MISC = 0x060;
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

    // and "real" END_OBJECT is chosen not to overlap, on purpose
    public final static byte TOKEN_LITERAL_END_OBJECT = 0x36;

    /*
    /****************************************************
    /* Single-byte token type identifiers for numbers
    /****************************************************
     */
    
    public final static byte TOKEN_NUMBER_FLOAT = 0x28;
    public final static byte TOKEN_NUMBER_DOUBLE = 0x29;

    public final static byte TOKEN_NUMBER_BYTE = 0x2A;    
    public final static byte TOKEN_NUMBER_SHORT = 0x2B;
    public final static byte TOKEN_NUMBER_INT = 0x2C;
    public final static byte TOKEN_NUMBER_LONG = 0x2D;

    /*
    /****************************************************
    /* Subtype constants
    /****************************************************
     */
    
    // // // Numbers: 3 bits between 3 MSB and 2 LSB
    // // // (note: currently one bit unused, effectively)
    
    /**
     * Bit #4 (0x10) indicates whether misc type is binary (set)
     * or other (not set)
     */
    public final static int TOKEN_MISC_BIT_BINARY = 0x10;

    /**
     * Subtype (for misc, other) used for variable length
     * UTF-8 encoded text
     */
    public final static int TOKEN_MISC_SUBTYPE_LONG_TEXT = 0x00;

    /**
     * Subtype (for misc, other) used for variable length
     * {@link java.math.BigInteger} representation
     */
    public final static int TOKEN_MISC_SUBTYPE_BIG_INTEGER = 0x04;

    /**
     * Subtype (for misc, other) used for variable length
     * {@link java.math.BigDecimal} representation
     */
    public final static int TOKEN_MISC_SUBTYPE_BIG_DECIMAL = 0x08;

    // Note: subtype with code 0x0C is reserved for future use
    
    /*
    /****************************************************
    /* Length suffix (2 LSB)
    /****************************************************
     */

    public final static int TOKEN_LENGTH_IND_8B = 0x00;
    public final static int TOKEN_LENGTH_IND_16B = 0x01;
    public final static int TOKEN_LENGTH_IND_32B = 0x02;
    public final static int TOKEN_LENGTH_IND_64B = 0x03;

    /*
    /****************************************************
    /* Compression indicator suffix (2 LSB)
    /****************************************************
     */

    public final static int TOKEN_COMP_TYPE_NONE = 0x00;
    public final static int TOKEN_COMP_TYPE_LZF = 0x01;
}
