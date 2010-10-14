package org.codehaus.jackson.io;

public final class NumberInput
{
    /**
     * Constants needed for parsing longs from basic int parsing methods
     */
    final static long L_BILLION = 1000000000;

    final static String MIN_LONG_STR_NO_SIGN = String.valueOf(Long.MIN_VALUE).substring(1);
    final static String MAX_LONG_STR = String.valueOf(Long.MAX_VALUE);
    
    /**
     * Fast method for parsing integers that are known to fit into
     * regular 32-bit signed int type. This means that length is
     * between 1 and 9 digits (inclusive)
     *<p>
     * Note: public to let unit tests call it
     */
    public final static int parseInt(char[] digitChars, int offset, int len)
    {
        int num = digitChars[offset] - '0';
        len += offset;
        // This looks ugly, but appears the fastest way:
        if (++offset < len) {
            num = (num * 10) + (digitChars[offset] - '0');
            if (++offset < len) {
                num = (num * 10) + (digitChars[offset] - '0');
                if (++offset < len) {
                    num = (num * 10) + (digitChars[offset] - '0');
                    if (++offset < len) {
                        num = (num * 10) + (digitChars[offset] - '0');
                        if (++offset < len) {
                            num = (num * 10) + (digitChars[offset] - '0');
                            if (++offset < len) {
                                num = (num * 10) + (digitChars[offset] - '0');
                                if (++offset < len) {
                                    num = (num * 10) + (digitChars[offset] - '0');
                                    if (++offset < len) {
                                        num = (num * 10) + (digitChars[offset] - '0');
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return num;
    }

    public final static long parseLong(char[] digitChars, int offset, int len)
    {
        // Note: caller must ensure length is [10, 18]
        int len1 = len-9;
        long val = parseInt(digitChars, offset, len1) * L_BILLION;
        return val + (long) parseInt(digitChars, offset+len1, 9);
    }

    /**
     * Helper method for determining if given String representation of
     * an integral number would fit in 64-bit Java long or not.
     * Note that input String must NOT contain leading minus sign (even
     * if 'negative' is set to true).
     *
     * @param negative Whether original number had a minus sign (which is
     *    NOT passed to this method) or not
     */
    public final static boolean inLongRange(char[] digitChars, int offset, int len,
            boolean negative)
    {
        String cmpStr = negative ? MIN_LONG_STR_NO_SIGN : MAX_LONG_STR;
        int cmpLen = cmpStr.length();
        if (len < cmpLen) return true;
        if (len > cmpLen) return false;

        for (int i = 0; i < cmpLen; ++i) {
            if (digitChars[offset+i] > cmpStr.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Similar to {@link #inLongRange(char[],int,int,boolean)}, but
     * with String argument
     *
     * @param negative Whether original number had a minus sign (which is
     *    NOT passed to this method) or not
     *
     * @since 1.5.0
     */
    public final static boolean inLongRange(String numberStr, boolean negative)
    {
        String cmpStr = negative ? MIN_LONG_STR_NO_SIGN : MAX_LONG_STR;
        int cmpLen = cmpStr.length();
        int actualLen = numberStr.length();
        if (actualLen < cmpLen) return true;
        if (actualLen > cmpLen) return false;

        // could perhaps just use String.compareTo()?
        for (int i = 0; i < cmpLen; ++i) {
            if (numberStr.charAt(i) > cmpStr.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @since 1.6
     */
    public static int parseAsInt(String input, int defaultValue)
    {
        if (input == null) {
            return defaultValue;
        }
        input = input.trim();
        int len = input.length();
        if (len == 0) {
            return defaultValue;
        }
        // One more thing: use integer parsing for 'simple'
        int i = 0;
        if (i < len) { // skip leading sign:
            char c = input.charAt(0);
            if (c == '+') { // for plus, actually physically remove
                input = input.substring(1);
                len = input.length();
            } else if (c == '-') { // minus, just skip for checks, must retain
                ++i;
            }
        }
        for (; i < len; ++i) {
            char c = input.charAt(i);
            // if other symbols, parse as Double, coerce
            if (c > '9' || c < '0') {
                try {
                    double d = Double.parseDouble(input);
                    return (int) d;
                } catch (NumberFormatException e) { }
            }
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) { }
        return defaultValue;
    }

    /**
     * @since 1.6
     */
    public static long parseAsLong(String input, long defaultValue)
    {
        if (input == null) {
            return defaultValue;
        }
        input = input.trim();
        int len = input.length();
        if (len == 0) {
            return defaultValue;
        }
        // One more thing: use integer parsing for 'simple'
        int i = 0;
        if (i < len) { // skip leading sign:
            char c = input.charAt(0);
            if (c == '+') { // for plus, actually physically remove
                input = input.substring(1);
                len = input.length();
            } else if (c == '-') { // minus, just skip for checks, must retain
                ++i;
            }
        }
        for (; i < len; ++i) {
            char c = input.charAt(i);
            // if other symbols, parse as Double, coerce
            if (c > '9' || c < '0') {
                try {
                    double d = Double.parseDouble(input);
                    return (long) d;
                } catch (NumberFormatException e) { }
            }
        }
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) { }
        return defaultValue;
    }
    
    /**
     * @since 1.6
     */
    public static double parseAsDouble(String input, double defaultValue)
    {
        if (input == null) {
            return defaultValue;
        }
        input = input.trim();
        int len = input.length();
        if (len == 0) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) { }
        return defaultValue;
    }
}
