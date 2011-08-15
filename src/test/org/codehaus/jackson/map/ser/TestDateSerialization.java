package org.codehaus.jackson.map.ser;

import java.io.*;
import java.text.*;
import java.util.*;

import org.codehaus.jackson.map.*;

public class TestDateSerialization
    extends BaseMapTest
{
    static class TimeZoneBean {
        private TimeZone tz;
        
        public TimeZoneBean(String name) {
            tz = TimeZone.getTimeZone(name);
        }

        public TimeZone getTz() { return tz; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    public void testDateNumeric() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        // default is to output time stamps...
        assertTrue(mapper.getSerializationConfig().isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS));
        // shouldn't matter which offset we give...
        mapper.writeValue(sw, new Date(199L));
        assertEquals("199", sw.toString());
    }

    public void testDateISO8601() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        // let's hit epoch start
        mapper.writeValue(sw, new Date(0L));
        assertEquals("\"1970-01-01T00:00:00.000+0000\"", sw.toString());
    }

    public void testDateOther() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("PST"));
        mapper.setDateFormat(df);
        // let's hit epoch start, offset by a bit
        assertEquals(quote("1969-12-31X16:00:00"), mapper.writeValueAsString(new Date(0L)));
    }

    @SuppressWarnings("deprecation")
    public void testSqlDate() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        // use date 1999-04-01 (note: months are 0-based, use constant)
        java.sql.Date date = new java.sql.Date(99, Calendar.APRIL, 1);
        assertEquals(quote("1999-04-01"), serializeAsString(mapper, date));
    }

    public void testTimeZone() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        TimeZone input = TimeZone.getTimeZone("PST");
        String json = mapper.writeValueAsString(input);
        assertEquals(quote("PST"), json);
    }

    // [JACKSON-663]
    public void testTimeZoneInBean() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new TimeZoneBean("PST"));
        assertEquals("{\"tz\":\"PST\"}", json);
    }
    
    // [JACKSON-648]: (re)configuring via ObjectWriter
    public void testDateUsingObjectWriter() throws IOException
    {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("PST"));
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(quote("1969-12-31X16:00:00"),
                mapper.writer(df).writeValueAsString(new Date(0L)));
        ObjectWriter w = mapper.writer((DateFormat)null);
        assertEquals("0", w.writeValueAsString(new Date(0L)));

        w = w.withDateFormat(df);
        assertEquals(quote("1969-12-31X16:00:00"), w.writeValueAsString(new Date(0L)));
        w = w.withDateFormat(null);
        assertEquals("0", w.writeValueAsString(new Date(0L)));
    }

    // [JACKSON-606]
    public void testDatesAsMapKeys() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<Date,Integer> map = new HashMap<Date,Integer>();
        assertFalse(mapper.isEnabled(SerializationConfig.Feature.WRITE_DATE_KEYS_AS_TIMESTAMPS));
        map.put(new Date(0L), Integer.valueOf(1));
        // by default will serialize as ISO-8601 values...
        assertEquals("{\"1970-01-01T00:00:00.000+0000\":1}", mapper.writeValueAsString(map));
        
        // but can change to use timestamps too
        mapper.configure(SerializationConfig.Feature.WRITE_DATE_KEYS_AS_TIMESTAMPS, true);
        assertEquals("{\"0\":1}", mapper.writeValueAsString(map));
    }
}

