package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying handling of simple Date-related
 * types.
 */
public class TestObjectMapperDateDeserializer
    extends BaseTest
{
    public void testDateUtil() throws Exception
    {
        // not ideal, to use (ever-changing) current date, but...
        java.util.Date value = new java.util.Date();
        long now = value.getTime();

        // First from long
        assertEquals(value, new ObjectMapper().readValue(""+now, java.util.Date.class));

        String dateStr = serializeDateAsString(value);
        java.util.Date result = new ObjectMapper().readValue("\""+dateStr+"\"", java.util.Date.class);

        assertEquals("Date: expect "+value+" ("+value.getTime()+"), got "+result+" ("+result.getTime()+")", value.getTime(), result.getTime());
    }

    public void testDateSql() throws Exception
    {
        // not ideal, to use (ever-changing) current date, but...
        long now = System.currentTimeMillis();
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
