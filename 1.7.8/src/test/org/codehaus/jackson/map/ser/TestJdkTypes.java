package org.codehaus.jackson.map.ser;

import java.io.*;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.*;

/**
 * Unit tests for JDK types not covered by other tests (i.e. things
 * that are not Enums, Collections, Maps, or standard Date/Time types)
 */
public class TestJdkTypes
    extends org.codehaus.jackson.map.BaseMapTest
{
    /**
     * Unit test related to [JACKSON-155]
     */
    public void testFile() throws IOException
    {
        /* Not sure if this gets translated differently on Windows, Mac?
         * It'd be hard to make truly portable test tho...
         */
        File f = new File("/tmp/foo.txt");
        String str = serializeAsString(new ObjectMapper(), f);
        assertEquals("\""+f.getAbsolutePath()+"\"", str);
    }

    public void testRegexps() throws IOException
    {
        final String PATTERN_STR = "\\s+([a-b]+)\\w?";
        Pattern p = Pattern.compile(PATTERN_STR);
        Map<String,Object> input = new HashMap<String,Object>();
        input.put("p", p);
        Map<String,Object> result = writeAndMap(input);
        assertEquals(p.pattern(), result.get("p"));
    }

    public void testCurrency() throws IOException
    {
        Currency usd = Currency.getInstance("USD");
        assertEquals(quote("USD"), new ObjectMapper().writeValueAsString(usd));
    }

    // @since 1.7
    public void testLocale() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(quote("en"), mapper.writeValueAsString(new Locale("en")));
        assertEquals(quote("es_ES"), mapper.writeValueAsString(new Locale("es", "ES")));
        assertEquals(quote("fi_FI_savo"), mapper.writeValueAsString(new Locale("FI", "fi", "savo")));
    }

    // [JACKSON-484]
    public void testInetAddress() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(quote("127.0.0.1"), mapper.writeValueAsString(InetAddress.getByName("127.0.0.1")));
        assertEquals(quote("ning.com"), mapper.writeValueAsString(InetAddress.getByName("ning.com")));
    }

    // [JACKSON-597]
    public void testClass() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(quote("java.lang.String"), mapper.writeValueAsString(String.class));
        assertEquals(quote("int"), mapper.writeValueAsString(Integer.TYPE));
        assertEquals(quote("boolean"), mapper.writeValueAsString(Boolean.TYPE));
        assertEquals(quote("void"), mapper.writeValueAsString(Void.TYPE));
    }

}
