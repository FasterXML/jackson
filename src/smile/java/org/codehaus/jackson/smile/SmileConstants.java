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
    /* Token literals, normal mode
    /****************************************************
     */
    
    public final static byte TOKEN_LITERAL_START_OBJECT = 0x20;
    // NOTE: NO END_OBJECT in normal mode since it can only occur instead of field name.
    // Slot reserved however.
    public final static byte TOKEN_LITERAL_START_ARRAY = 0x22;
    public final static byte TOKEN_LITERAL_END_ARRAY = 0x23;
    public final static byte TOKEN_LITERAL_TRUE = 0x24;
    public final static byte TOKEN_LITERAL_FALSE = 0x25;
    public final static byte TOKEN_LITERAL_NULL = 0x26;
    public final static byte TOKEN_LITERAL_EMPTY_STRING = 0x27;

    // and "real" END_OBJECT is chosen not to overlap, on purpose
    public final static byte TOKEN_NAME_LITERAL_END_OBJECT = 0x30;
    
    /*
    /****************************************************
    /* Type prefixes: 3 MSB of token byte
    /****************************************************
     */
    
    public final static int TOKEN_PREFIX_SHORT_ASCII = 0x00;
    public final static int TOKEN_PREFIX_LITERAL = 0x20;
    public final static int TOKEN_PREFIX_SHORT_UNICODE = 0x40;
    public final static int TOKEN_PREFIX_SHARED_STRING = 0x60;
    public final static int TOKEN_PREFIX_SMALL_INT = 0x80;
    public final static int TOKEN_PREFIX_TEXT = 0xA0;
    public final static int TOKEN_PREFIX_NUMBER = 0xC0;
    public final static int TOKEN_PREFIX_BINARY = 0xE0;

    /*
    /****************************************************
    /* Subtype constants
    /****************************************************
     */
    
    // // // Numbers: 3 bits between 3 MSB and 2 LSB
    // // // (note: currently one bit unused, effectively)
    
    /**
     * Type used for integral numbers: byte/short/int/long.
     */
    public final static int TOKEN_SUBTYPE_NR_INTEGRAL = 0x00;

    /**
     * Type used for fixed-length floating point numbers;
     * float and double
     */
    public final static int TOKEN_SUBTYPE_NR_FLOAT = 0x01;

    /**
     * Type used for variable length {@link java.math.BigInteger}.
     */
    public final static int TOKEN_SUBTYPE_BIG_INTEGER = 0x02;

    /**
     * Type used for variable length {@link java.math.BigDecimal}.
     */
    public final static int TOKEN_SUBTYPE_BIG_DECIMAL = 0x03; 
    
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
    /* Convenient short cuts to type + length
    /****************************************************
     */

    public final static byte TOKEN_NUMBER_8B_INT = (byte) (TOKEN_PREFIX_NUMBER + TOKEN_SUBTYPE_NR_INTEGRAL + TOKEN_LENGTH_IND_8B);
    public final static byte TOKEN_NUMBER_16B_INT = (byte) (TOKEN_PREFIX_NUMBER + TOKEN_SUBTYPE_NR_INTEGRAL + TOKEN_LENGTH_IND_16B);
    public final static byte TOKEN_NUMBER_32B_INT = (byte) (TOKEN_PREFIX_NUMBER + TOKEN_SUBTYPE_NR_INTEGRAL + TOKEN_LENGTH_IND_32B);
    public final static byte TOKEN_NUMBER_64B_INT = (byte) (TOKEN_PREFIX_NUMBER + TOKEN_SUBTYPE_NR_INTEGRAL + TOKEN_LENGTH_IND_64B);

    public final static byte TOKEN_NUMBER_FLOAT = (byte) (TOKEN_PREFIX_NUMBER + TOKEN_SUBTYPE_NR_FLOAT + TOKEN_LENGTH_IND_32B);
    public final static byte TOKEN_NUMBER_DOUBLE = (byte) (TOKEN_PREFIX_NUMBER + TOKEN_SUBTYPE_NR_FLOAT + TOKEN_LENGTH_IND_64B);

}
