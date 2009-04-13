package org.codehaus.jackson.map.deser;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit test for verifying that exceptions are properly handled (caught,
 * re-thrown or wrapped, depending)
 * with Object deserialization.
 */
public class TestExceptionHandling
    extends BaseMapTest
{
    public void testExceptionWithEOF()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        BrokenStringReader r = new BrokenStringReader("[ 1, ", "TEST");
        try {
            Object ob = mapper.readValue(r, Object.class);
            fail("Should have gotten an exception");
        } catch (IOException e) {
            verifyException(e, IOException.class, "TEST");
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////////////////
     */

    void verifyException(Exception e, Class<?> expType, String expMsg)
        throws Exception
    {
        if (e.getClass() != expType) {
            fail("Expected exception of type "+expType.getName()+", got "+e.getClass().getName());
        }
        if (expMsg != null) {
            verifyException(e, expMsg);
        }
    }
}
