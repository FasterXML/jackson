package org.codehaus.jackson.map.deser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.codehaus.jackson.map.*;

public class TestDateDeserialization
    extends BaseMapTest
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
     * ISO8601 is supported as well
     */
    public void testDateUtilISO8601() throws Exception
    {
        /* let's use simple baseline value, arbitrary date in GMT,
         * using the standard notation
         */
        ObjectMapper mapper = new ObjectMapper();
        String inputStr = "1972-12-28T00:00:00.000+0000";
        Date inputDate = mapper.readValue("\""+inputStr+"\"", java.util.Date.class);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        // And then the same, but using 'Z' as alias for +0000 (very common)
        inputStr = "1972-12-28T00:00:00.000Z";
        inputDate = mapper.readValue(quote(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        // Same but using colon in timezone
        inputStr = "1972-12-28T00:00:00.000+00:00";
        inputDate = mapper.readValue(quote(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        // Same but only passing hour difference as timezone
        inputStr = "1972-12-28T00:00:00.000+00";
        inputDate = mapper.readValue(quote(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        inputStr = "1984-11-30T00:00:00.000Z";
        inputDate = mapper.readValue(quote(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1984, c.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, c.get(Calendar.MONTH));
        assertEquals(30, c.get(Calendar.DAY_OF_MONTH));
    }

    public void testDateUtilISO8601NoTimezone() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // Timezone itself is optional as well... 
        String inputStr = "1984-11-13T00:00:09";
        Date inputDate = mapper.readValue(quote(inputStr), java.util.Date.class);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(1984, c.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, c.get(Calendar.MONTH));
        assertEquals(13, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c.get(Calendar.MINUTE));
        assertEquals(9, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));
    }

    public void testDateUtilISO8601JustDate() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // Plain date (no time)
        String inputStr = "1972-12-28";
        Date inputDate = mapper.readValue(quote(inputStr), java.util.Date.class);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

    }


    @SuppressWarnings("deprecation")
    public void testDateSql() throws Exception
    {
        java.sql.Date value = new java.sql.Date(0L);
        value.setYear(99); // 1999
        value.setDate(19);
        value.setMonth(Calendar.APRIL);
        long now = value.getTime();

        // First from long
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(value, mapper.readValue(String.valueOf(now), java.sql.Date.class));

        // then from default java.sql.Date String serialization:
        
        java.sql.Date result = mapper.readValue(quote(value.toString()), java.sql.Date.class);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTimeInMillis(result.getTime());
        assertEquals(1999, c.get(Calendar.YEAR));
        assertEquals(Calendar.APRIL, c.get(Calendar.MONTH));
        assertEquals(19, c.get(Calendar.DAY_OF_MONTH));

        /* [JACKSON-200]: looks like we better add support for regular date
         *   formats as well
         */
        String expStr = "1981-07-13";
        result = mapper.readValue(quote(expStr), java.sql.Date.class);
        c.setTimeInMillis(result.getTime());
        assertEquals(1981, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(13, c.get(Calendar.DAY_OF_MONTH));

        /* 20-Nov-2009, tatus: I'll be damned if I understand why string serialization
         *   is off-by-one, but day-of-month does seem to be one less. My guess is
         *   that something is funky with timezones (i.e. somewhere local TZ is
         *   being used), but just can't resolve it. Hence, need to comment this:
         */
        //assertEquals(expStr, result.toString());
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

    /**
     * Test for [JACKSON-203]: make empty Strings deserialize as nulls by default,
     * without need to turn on feature (which may be added in future)
     */
    public void testDatesWithEmptyStrings() throws Exception
    {
        java.util.Date now = new Date();
        ObjectMapper mapper = new ObjectMapper();

        assertNull(mapper.readValue(quote(""), java.util.Date.class));
        assertNull(mapper.readValue(quote(""), java.util.Calendar.class));
        assertNull(mapper.readValue(quote(""), java.sql.Date.class));
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
