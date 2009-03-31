package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;
import java.text.*;
import java.util.Date;
import java.util.TimeZone;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestDateSerialization
    extends BaseMapTest
{
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
        StringWriter sw = new StringWriter();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("PST"));
        mapper.getSerializationConfig().setDateFormat(df);
        // let's hit epoch start, offset by a bit
        mapper.writeValue(sw, new Date(0L));
        assertEquals("\"1969-12-31X16:00:00\"", sw.toString());
    }
}

