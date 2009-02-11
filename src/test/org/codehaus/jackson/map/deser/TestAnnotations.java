package org.codehaus.jackson.map.deser;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tests use of Annotations for
 * bean deserialization.
 */
public class TestAnnotations
    extends BaseTest
{
    /*
    //////////////////////////////////////////////
    // Annotated helper classes
    //////////////////////////////////////////////
     */

    /// Class for testing {@link JsonSetter} annotations
    final static class SizeClassSetter
    {
        int _size;
        int _length;
        int _other;

        @JsonSetter public void size(int value) { _size = value; }
        @JsonSetter("length") public void foobar(int value) { _length = value; }

        // note: need not be public if annotated
        @JsonSetter protected void other(int value) { _other = value; }
    }

    final static class SizeClassIgnore
    {
        int _x = 0;
        int _y = 0;

        public void setX(int value) { _x = value; }
        @JsonIgnore public void setY(int value) { _y = value; }
    }

    /*
    //////////////////////////////////////////////
    // Main tests
    //////////////////////////////////////////////
     */

    public void testSimpleSetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        SizeClassSetter result = m.readValue
            ("{ \"other\":3, \"size\" : 2, \"length\" : -999 }",
             SizeClassSetter.class);
                                             
        assertEquals(3, result._other);
        assertEquals(2, result._size);
        assertEquals(-999, result._length);
    }

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

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

}
