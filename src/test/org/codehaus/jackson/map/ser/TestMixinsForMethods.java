package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

public class TestMixinsForMethods
    extends BaseMapTest
{
    /*
    ///////////////////////////////////////////////////////////
    // Helper bean classes
    ///////////////////////////////////////////////////////////
     */

    // base class: just one visible property ('b')
    static class BaseClass
    {
        private String a, b;

        protected BaseClass() { }

        public BaseClass(String a, String b) {
            this.a = a;
            this.b = b;
        }

        @JsonProperty("b")
        public String takeB() { return b; }
    }

    /* extends, just for fun; and to show possible benefit of being
     * able to declare that a method is overridden (compile-time check
     * that our intended mix-in override will match a method)
     */
    abstract static class MixIn
        extends BaseClass
    {
        // let's make 'a' visible
        @JsonProperty String a;

        @Override
            @JsonProperty("b2")
            public abstract String takeB();

        // also: just for fun; add a "red herring"... unmatched method
        @JsonProperty abstract String getFoobar();
    }

    static class LeafClass
        extends BaseClass
    {
        public LeafClass(String a, String b) { super(a, b); }

        @Override
            @JsonIgnore
            public String takeB() { return null; }
    }
               
    interface ObjectMixIn
    {
        // and then ditto for hashCode...
        @JsonProperty public int hashCode();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Unit tests
    ///////////////////////////////////////////////////////////
     */

    /**
     * Unit test for verifying that leaf-level mix-ins work ok; 
     * that is, any annotations added properly override all annotations
     * that masked methods (fields etc) have.
     */
    public void testLeafMixin() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> result;
        BaseClass bean = new BaseClass("a1", "b2");

        // first: with no mix-ins:
        result = writeAndMap(mapper, bean);
        assertEquals(1, result.size());
        assertEquals("b2", result.get("b"));

        // then with leaf-level mix-in
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(BaseClass.class, MixIn.class);
        result = writeAndMap(mapper, bean);
        assertEquals(2, result.size());
        assertEquals("b2", result.get("b2"));
        assertEquals("a1", result.get("a"));
    }

    /**
     * Unit test for verifying that having a mix-in "between" classes
     * (overriding annotations of a base class, but being overridden
     * further by a sub-class) works as expected
     */
    public void testIntermediateMixin() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> result;
        LeafClass bean = new LeafClass("XXX", "b2");

        mapper.getSerializationConfig().addMixInAnnotations(BaseClass.class, MixIn.class);
        result = writeAndMap(mapper, bean);
        assertEquals(1, result.size());
        assertEquals("XXX", result.get("a"));
    }

    /**
     * Unit test for verifying that it is actually possible to attach
     * mix-in annotations to basic <code>Object.class</code>. This
     * will essentially apply to any and all Objects.
     */
    public void testObjectMixin() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(Object.class, ObjectMixIn.class);

        // First, with our bean...
        Map<String,Object> result = writeAndMap(mapper, new BaseClass("a", "b"));

        assertEquals(2, result.size());
        assertEquals("b", result.get("b"));
        Object ob = result.get("hashCode");
        assertNotNull(ob);
        assertEquals(Integer.class, ob.getClass());

        /* Hmmh. For plain Object.class... I suppose getClass() does
         * get serialized (and can't really be blocked either).
         * Fine.
         */
         result = writeAndMap(mapper, new Object());
        assertEquals(2, result.size());
        ob = result.get("hashCode");
        assertNotNull(ob);
        assertEquals(Integer.class, ob.getClass());
        assertEquals("java.lang.Object", result.get("class"));
    }
}
