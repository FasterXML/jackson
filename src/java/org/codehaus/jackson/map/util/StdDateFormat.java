package org.codehaus.jackson.map.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Default {@link DateFormat} implementation used by standard Date
 * serializers and deserializers. For serialization defaults to using
 * an ISO-8601 compliant format (format String "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
 * and for deserialization, both ISO-8601 and RFC-1123.
 */
@SuppressWarnings("serial")
public class StdDateFormat
    extends DateFormat
{
    /**
     * Defines a commonly used date format that conforms
     * to ISO-8601 date formatting standard, when it includes basic undecorated
     * timezone definition
     */
    final static String DATE_FORMAT_STR_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * Same as 'regular' 8601, but handles 'Z' as an alias for "+0000"
     * (or "GMT")
     */
    final static String DATE_FORMAT_STR_ISO8601_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * This constant defines the date format specified by
     * RFC 1123.
     */
    final static String DATE_FORMAT_STR_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * For error messages we'll also need a list of all formats.
     */
    final static String[] ALL_FORMATS = new String[] {
        DATE_FORMAT_STR_ISO8601,
        DATE_FORMAT_STR_ISO8601_Z,
        DATE_FORMAT_STR_RFC1123
    };

    final static SimpleDateFormat DATE_FORMAT_RFC1123;

    final static SimpleDateFormat DATE_FORMAT_ISO8601;
    final static SimpleDateFormat DATE_FORMAT_ISO8601_Z;

    /* Let's construct "blueprint" date format instances: can not be used
     * as is, due to thread-safety issues, but can be used for constructing
     * actual instances more cheaply (avoids re-parsing).
     */
    static {
        /* Another important thing: let's force use of GMT for
         * baseline DataFormat objects
         */
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        DATE_FORMAT_RFC1123 = new SimpleDateFormat(DATE_FORMAT_STR_RFC1123);
        DATE_FORMAT_RFC1123.setTimeZone(gmt);
        DATE_FORMAT_ISO8601 = new SimpleDateFormat(DATE_FORMAT_STR_ISO8601);
        DATE_FORMAT_ISO8601.setTimeZone(gmt);
        DATE_FORMAT_ISO8601_Z = new SimpleDateFormat(DATE_FORMAT_STR_ISO8601_Z);
        DATE_FORMAT_ISO8601_Z.setTimeZone(gmt);
    }

    /**
     * A singleton instance can be used for cloning purposes.
     */
    public final static StdDateFormat instance = new StdDateFormat();

    transient SimpleDateFormat _formatRFC1123;
    transient SimpleDateFormat _formatISO8601;
    transient SimpleDateFormat _formatISO8601_z;

    /*
    /////////////////////////////////////////////////////
    // Life cycle, accessing singleton "standard" formats
    /////////////////////////////////////////////////////
     */

    public StdDateFormat() { }

    public StdDateFormat clone() {
        /* Since we always delegate all work to child DateFormat instances,
         * let's NOT call super.clone(); this is bit unusual, but makes
         * sense here to avoid unnecessary work.
         */
        return new StdDateFormat();
    }

    /**
     * Method for getting the globally shared DateFormat instance
     * that uses GMT timezone and can handle simple ISO-8601
     * compliant date format.
     */
    public static DateFormat getBlueprintISO8601Format() {
        return DATE_FORMAT_ISO8601;
    }

    /**
     * Method for getting a non-shared DateFormat instance
     * that uses specified timezone and can handle simple ISO-8601
     * compliant date format.
     */
    public static DateFormat getISO8601Format(TimeZone tz) {
        DateFormat df = (SimpleDateFormat) DATE_FORMAT_ISO8601.clone();
        df.setTimeZone(tz);
        return df;
    }

    /**
     * Method for getting the globally shared DateFormat instance
     * that uses GMT timezone and can handle RFC-1123
     * compliant date format.
     */
    public static DateFormat getBlueprintRFC1123Format() {
        return DATE_FORMAT_RFC1123;
    }


    /**
     * Method for getting a non-shared DateFormat instance
     * that uses specific timezone and can handle RFC-1123
     * compliant date format.
     */
    public static DateFormat getRFC1123Format(TimeZone tz)
    {
        DateFormat df = (SimpleDateFormat) DATE_FORMAT_RFC1123.clone();
        df.setTimeZone(tz);
        return df;
    }

    /*
    /////////////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////////////
     */

    public Date parse(String dateStr)
        throws ParseException
    {
        dateStr = dateStr.trim();
        ParsePosition pos = new ParsePosition(0);
        Date result = parse(dateStr, pos);
        if (result != null) {
            return result;
        }

        StringBuilder sb = new StringBuilder();
        for (String f : ALL_FORMATS) {
            if (sb.length() > 0) {
                sb.append("\", \"");
            } else {
                sb.append('"');
            }
            sb.append(f);
        }
        sb.append('"');
        throw new ParseException
            (String.format("Can not parse date \"%s\": not compatible with any of standard forms (%s)",
                           dateStr, sb.toString()), pos.getErrorIndex());
    }

    public Date parse(String dateStr, ParsePosition pos)
    {
        if (looksLikeISO8601(dateStr)) {
            return parseAsISO8601(dateStr, pos);
        }
        // Otherwise, fall back to using RFC 1123
        return parseAsRFC1123(dateStr, pos);
    }

    public StringBuffer format(Date date, StringBuffer toAppendTo,
                               FieldPosition fieldPosition)
    {
        if (_formatISO8601 == null) {
            _formatISO8601 = (SimpleDateFormat) DATE_FORMAT_ISO8601.clone();
        }
        return _formatISO8601.format(date, toAppendTo, fieldPosition);
    }

    /*
    /////////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////////
     */

    /**
     * Overridable helper method used to figure out which of supported
     * formats is the likeliest match.
     */
    protected boolean looksLikeISO8601(String dateStr)
    {
        if (dateStr.length() >= 5
            && Character.isDigit(dateStr.charAt(0))
            && Character.isDigit(dateStr.charAt(3))
            && dateStr.charAt(4) == '-'
            ) {
            return true;
        }
        return false;
    }

    protected Date parseAsISO8601(String dateStr, ParsePosition pos)
    {
        /* 21-May-2009, tatu: SimpleDateFormat has very strict handling of
         * timezone  modifiers for ISO-8601. So we need to do some scrubbing.
         */

        /* First: do we have "zulu" format ('Z' == "GMT")? If yes, that's
         * quite simple because we already set date format timezone to be
         * GMT, and hence can just strip out 'Z' altogether
         */
        int len = dateStr.length();
        char c = dateStr.charAt(len-1);
        SimpleDateFormat df;

        if (c == 'Z') {
            df = _formatISO8601_z;
            if (df == null) {
                df = _formatISO8601_z = (SimpleDateFormat) DATE_FORMAT_ISO8601_Z.clone();
            }
        } else {
            c = dateStr.charAt(len-3);
            if (c == ':') { // remove optional colon
                // remove colon
                StringBuilder sb = new StringBuilder(dateStr);
                sb.delete(len-3, len-2);
                dateStr = sb.toString();
            } else if (c == '+' || c == '-') { // missing minutes
                // let's just append '00'
                dateStr += "00";
            }
            // otherwise regular (or invalid)

            df = _formatISO8601;
            if (_formatISO8601 == null) {
                df = _formatISO8601 = (SimpleDateFormat) DATE_FORMAT_ISO8601.clone();
            }
        }
        return df.parse(dateStr, pos);
    }

    protected Date parseAsRFC1123(String dateStr, ParsePosition pos)
    {
        if (_formatRFC1123 == null) {
            _formatRFC1123 = (SimpleDateFormat) DATE_FORMAT_RFC1123.clone();
        }
        return _formatRFC1123.parse(dateStr, pos);
    }
}

