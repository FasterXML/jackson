package org.codehaus.jackson.map.ext;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.map.*;

import org.joda.time.*;

/**
 * Unit tests for verifying limited interoperability for Joda time.
 * Basic support is added for handling {@link DateTime}; more can be
 * added over time if and when requested.
 */
public class TestJodaTime
    extends org.codehaus.jackson.map.BaseMapTest
{
    /*
    /**********************************************************
    /* Tests for DateTime (and closely related)
    /**********************************************************
     */

    /**
     * First: let's ensure that serialization does not fail
     * with an error (see [JACKSON-157]).
     */
    public void testSerialization() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        // let's use epoch time (Jan 1, 1970, UTC)
        DateTime dt = new DateTime(0L, DateTimeZone.UTC);
        // by default, dates use timestamp, so:
        assertEquals("0", serializeAsString(m, dt));

        // but if re-configured, as regular ISO-8601 string
        m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        assertEquals(quote("1970-01-01T00:00:00.000Z"), serializeAsString(m, dt));
    }

    /**
     * Ok, then: should be able to convert from JSON String or Number,
     * with standard deserializer we provide.
     */
    public void testDeserFromNumber() throws IOException
    {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        // use some arbitrary but non-default time point (after 1.1.1970)
        cal.set(Calendar.YEAR, 1972);
        long timepoint = cal.getTime().getTime();

        ObjectMapper mapper = new ObjectMapper();
        // Ok, first: using JSON number (milliseconds since epoch)
        DateTime dt = mapper.readValue(String.valueOf(timepoint), DateTime.class);
        assertEquals(timepoint, dt.getMillis());

        // And then ISO-8601 String
        dt = mapper.readValue(quote("1972-12-28T12:00:01.000+0000"), DateTime.class);
        assertEquals("1972-12-28T12:00:01.000Z", dt.toString());
    }

    // @since 1.6    
    public void testDeserReadableDateTime() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        ReadableDateTime date = mapper.readValue(quote("1972-12-28T12:00:01.000+0000"), ReadableDateTime.class);
        assertNotNull(date);
        assertEquals("1972-12-28T12:00:01.000Z", date.toString());

        // since 1.6.1, for [JACKSON-360]
        assertNull(mapper.readValue(quote(""), ReadableDateTime.class));
    }

    // @since 1.6    
    public void testDeserReadableInstant() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        ReadableInstant date = mapper.readValue(quote("1972-12-28T12:00:01.000+0000"), ReadableInstant.class);
        assertNotNull(date);
        assertEquals("1972-12-28T12:00:01.000Z", date.toString());

        // since 1.6.1, for [JACKSON-360]
        assertNull(mapper.readValue(quote(""), ReadableInstant.class));
    }
    
    /*
    /**********************************************************
    /* Tests for DateMidnight type
    /**********************************************************
     */
    
    // @since 1.5
    public void testDateMidnightSer() throws IOException
    {
        DateMidnight date = new DateMidnight(2001, 5, 25);
        ObjectMapper mapper = new ObjectMapper();
        // default format is that of JSON array...
        assertEquals("[2001,5,25]", mapper.writeValueAsString(date));
        // but we can force it to be a String as well (note: here we assume this is
        // dynamically changeable)
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);        
        assertEquals(quote("2001-05-25"), mapper.writeValueAsString(date));
    }

    // @since 1.5
    public void testDateMidnightDeser() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        // couple of acceptable formats, so:
        DateMidnight date = mapper.readValue("[2001,5,25]", DateMidnight.class);
        assertEquals(2001, date.getYear());
        assertEquals(5, date.getMonthOfYear());
        assertEquals(25, date.getDayOfMonth());

        DateMidnight date2 = mapper.readValue(quote("2005-07-13"), DateMidnight.class);
        assertEquals(2005, date2.getYear());
        assertEquals(7, date2.getMonthOfYear());
        assertEquals(13, date2.getDayOfMonth());

        // since 1.6.1, for [JACKSON-360]
        assertNull(mapper.readValue(quote(""), DateMidnight.class));
    }
    
    /*
    /**********************************************************
    /* Tests for LocalDate type
    /**********************************************************
     */
    
    // @since 1.5
    public void testLocalDateSer() throws IOException
    {
        LocalDate date = new LocalDate(2001, 5, 25);
        ObjectMapper mapper = new ObjectMapper();
        // default format is that of JSON array...
        assertEquals("[2001,5,25]", mapper.writeValueAsString(date));
        // but we can force it to be a String as well (note: here we assume this is
        // dynamically changeable)
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);        
        assertEquals(quote("2001-05-25"), mapper.writeValueAsString(date));
    }

    // @since 1.5
    public void testLocalDateDeser() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        // couple of acceptable formats, so:
        LocalDate date = mapper.readValue("[2001,5,25]", LocalDate.class);
        assertEquals(2001, date.getYear());
        assertEquals(5, date.getMonthOfYear());
        assertEquals(25, date.getDayOfMonth());

        LocalDate date2 = mapper.readValue(quote("2005-07-13"), LocalDate.class);
        assertEquals(2005, date2.getYear());
        assertEquals(7, date2.getMonthOfYear());
        assertEquals(13, date2.getDayOfMonth());

        // since 1.6.1, for [JACKSON-360]
        assertNull(mapper.readValue(quote(""), LocalDate.class));
    }

    /*
    /**********************************************************
    /* Tests for LocalDateTime type
    /**********************************************************
     */
    
    // @since 1.5
    public void testLocalDateTimeSer() throws IOException
    {
        LocalDateTime date = new LocalDateTime(2001, 5, 25,
                10, 15, 30, 37);
        ObjectMapper mapper = new ObjectMapper();
        // default format is that of JSON array...
        assertEquals("[2001,5,25,10,15,30,37]", mapper.writeValueAsString(date));
        // but we can force it to be a String as well (note: here we assume this is
        // dynamically changeable)
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);        
        assertEquals(quote("2001-05-25T10:15:30.037"), mapper.writeValueAsString(date));
    }

    // @since 1.5
    public void testLocalDateTimeDeser() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        // couple of acceptable formats again:
        LocalDateTime date = mapper.readValue("[2001,5,25,10,15,30,37]", LocalDateTime.class);
        assertEquals(2001, date.getYear());
        assertEquals(5, date.getMonthOfYear());
        assertEquals(25, date.getDayOfMonth());

        assertEquals(10, date.getHourOfDay());
        assertEquals(15, date.getMinuteOfHour());
        assertEquals(30, date.getSecondOfMinute());
        assertEquals(37, date.getMillisOfSecond());

        LocalDateTime date2 = mapper.readValue(quote("2007-06-30T08:34:09.001"), LocalDateTime.class);
        assertEquals(2007, date2.getYear());
        assertEquals(6, date2.getMonthOfYear());
        assertEquals(30, date2.getDayOfMonth());

        assertEquals(8, date2.getHourOfDay());
        assertEquals(34, date2.getMinuteOfHour());
        assertEquals(9, date2.getSecondOfMinute());
        assertEquals(1, date2.getMillisOfSecond());

        // since 1.6.1, for [JACKSON-360]
        assertNull(mapper.readValue(quote(""), LocalDateTime.class));
    }
}
