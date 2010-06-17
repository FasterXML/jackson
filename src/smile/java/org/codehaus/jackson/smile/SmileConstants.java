package org.codehaus.jackson.smile;

/**
 * Constants used by {@link SmileGenerator} and {@link SmileParser}
 * 
 * @author tatu
 */
public final class SmileConstants
{
    /*
    /**********************************************************
    /* Thresholds
    /**********************************************************
     */

    /**
     * Encoding has special "short" forms for Strings that can
     * be represented by 64 bytes of UTF-8 or less.
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

    /*
    /**********************************************************
    /* Byte markers
    /**********************************************************
     */
    
    /**
     * We need a byte marker to denote end of variable-length Strings. Although
     * null byte is commonly used, let's try to avoid using it since it can't
     * be embedded in Web Sockets content (similarly, 0xFF can't). There are
     * multiple candidates for bytes UTF-8 can not have; 0xFE seems like a
     * good choice
     */
    public final static byte BYTE_MARKER_END_OF_STRING = (byte) 0xFE;

    /**
     * In addition we can use a marker to allow simple framing; splitting
     * of physical data (like file) into distinct logical sections like
     * JSON documents. 0xFF makes sense here since it is also used
     * as end marker for Web Sockets.
     */
    public final static byte BYTE_MARKER_END_OF_CONTENT = (byte) 0xFF;

    /*
    /**********************************************************
    /* Format header: put smile on your data...
    /**********************************************************
     */

    /**
     * First byte of data header
     */
    public final static byte HEADER_BYTE_1 = (byte) ':';

    /**
     * Second byte of data header
     */
    public final static byte HEADER_BYTE_2 = (byte) ')';

    /**
     * Third byte of data header
     */
    public final static byte HEADER_BYTE_3 = (byte) '\n';

    /**
     * Fourth byte of data header
     */
    public final static byte HEADER_BYTE_4 = (byte) 0;

    /**
     * Current version consists of two zero bits.
     */
    public final static int HEADER_VERSION_00 = 0x0;
    
    /*
    /**********************************************************
    /* Type prefixes: 3 MSB of token byte
    /**********************************************************
     */
    
    // Shared strings are back references for last 63 short (< 64 byte) string values
    // NOTE: 0x00 is reserved, not used
    public final static int TOKEN_PREFIX_SHORT_SHARED_STRING = 0x00;
    // literals are put between 0x20 and 0x3F to reserve markers (smiley)
    public final static int TOKEN_PREFIX_LITERAL = 0x20;

    public final static int TOKEN_PREFIX_TINY_ASCII = 0x40;
    public final static int TOKEN_PREFIX_SMALL_ASCII = 0x60;
    public final static int TOKEN_PREFIX_TINY_UNICODE = 0x80;
    public final static int TOKEN_PREFIX_SHORT_UNICODE = 0xA0;

    // Small ints are 4-bit (-16 to +15) integer constants
    public final static int TOKEN_PREFIX_SMALL_INT = 0xC0;

    // And misc types have empty at the end too, to reserve 0xF8 - 0xFF
    public final static int TOKEN_PREFIX_MISC_TYPES = 0xE0;

    /*
    /**********************************************************
    /* Token literals, normal mode
    /**********************************************************
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
    /**********************************************************
    /* Subtype constants for "Misc types" -- 3 bit
    /**********************************************************
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
    public final static int TOKEN_MISC_OTHER_NUMBER = 0x14;
    
    /* Note: subtypes with code 0x1C can not be used since
     * that overlaps with 0xFE and 0xFF; 18 is reserved
     * to reserve 0xF8 - 0xFB for future expansion.
     */

    /*
    /**********************************************************
    /* Modifiers for misc entries
    /**********************************************************
     */
    
    /**
     * Numeric subtype (2 LSB) for {@link #TOKEN_MISC_OTHER_NUMBER},
     * indicating 32-bit IEEE single precision floating point number.
     */
    public final static int TOKEN_MISC_NUMBER_FLOAT = 0x00;

    /**
     * Numeric subtype (2 LSB) for {@link #TOKEN_MISC_OTHER_NUMBER},
     * indicating 64-bit IEEE double precision floating point number.
     */
    public final static int TOKEN_MISC_NUMBER_DOUBLE = 0x01;

    /**
     * Numeric subtype (2 LSB) for {@link #TOKEN_MISC_OTHER_NUMBER},
     * indicating {@link java.math.BigInteger} type.
     */
    public final static int TOKEN_MISC_NUMBER_BIG_INTEGER = 0x02;

    /**
     * Numeric subtype (2 LSB) for {@link #TOKEN_MISC_OTHER_NUMBER},
     * indicating {@link java.math.BigDecimal} type.
     */
    public final static int TOKEN_MISC_NUMBER_BIG_DECIMAL = 0x03;
    
    /*
    /**********************************************************
    /* Token types for keys
    /**********************************************************
     */

    public final static byte TOKEN_KEY_LONG_STRING = 0x34;

    public final static byte TOKEN_KEY_EMPTY_STRING = 0x35;

    public final static int TOKEN_PREFIX_KEY_ASCII = 0x80;

    public final static int TOKEN_PREFIX_KEY_UNICODE = 0xC0;
    /*
    /**********************************************************
    /* Compression indicator suffix (2 LSB)
    /**********************************************************
     */

    public final static int TOKEN_COMP_TYPE_NONE = 0x00;
    public final static int TOKEN_COMP_TYPE_LZF = 0x01;

    /*
    /**********************************************************
    /* Basic UTF-8 decode/encode table
    /**********************************************************
     */
    
    /**
     * Additionally we can combine UTF-8 decoding info into similar
     * data table.
     * Values indicate "byte length - 1"; meaning -1 is used for
     * invalid bytes, 0 for single-byte codes, 1 for 2-byte codes
     * and 2 for 3-byte codes.
     */
    public final static int[] sUtf8UnitLengths;
    static {
        int[] table = new int[256];
        for (int c = 128; c < 256; ++c) {
            int code;

            // We'll add number of bytes needed for decoding
            if ((c & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
                code = 1;
            } else if ((c & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
                code = 2;
            } else if ((c & 0xF8) == 0xF0) {
                // 4 bytes; double-char with surrogates and all...
                code = 3;
            } else {
                // And -1 seems like a good "universal" error marker...
                code = -1;
            }
            table[c] = code;
        }
        sUtf8UnitLengths = table;
    }
}

