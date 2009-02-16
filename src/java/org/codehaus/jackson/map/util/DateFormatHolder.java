package org.codehaus.jackson.map.deser;

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
    final static String DATE_FORMAT_STR_STD = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    final static String DATE_FORMAT_STR_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    final static SimpleDateFormat DATE_FORMAT_STD;
    final static SimpleDateFormat DATE_FORMAT_RFC1123;
    static {
        /* 07-Jan-2009, tatu: Let's first try parsing using what seems like
         *   a good standard timestamp format. Chances are we need to improve
         *   on this... but have to start somewhere.
         */
        DATE_FORMAT_STD = new SimpleDateFormat(DATE_FORMAT_STR_STD);

        /**
         * 16-Feb-2009, tatu: Looks like RFC 1123 dates are common too, so let's
         *   allow use of it as well.
         */
        DATE_FORMAT_RFC1123 = new SimpleDateFormat(DATE_FORMAT_STR_RFC1123);
    }

    DateFormat _formatStd;
    DateFormat _formatRfc1123;

    public DateFormatHolder() { }

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
                               DATE_FORMAT_STR_STD,
                               DATE_FORMAT_STR_RFC1123
                               ), pex);
        }
    }

    protected DateFormat findLikeliestFormat(String dateStr)
    {
        if (dateStr.length() >= 5
            && Character.isDigit(dateStr.charAt(0))
            && Character.isDigit(dateStr.charAt(3))
            && dateStr.charAt(4) == '-'
            ) {
            if (_formatStd == null) {
                _formatStd = (DateFormat) DATE_FORMAT_STD.clone();
            }
            return _formatStd;
        }
        // Fall back to RFC-1123
        if (_formatRfc1123 == null) {
            _formatRfc1123 = (DateFormat) DATE_FORMAT_RFC1123.clone();
        }
        return _formatRfc1123;
    }
}

