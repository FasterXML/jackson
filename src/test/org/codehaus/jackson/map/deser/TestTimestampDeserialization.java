package org.codehaus.jackson.map.deser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.codehaus.jackson.map.*;

public class TestTimestampDeserialization
    extends BaseMapTest
{
    // As for TestDateDeserialization except we don't need to test date conversion routines, so
    // just check we pick up timestamp class

    public void testTimestampUtil() throws Exception
    {
        long now = 123456789L;
        java.sql.Timestamp value = new java.sql.Timestamp(now);

        // First from long
        assertEquals(value, new ObjectMapper().readValue(""+now, java.sql.Timestamp.class));

        String dateStr = serializeTimestampAsString(value);
        java.sql.Timestamp result = new ObjectMapper().readValue("\""+dateStr+"\"", java.sql.Timestamp.class);

        assertEquals("Date: expect "+value+" ("+value.getTime()+"), got "+result+" ("+result.getTime()+")", value.getTime(), result.getTime());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private String serializeTimestampAsString(java.sql.Timestamp value)
    {
        /* Then from String. This is bit tricky, since JDK does not really
         * suggest a 'standard' format. So let's try using something...
         */
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return df.format(value);
    }
}
