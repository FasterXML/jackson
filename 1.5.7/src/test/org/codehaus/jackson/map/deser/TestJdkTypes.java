package org.codehaus.jackson.map.deser;

import java.io.*;
import java.util.Currency;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.*;

public class TestJdkTypes
    extends org.codehaus.jackson.map.BaseMapTest
{
    /**
     * Related to issue [JACKSON-155].
     */
    public void testFile() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // Not portable etc... has to do:
        File src = new File("/test").getAbsoluteFile();
        File result = m.readValue("\""+src.getAbsolutePath()+"\"", File.class);
        assertEquals(src.getAbsolutePath(), result.getAbsolutePath());
    }

    public void testRegexps() throws IOException
    {
        final String PATTERN_STR = "abc:\\s?(\\d+)";
        Pattern exp = Pattern.compile(PATTERN_STR);
        /* Ok: easiest way is to just serialize first; problem
         * is the backslash
         */
        ObjectMapper m = new ObjectMapper();
        String json = m.writeValueAsString(exp);
        Pattern result = m.readValue(json, Pattern.class);
        assertEquals(exp.pattern(), result.pattern());
    }

    public void testCurrency() throws IOException
    {
        Currency usd = Currency.getInstance("USD");
        assertEquals(usd, new ObjectMapper().readValue(quote("USD"), Currency.class));
    }
}
