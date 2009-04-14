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
public class StdDateFormat
    extends DateFormat
{
    /**
     * This constant defines a commonly used date format that conforms
     * to ISO-8601 date formatting standard.
     */
    final static String DATE_FORMAT_STR_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * This constant defines the date format specified by
     * RFC 1123.
     */
    final static String DATE_FORMAT_STR_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    final static SimpleDateFormat DATE_FORMAT_ISO8601;
    final static SimpleDateFormat DATE_FORMAT_RFC1123;

    /* Let's construct "blueprint" date format instances: can not be used
     * as is, due to thread-safety issues, but can be used for constructing
     * actual instances more cheaply (avoids re-parsing).
     */
    static {
        /* Another important thing: let's force use of GMT for
         * baseline DataFormat objects
         */
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        DATE_FORMAT_ISO8601 = new SimpleDateFormat(DATE_FORMAT_STR_ISO8601);
        DATE_FORMAT_ISO8601.setTimeZone(gmt);
        DATE_FORMAT_RFC1123 = new SimpleDateFormat(DATE_FORMAT_STR_RFC1123);
        DATE_FORMAT_RFC1123.setTimeZone(gmt);
    }

    /**
     * A singleton instance can be used for cloning purposes.
     */
    public final static StdDateFormat instance = new StdDateFormat();

    transient DateFormat _formatISO8601;
    transient DateFormat _formatRFC1123;

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
        DateFormat df = (DateFormat) DATE_FORMAT_ISO8601.clone();
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
        DateFormat df = (DateFormat) DATE_FORMAT_RFC1123.clone();
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
        if (result == null) {
            throw new ParseException
                (String.format("Can not parse date \"%s\": not compatible with any of standard forms (\"%s\" or \"%s\")",
                               dateStr,
                               DATE_FORMAT_STR_ISO8601,
                               DATE_FORMAT_STR_RFC1123
                               ), pos.getErrorIndex());
        }
        return result;
    }

    public Date parse(String dateStr, ParsePosition pos)
    {

        return findLikeliestFormat(dateStr).parse(dateStr, pos);
    }

    public StringBuffer format(Date date, StringBuffer toAppendTo,
                               FieldPosition fieldPosition)
    {
        if (_formatISO8601 == null) {
            _formatISO8601 = (DateFormat) DATE_FORMAT_ISO8601.clone();
        }
        return _formatISO8601.format(date, toAppendTo, fieldPosition);
    }

    /*
    /////////////////////////////////////////////////////
    // Helper method
    /////////////////////////////////////////////////////
     */

    /**
     * Overridable helper method used to figure out which of supported
     * formats is the likeliest match.
     */
    protected DateFormat findLikeliestFormat(String dateStr)
    {
        if (dateStr.length() >= 5
            && Character.isDigit(dateStr.charAt(0))
            && Character.isDigit(dateStr.charAt(3))
            && dateStr.charAt(4) == '-'
            ) {
            if (_formatISO8601 == null) {
                _formatISO8601 = (DateFormat) DATE_FORMAT_ISO8601.clone();
            }
            return _formatISO8601;
        }
        // Fall back to RFC-1123
        if (_formatRFC1123 == null) {
            _formatRFC1123 = (DateFormat) DATE_FORMAT_RFC1123.clone();
        }
        return _formatRFC1123;
    }
}

