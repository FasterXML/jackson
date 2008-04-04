package org.codehaus.jackson.util;

import java.util.Arrays;

public final class CharTypes
{
    final static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    private CharTypes() { }

    /**
     * Lookup table used for determining which input characters
     * need special handling when contained in text segment.
     */
    final static int[] sInputCodes;
    static {
        /* 96 would do for most cases (backslash is ascii 94)
         * but if we want to do lookups by raw bytes it's better
         * to have full table
         */
        int[] table = new int[96];
        // Control chars and non-space white space are not allowed unquoted
        for (int i = 0; i < 32; ++i) {
            table[i] = 1;
        }
        // And then string end and quote markers are special too
        table['"'] = 1;
        table['\\'] = 1;
        sInputCodes = table;
    }

    /**
     * Lookup table used for determining which output characters
     * need to be quoted.
     */
    final static int[] sOutputEscapes;
    static {
        int[] table = new int[256];
        // Control chars need generic escape sequence
        for (int i = 0; i < 32; ++i) {
            table[i] = -(i + 1);
        }
        /* Others (and some within that range too) have explicit shorter
         * sequences
         */
        table['"'] = '"';
        table['\\'] = '\\';
        // Escaping of slash is optional, so let's not add it
        table[0x08] = 'b';
        table[0x09] = 't';
        table[0x0C] = 'f';
        table[0x0A] = 'n';
        table[0x0D] = 'r';
        sOutputEscapes = table;
    }

    /**
     * Lookup table for the first 128 Unicode characters (7-bit ascii)
     * range. For actual hex digits, contains corresponding value;
     * for others -1.
     */
    final static int[] sHexValues = new int[128];
    static {
        Arrays.fill(sHexValues, -1);
        for (int i = 0; i < 10; ++i) {
            sHexValues['0' + i] = i;
        }
        for (int i = 0; i < 6; ++i) {
            sHexValues['a' + i] = 10 + i;
            sHexValues['A' + i] = 10 + i;
        }
    }

    public static int[] getInputCode() { return sInputCodes; }
    public static int[] getOutputEscapes() { return sOutputEscapes; }

    public static int charToHex(int ch)
    {
        return (ch > 127) ? -1 : sHexValues[ch];
    }

    public static void appendQuoted(StringBuilder sb, String content)
    {
        final int[] escCodes = sOutputEscapes;
        int escLen = escCodes.length;
        for (int i = 0, len = content.length(); i < len; ++i) {
            char c = content.charAt(i);
            if (c >= escLen || escCodes[c] == 0) {
                sb.append(c);
                continue;
            }
            sb.append('\\');
            int escCode = escCodes[c];
            if (escCode < 0) { // generic quoting (hex value)
                // We know that it has to fit in just 2 hex chars
                sb.append('u');
                sb.append('0');
                sb.append('0');
                int value = -(escCode + 1);
                sb.append(HEX_CHARS[value >> 4]);
                sb.append(HEX_CHARS[value & 0xF]);
            } else { // "named", i.e. prepend with slash
                sb.append((char) escCode);
            }
        }
    }
}

