package org.codehaus.jackson.map.interop;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.map.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Unit tests for verifying limited interoperability for Joda time
 * (minimal just because Jackson tries to minimize default deps to
 * all non-JDK libs -- Joda is a fine date/time lib, and ideally
 * better integration should be added via extension mechanisms)
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
        Map<String,Object> result = writeAndMap(m, dt);

        // should verify something here? looks like we get 24 fields...
        assertEquals(24, result.size());
    }

    /**
     * Ok, then: should be able to convert from number directly, since
     * DateTime has a single-long-arg ctor
     */
    /* 19-Sep-2009, tatu: Alas, the way Jackson currently works, this
     *   will fail because there's no deserializer for referred type
     *   Chronology (abstract class)
     */
    /*
    public void testDeserFromNumber() throws IOException
    {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        // use some arbitrary but non-default time point (after 1.1.1970)
        cal.set(Calendar.YEAR, 1972);
        long timepoint = cal.getTime().getTime();

        DateTime dt = new ObjectMapper().readValue(String.valueOf(timepoint), DateTime.class);
        assertEquals(timepoint, dt.getMillis());
        
    }
    */
}
