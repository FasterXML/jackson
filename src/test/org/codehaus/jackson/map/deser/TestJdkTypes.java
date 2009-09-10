package org.codehaus.jackson.map.deser;

import java.io.*;

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
}
