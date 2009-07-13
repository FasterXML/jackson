package org.codehaus.jackson.map.deser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.*;

public class TestMixinsForCreators
    extends BaseMapTest
{
    /*
    ///////////////////////////////////////////////////////////
    // Helper bean classes
    ///////////////////////////////////////////////////////////
     */

    static class BaseClass
    {
        protected String _a;

        public BaseClass(String a) {
            _a = a;
        }

        public static BaseClass myFactory(String a) {
            return new BaseClass(a+"X");
        }
    }

    /**
     * Mix-in class that will effectively suppresses String constructor,
     * and marks a non-auto-detectable static method as factory method
     * as a creator.
     *<p>
     * Note that method implementations are not used for anything; but
     * we have to a class: interface won't do, as they can't have
     * constructors or static methods.
     */
    static class MixIn
    {
        @JsonIgnore protected MixIn(String s) { }

        @JsonCreator static BaseClass myFactory(String a) { return null; }
    }

    /*
    ///////////////////////////////////////////////////////////
    // Unit tests
    ///////////////////////////////////////////////////////////
     */

    public void testSimple() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        // First: test default behavior: should use constructor
        String result = m.readValue("\"string\"", String.class);
        assertEquals("string", result);

        /* Then with simple mix-in: should change to use the factory
         * method.
         */
        m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(BaseClass.class, MixIn.class);
        result = m.readValue("\"string\"", String.class);
        assertEquals("stringX", result);
    }

}
