package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tests use of Annotations for
 * bean serialization.
 */
public class TestAnnotationInheritance
    extends BaseTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    /// Base class for testing {@link JsonProperty} annotations
    static class BasePojo
    {
        @JsonProperty public int width() { return 3; }
        @JsonProperty public int length() { return 7; }
    }

    /**
     * It should also be possible to specify annotations on interfaces,
     * to be implemented by classes. This should not only work when interface
     * is used (which may be the case for de-serialization) but also
     * when implementing class is used and overrides methods. In latter
     * case overriding methods should still "inherit" annotations -- this
     * is not something JVM runtime provides, but Jackson class
     * instrospector does.
     */
    interface PojoInterface
    {
        @JsonProperty int width();
        @JsonProperty int length();
    }

    /**
     * Sub-class for testing that inheritance is handled properly
     * wrt annotations.
     */
    static class PojoSubclass extends BasePojo
    {
        /**
         * Should still be recognized as a Getter here.
         */
        @Override
        public int width() { return 9; }
    }

    static class PojoImpl implements PojoInterface
    {
        // Both should be recognized as getters here

        @Override
        public int width() { return 1; }
        @Override
        public int length() { return 2; }

        public int getFoobar() { return 5; }
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    public void testSimpleGetterInheritance() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new PojoSubclass());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(7), result.get("length"));
        assertEquals(Integer.valueOf(9), result.get("width"));
    }

    public void testSimpleGetterInterfaceImpl() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new PojoImpl());
        // should get 2 from interface, and one more from impl itself
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get("foobar"));
        assertEquals(Integer.valueOf(1), result.get("width"));
        assertEquals(Integer.valueOf(2), result.get("length"));
    }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
	private Map<String,Object> writeAndMap(ObjectMapper m, Object value)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        m.writeValue(sw, value);
        return (Map<String,Object>) m.readValue(sw.toString(), Object.class);
    }
}
