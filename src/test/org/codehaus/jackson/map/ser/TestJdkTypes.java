package org.codehaus.jackson.map.ser;

import java.io.*;
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
        String str = serializeAsString(new ObjectMapper(), p);
        assertEquals(p.pattern(), str);
    }
}
