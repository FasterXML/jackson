package org.codehaus.jackson.map.deser;

import main.BaseTest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.codehaus.jackson.map.*;

public class TestDateDeserialization
    extends BaseTest
{
    public void testDateUtil() throws Exception
    {
        long now = 123456789L;
        java.util.Date value = new java.util.Date(now);

        // First from long
        assertEquals(value, new ObjectMapper().readValue(""+now, java.util.Date.class));

        String dateStr = serializeDateAsString(value);
        java.util.Date result = new ObjectMapper().readValue("\""+dateStr+"\"", java.util.Date.class);

        assertEquals("Date: expect "+value+" ("+value.getTime()+"), got "+result+" ("+result.getTime()+")", value.getTime(), result.getTime());
    }

    /**
     * As of version 0.9.8, we'll try to cover RFC-1123 Strings too,
     * automatically.
     */
    public void testDateUtilRFC1123() throws Exception
    {
        DateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        // let's use an arbitrary value...
        String inputStr = "Sat, 17 Jan 2009 06:13:58 +0000";
        java.util.Date inputDate = fmt.parse(inputStr);
        assertEquals(inputDate, new ObjectMapper().readValue("\""+inputStr+"\"", java.util.Date.class));
    }

    /**
     * With 1.0.0 release, ISO8601 is supported as well
     */
    public void testDateUtilISO8601() throws Exception
    {
        /* let's use simple baseline value, arbitrary date in GMT,
         * using the standard notation
         */
        String inputStr = "1972-12-28T00:00:00.000+0000";
        Date inputDate = new ObjectMapper().readValue("\""+inputStr+"\"", java.util.Date.class);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        // And then the same, but using 'Z' as alias for +0000 (very common)
        inputStr = "1972-12-28T00:00:00.000Z";
        inputDate = new ObjectMapper().readValue("\""+inputStr+"\"", java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        // Same but using colon in timezone
        inputStr = "1972-12-28T00:00:00.000+00:00";
        inputDate = new ObjectMapper().readValue("\""+inputStr+"\"", java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        // Same but only passing hour difference as timezone
        inputStr = "1972-12-28T00:00:00.000+00";
        inputDate = new ObjectMapper().readValue("\""+inputStr+"\"", java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));
    }

    public void testDateSql() throws Exception
    {
        // not ideal, to use (ever-changing) current date, but...
        long now = 1112223334445L;
        java.sql.Date value = new java.sql.Date(now);

        // First from long
        assertEquals(value, new ObjectMapper().readValue(""+now, java.sql.Date.class));
        // then from String
        
        String expStr = value.toString();
        java.sql.Date result = new ObjectMapper().readValue("\""+expStr+"\"", java.sql.Date.class);
        String actStr = result.toString();

        /* 07-Jan-2009, tatu: Ok; things get weird here: java.sql.Date
         *   does NOT override equals() method. But that's just plain wrong,
         *   as it should NOT compare time part, as it's not supposed to even
         *   exist (essentially) for this type. So, let's compare String
         *   representation, not timestamp
         */

        assertEquals(actStr, expStr);
    }

    public void testCalendar() throws Exception
    {
        // not ideal, to use (ever-changing) current date, but...
        java.util.Date now = new Date();
        java.util.Calendar value = Calendar.getInstance();
        value.setTime(now);

        // First from long
        assertEquals(value, new ObjectMapper().readValue(""+now.getTime(), Calendar.class));

        String dateStr = serializeDateAsString(now);
        Calendar result = new ObjectMapper().readValue("\""+dateStr+"\"", Calendar.class);

        assertEquals(value, result);
    }

    public void testCustom() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("PST"));
        mapper.getDeserializationConfig().setDateFormat(df);

        String dateStr = "1972-12-28X15:45:00";
        java.util.Date exp = df.parse(dateStr);
        java.util.Date result = mapper.readValue("\""+dateStr+"\"", java.util.Date.class);
        assertEquals(exp, result);
    }

    /*
    //////////////////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////////////////
     */

    String serializeDateAsString(java.util.Date value)
    {
        /* Then from String. This is bit tricky, since JDK does not really
         * suggest a 'standard' format. So let's try using something...
         */
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return df.format(value);
    }
}
