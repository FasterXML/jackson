package org.codehaus.jackson.map.mixins;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class TestMixinSerForClass
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

        // will be auto-detectable unless disabled:
        public String getA() { return _a; }

        @JsonProperty
        public String getB() { return _b; }

        @JsonProperty
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
    interface MixIn { }

    // test disabling of autodetect...
    @JsonAutoDetect(JsonMethod.NONE)
    interface MixInAutoDetect { }

    /*
    ///////////////////////////////////////////////////////////
    // Unit tests
    ///////////////////////////////////////////////////////////
     */

    public void testClassMixInsTopLevel() throws IOException
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

        // mid-level override; should not have any effect
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(BaseClass.class, MixIn.class);
        result = writeAndMap(mapper, new LeafClass("abc"));
        assertEquals(1, result.size());
        assertEquals("abc", result.get("a"));
    }

    public void testClassMixInsMidLevel() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> result;
        LeafClass bean = new LeafClass("xyz");
        bean._c = "c2";

        // with no mix-ins first...
        result = writeAndMap(mapper, bean);
        assertEquals(2, result.size());
        assertEquals("xyz", result.get("a"));
        assertEquals("c2", result.get("c"));

        // then with working mid-level override, which effectively suppresses 'a'
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(BaseClass.class, MixInAutoDetect.class);
        result = writeAndMap(mapper, bean);
        assertEquals(1, result.size());
        assertEquals("c2", result.get("c"));
    }
}
