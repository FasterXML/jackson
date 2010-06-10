package org.codehaus.jackson.map.deser;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;

/**
 * Unit test for verifying that exceptions are properly handled (caught,
 * re-thrown or wrapped, depending)
 * with Object deserialization.
 */
public class TestExceptionHandling
    extends BaseMapTest
{
    static class Bean {
        String x;
    }

    /**
     * Verification of [JACKSON-301]
     */
    public void testHandlingOfUnrecognized() throws Exception
    {
        try {
            new ObjectMapper().readValue("{\"bar\":3}", Bean.class);
            fail("Should have failed binding");
        } catch (UnrecognizedPropertyException e) {
            assertEquals("bar", e.getUnrecognizedPropertyName());
            assertEquals(Bean.class, e.getReferringClass());
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    /**
     * Simple test to check behavior when end-of-stream is encountered
     * without content. Should throw EOFException.
     */
    public void testExceptionWithEmpty() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Object result = mapper.readValue("    ", Object.class);
            fail("Expected an exception, but got result value: "+result);
        } catch (Exception e) {
            verifyException(e, EOFException.class, "No content");
        }
    }

    public void testExceptionWithIncomplete()
        throws Exception
    {
        BrokenStringReader r = new BrokenStringReader("[ 1, ", "TEST");
        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(r);
        ObjectMapper mapper = new ObjectMapper();
        try {
            @SuppressWarnings("unused")
            Object ob = mapper.readValue(jp, Object.class);
            fail("Should have gotten an exception");
        } catch (IOException e) {
            /* For "bona fide" IO problems (due to low-level problem,
             * thrown by reader/stream), IOException must be thrown
             */
            verifyException(e, IOException.class, "TEST");
        }
    }

    public void testExceptionWithEOF()
        throws Exception
    {
        StringReader r = new StringReader("  3");
        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(r);
        ObjectMapper mapper = new ObjectMapper();

        Integer I = mapper.readValue(jp, Integer.class);
        assertEquals(3, I.intValue());

        // and then end-of-input...
        try {
            I = mapper.readValue(jp, Integer.class);
            fail("Should have gotten an exception");
        } catch (IOException e) {
            verifyException(e, EOFException.class, "No content");
        }
        // also: should have no current token after end-of-input
        JsonToken t = jp.getCurrentToken();
        if (t != null) {
            fail("Expected current token to be null after end-of-stream, was: "+t);
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
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
