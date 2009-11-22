package org.codehaus.jackson.map.interop;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.map.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Unit tests for verifying limited interoperability for Joda time.
 * Basic support is added for handling {@link DateTime}; more can be
 * added over time if and when requested.
 */
public class TestJodaTime
    extends org.codehaus.jackson.map.BaseMapTest
{
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
}
