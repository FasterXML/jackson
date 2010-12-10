package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite that tests use of {@link JsonIgnore}
 * annotation with deserialization.
 */
public class TestAnnotationIgnore
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /// Class for testing {@link JsonIgnore} annotations with setters
    final static class SizeClassIgnore
    {
        int _x = 0;
        int _y = 0;

        public void setX(int value) { _x = value; }
        @JsonIgnore public void setY(int value) { _y = value; }

        /* Just igoring won't help a lot here; let's define a replacement
         * so that we won't get an exception for "unknown field"
         */
        @JsonProperty("y") void foobar(int value) {
            ; // nop
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimpleIgnore() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        SizeClassIgnore result = m.readValue
            ("{ \"x\":1, \"y\" : 2 }",
             SizeClassIgnore.class);
        // x should be set, y not
        assertEquals(1, result._x);
        assertEquals(0, result._y);
    }

}
