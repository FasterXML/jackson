package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class TestMixinsForClass
    extends BaseMapTest
{
    /*
    ///////////////////////////////////////////////////////////
    // Helper bean classes
    ///////////////////////////////////////////////////////////
     */

    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
    static class BaseClass
    {
        protected String _a, _b;
        protected String _c = "c";

        protected BaseClass() { }

        public BaseClass(String a) {
            _a = a;
        }

        public String getA() { return _a; }
        public String getB() { return _b; }
        public String getC() { return _c; }
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
    static class LeafClass
        extends BaseClass
    {
        public LeafClass() { super(null); }

        public LeafClass(String a) {
            super(a);
        }
    }

    /**
     * This interface only exists to add "mix-in annotations": that is, any
     * annotations it has can be virtually added to mask annotations
     * of other classes
     */
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    static interface MixIn {
    }

    /*
    ///////////////////////////////////////////////////////////
    // Unit tests
    ///////////////////////////////////////////////////////////
     */

    public void testClassMixIns() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> result;

        // first: with no mix-ins:
        result = writeAndMap(mapper, new LeafClass("abc"));
        assertEquals(1, result.size());
        assertEquals("abc", result.get("a"));

        // then with top-level override
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(LeafClass.class, MixIn.class);
        result = writeAndMap(mapper, new LeafClass("abc"));
        assertEquals(2, result.size());
        assertEquals("abc", result.get("a"));
        assertEquals("c", result.get("c"));

        // then mid-level override; should not have any effect
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(BaseClass.class, MixIn.class);
        result = writeAndMap(mapper, new LeafClass("abc"));
        assertEquals(1, result.size());
        assertEquals("abc", result.get("a"));
    }

    /*
    ///////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////
     */
}
