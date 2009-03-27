package org.codehaus.jackson.map.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Simple container class, used to encapsulate most details of
 * Date parsing, so that other classes don't have to.
 * It will also handle simple form of reuse, based
 * on usage pattern where a distinct instance is constructed for
 * each mapping operation (or at least one per thread); such that
 * access is always single-threaded. If so, we can lazily instantiate
 * each DateFormat instance first time it is needed.
 */
public class DateFormatHolder
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
        DATE_FORMAT_ISO8601 = new SimpleDateFormat(DATE_FORMAT_STR_ISO8601);
        DATE_FORMAT_RFC1123 = new SimpleDateFormat(DATE_FORMAT_STR_RFC1123);
    }

    DateFormat _formatISO8601;
    DateFormat _formatRFC1123;

    /*
    /////////////////////////////////////////////////////
    // Life cycle, instance access
    /////////////////////////////////////////////////////
     */

    public DateFormatHolder() { }

    public static DateFormat getBlueprintISO8601Format() {
        return DATE_FORMAT_ISO8601;
    }

    public static DateFormat getBlueprintRFC1123Format() {
        return DATE_FORMAT_RFC1123;
    }

    /*
    /////////////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////////////
     */

    public Date parse(String dateStr)
        throws IllegalArgumentException
    {
        dateStr = dateStr.trim();

        /* 16-Feb-2009, tatu: Since we now have multiple date formats to
         *   considers, let's try to determine which one String could
         *   possibly be compatible with.
         */
        // First, "standard" one, "yyyy-MM-dd'T'HH:mm:ss.SSSZ"?
        DateFormat fmt = findLikeliestFormat(dateStr);
        try {
            return fmt.parse(dateStr);
        } catch (ParseException pex) {
            throw new IllegalArgumentException
                (String.format("Can not parse date \"%s\": not compatible with any of standard forms (\"%s\" or \"%s\")",
                               dateStr,
                               DATE_FORMAT_STR_ISO8601,
                               DATE_FORMAT_STR_RFC1123
                               ), pex);
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Helper method
    /////////////////////////////////////////////////////
     */

    /**
     * Overridable helper method 
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

