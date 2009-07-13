package org.codehaus.jackson.map.deser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

public class TestMixinsForClass
    extends BaseMapTest
{
    /*
    ///////////////////////////////////////////////////////////
    // Helper bean classes
    ///////////////////////////////////////////////////////////
     */

    /*
    static class BaseClass
    {
        protected String _a, _b;
        protected String _c = "c";

        protected BaseClass() { }

        public BaseClass(String a) {
            _a = a;
        }

        // will be auto-detectable unless disabled:
        public String getA() { return _a; }

        @JsonProperty
        public String getB() { return _b; }

        @JsonProperty
        public String getC() { return _c; }
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    interface MixIn { }
    */

    /*
    ///////////////////////////////////////////////////////////
    // Unit tests
    ///////////////////////////////////////////////////////////
     */

    public void testClassMixInsTopLevel() throws IOException
    {
    }
}
