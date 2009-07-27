package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that it is possible to annotate
 * various kinds of things with {@link @JsonCreator} annotation.
 */
public class TestCreators
    extends BaseMapTest
{
    /*
    //////////////////////////////////////////////
    // Annotated helper classes, simple
    //////////////////////////////////////////////
     */

    /**
     * Simple(st) possible demonstration of using annotated
     * constructors
     */
    static class ConstructorBean {
        int x;

        @JsonCreator protected ConstructorBean(@JsonProperty("x") int x) {
            this.x = x;
        }
    }

    static class FactoryBean {
        double d; // teehee
        @JsonCreator protected FactoryBean(@JsonProperty("f") double value) {
            d = value;
        }
    }

    /**
     * Simple demonstration of INVALID construtor annotation (only
     * defining name for first arg)
     */
    static class BrokenBean {
        @JsonCreator protected BrokenBean(@JsonProperty("a") int a,
                                          int b) {
        }
    }

    /**
     * Bean that defines both creator and factory methor as
     * creators. Constructors have priority; but it is possible
     * to hide it using mix-in annotations.
     */
    static class CreatorBean
    {
        String a;
        int x;

        @JsonCreator
        protected CreatorBean(@JsonProperty("a") String paramA,
                              @JsonProperty("x") int paramX)
        {
            a = "ctor:"+paramA;
            x = 1+paramX;
        }

        private CreatorBean(String a, int x, boolean dummy) {
            this.a = a;
            this.x = x;
        }

        @JsonCreator
        public static CreatorBean buildMeUpButterCup(@JsonProperty("a") String paramA,
                                                     @JsonProperty("x") int paramX)
        {
            return new CreatorBean("factory:"+paramA, paramX-1, false);
        }
    }

    /**
     * Class for sole purpose of hosting mix-in annotations.
     * Couple of things to note: (a) MUST be static class (non-static
     * get implicit pseudo-arg, 'this';
     * (b) for factory methods, must have static to match (part of signature)
     */
    abstract static class MixIn {
        @JsonIgnore private MixIn(String a, int x) { }
    }

    /*
    //////////////////////////////////////////////////////
    // Annotated helper classes, mixed (creator and props)
    //////////////////////////////////////////////////////
     */

    /**
     * Test bean for ensuring that constructors can be mixed with setters
     */
    static class ConstructorAndPropsBean
    {
        final int a, b;
        boolean c;

        @JsonCreator protected ConstructorAndPropsBean(@JsonProperty("a") int a,
                                                       @JsonProperty("b") int b)
        {
            this.a = a;
            this.b = b;
        }

        public void setC(boolean value) { c = value; }
    }

    /**
     * Test bean for ensuring that factory methods can be mixed with setters
     */
    static class FactoryAndPropsBean
    {
        boolean[] arg1;
        int arg2, arg3;

        @JsonCreator protected FactoryAndPropsBean(@JsonProperty("a") boolean[] arg)
        {
            arg1 = arg;
        }

        public void setB(int value) { arg2 = value; }
        public void setC(int value) { arg3 = value; }
    }

    static class DeferredConstructorAndPropsBean
    {
        final int[] createA;
        String propA = "xyz";
        String propB;

        @JsonCreator
        public DeferredConstructorAndPropsBean(@JsonProperty("createA") int[] a)
        {
            createA = a;
        }
        public void setPropA(String a) { propA = a; }
        public void setPropB(String b) { propB = b; }
    }

    static class DeferredFactoryAndPropsBean
    {
        String prop, ctor;

        @JsonCreator DeferredFactoryAndPropsBean(@JsonProperty("ctor") String str)
        {
            ctor = str;
        }

        public void setProp(String str) { prop = str; }
    }

    /*
    /////////////////////////////////////////////////////
    // Test methods, valid cases, non-deferred, no-mixins
    /////////////////////////////////////////////////////
     */

    public void testSimpleConstructor() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        ConstructorBean bean = m.readValue("{ \"x\" : 42 }", ConstructorBean.class);
        assertEquals(42, bean.x);
    }

    public void testSimpleFactory() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        FactoryBean bean = m.readValue("{ \"f\" : 0.25 }", FactoryBean.class);
        assertEquals(0.25, bean.d);
    }

    public void testConstructorCreator() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        CreatorBean bean = m.readValue
            ("{ \"a\" : \"xyz\", \"x\" : 12 }", CreatorBean.class);
        assertEquals(13, bean.x);
        assertEquals("ctor:xyz", bean.a);
    }

    public void testConstructorAndProps() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        ConstructorAndPropsBean bean = m.readValue
            ("{ \"a\" : \"1\", \"b\": 2, \"c\" : true }", ConstructorAndPropsBean.class);
        assertEquals(1, bean.a);
        assertEquals(2, bean.b);
        assertEquals(true, bean.c);
    }

    public void testFactoryAndProps() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        FactoryAndPropsBean bean = m.readValue
            ("{ \"a\" : [ false, true, false ], \"b\": 2, \"c\" : -1 }", FactoryAndPropsBean.class);
        assertEquals(2, bean.arg2);
        assertEquals(-1, bean.arg3);
        boolean[] arg1 = bean.arg1;
        assertNotNull(arg1);
        assertEquals(3, arg1.length);
        assertFalse(arg1[0]);
        assertTrue(arg1[1]);
        assertFalse(arg1[2]);
    }

    /*
    /////////////////////////////////////////////////////
    // Test methods, valid cases, deferred, no mixins
    /////////////////////////////////////////////////////
     */

    public void testDeferredConstructorAndProps() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        DeferredConstructorAndPropsBean bean = m.readValue
            ("{ \"propB\" : \"...\", \"createA\" : [ 1 ], \"propA\" : null }",
             DeferredConstructorAndPropsBean.class);

        assertEquals("...", bean.propB);
        assertNull(bean.propA);
        assertNotNull(bean.createA);
        assertEquals(1, bean.createA.length);
        assertEquals(1, bean.createA[0]);
    }

    public void testDeferredFactoryAndProps() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        DeferredFactoryAndPropsBean bean = m.readValue
            ("{ \"prop\" : \"1\", \"ctor\" : \"2\" }", DeferredFactoryAndPropsBean.class);
        assertEquals("1", bean.prop);
        assertEquals("2", bean.ctor);
    }

    /*
    /////////////////////////////////////////////////////
    // Test methods, valid cases, mixins
    /////////////////////////////////////////////////////
     */

    public void testFactoryCreator() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(CreatorBean.class, MixIn.class);
        CreatorBean bean = m.readValue
            ("{ \"a\" : \"xyz\", \"x\" : 12 }", CreatorBean.class);
        assertEquals(11, bean.x);
        assertEquals("factory:xyz", bean.a);
    }

    /*
    //////////////////////////////////////////////
    // Test methods, invalid/broken cases
    //////////////////////////////////////////////
     */

    public void testBrokenConstructor() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        try {
            BrokenBean bean = m.readValue("{ \"x\" : 42 }", BrokenBean.class);
        } catch (JsonMappingException je) {
            verifyException(je, "has no property name");
        }
    }
}
