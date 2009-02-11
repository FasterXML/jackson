package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tests use of Annotations for
 * bean serialization.
 */
public class TestAnnotations
    extends BaseTest
{
    /*
    //////////////////////////////////////////////
    // Annotated helper classes
    //////////////////////////////////////////////
     */

    /// Class for testing {@link JsonGetter} annotations
    final static class SizeClassGetter
    {
        @JsonGetter public int size() { return 3; }
        @JsonGetter("length") public int foobar() { return -17; }
        // note: need not be public since there's annotation
        @JsonGetter protected int value() { return 0; }
    }

    /// Class for testing {@link JsonIgnore} annotation
    final static class SizeClassIgnore
    {
        // note: must be public to be seen
        public int getX() { return 1; }
        @JsonIgnore public int getY() { return 9; }
    }

    /*
    //////////////////////////////////////////////
    // Main tests
    //////////////////////////////////////////////
     */

    public void testSimpleGetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new SizeClassGetter());
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(3), result.get("size"));
        assertEquals(Integer.valueOf(-17), result.get("length"));
        assertEquals(Integer.valueOf(0), result.get("value"));
    }

    public void testSimpleIgnore() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // Should see "x", not "y"
        Map<String,Object> result = writeAndMap(m, new SizeClassIgnore());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
        assertNull(result.get("y"));
    }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

    private Map<String,Object> writeAndMap(ObjectMapper m, Object value)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        m.writeValue(sw, value);
        return (Map<String,Object>) m.readValue(sw.toString(), Object.class);
    }
}
