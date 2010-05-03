package org.codehaus.jackson.smile;

/**
 * Class for miscellaneous helper methods.
 */
public class SmileUtil
{
    public static int zigzagEncode(int input) {
        // Canonical version:
        //return (input << 1) ^  (input >> 31);
        // but this is even better
        if (input < 0) {
            return (input << 1) ^ -1;
        }
        return (input << 1);
    }

    public static int zigzagDecode(int encoded) {
        // canonical:
        //return (encoded >>> 1) ^ (-(encoded & 1));
        if ((encoded & 1) == 0) { // positive
            return (encoded >>> 1);
        }
        // negative
        return (encoded >>> 1) ^ -1;
    }
    
    public static long zigzagEncode(long input) {
        // Canonical version
        //return (input << 1) ^  (input >> 63);
        if (input < 0L) {
            return (input << 1) ^ -1L;
        }
        return (input << 1);
    }

    public static long zigzagDecode(long encoded) {
        // canonical:
        //return (encoded >>> 1) ^ (-(encoded & 1));
        if ((encoded & 1) == 0) { // positive
            return (encoded >>> 1);
        }
        // negative
        return (encoded >>> 1) ^ -1L;
    }
}
