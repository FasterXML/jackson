package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that it is possible to annotate
 * various kinds of things with {@link JsonCreator} annotation.
 */
public class TestCreators
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes, simple
    /**********************************************************
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

    /**
     * Another simple constructor, but with bit more unusual argument
     * type
     */
    static class BooleanConstructorBean {
        Boolean b;
        @JsonCreator protected BooleanConstructorBean(Boolean b) {
            this.b = b;
        }
    }

    @SuppressWarnings("unused")
    private static class BooleanConstructorBean2 {
        boolean b;
        @JsonCreator protected BooleanConstructorBean2(boolean b) {
            this.b = b;
        }
    }
    
    static class DoubleConstructorBean {
        Double d; // cup?
        @JsonCreator protected DoubleConstructorBean(Double d) {
            this.d = d;
        }
    }

    static class FactoryBean {
        double d; // teehee

        private FactoryBean(double value, boolean dummy) { d = value; }

        @JsonCreator protected static FactoryBean createIt(@JsonProperty("f") double value) {
            return new FactoryBean(value, true);
        }
    }

    static class LongFactoryBean {
        long value;

        private LongFactoryBean(long v) { value = v; }

        @JsonCreator static protected LongFactoryBean valueOf(long v) {
            return new LongFactoryBean(v);
        }
    }

    static class StringFactoryBean {
        String value;

        private StringFactoryBean(String v, boolean dummy) { value = v; }

        @JsonCreator static protected StringFactoryBean valueOf(String v) {
            return new StringFactoryBean(v, true);
        }
    }

    static class FactoryBeanMixIn { // static just to be able to use static methods
        /**
         * Note: signature (name and parameter types) must match; but
         * only annotations will be used, not code or such. And use
         * is by augmentation, so we only need to add things to add
         * or override.
         */
        static FactoryBean createIt(@JsonProperty("mixed") double xyz) {
            return null;
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

    static class MultiBean {
        Object value;

        @JsonCreator public MultiBean(int v) { value = v; }
        @JsonCreator public MultiBean(double v) { value = v; }
        @JsonCreator public MultiBean(String v) { value = v; }
        @JsonCreator public MultiBean(boolean v) { value = v; }
    }
    
    /*
    /**********************************************************
    /* Annotated helper classes, mixed (creator and props)
    /**********************************************************
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
    //////////////////////////////////////////////
    // Annotated helper classes for Maps
    //////////////////////////////////////////////
     */

    @SuppressWarnings("serial")
    static class MapWithCtor extends HashMap<Object,Object>
    {
        final int _number;
        String _text = "initial";

        MapWithCtor() { this(-1, "default"); }

        @JsonCreator
            public MapWithCtor(@JsonProperty("number") int nr,
                               @JsonProperty("text") String t)
        {
            _number = nr;
            _text = t;
        }
    }

    @SuppressWarnings("serial")
    static class MapWithFactory extends TreeMap<Object,Object>
    {
        Boolean _b;

        private MapWithFactory(Boolean b) {
            _b = b;
        }

        @JsonCreator
            static MapWithFactory createIt(@JsonProperty("b") Boolean b)
        {
            return new MapWithFactory(b);
        }
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

    public void testSimpleDoubleConstructor() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Double exp = new Double("0.25");
        DoubleConstructorBean bean = m.readValue(exp.toString(), DoubleConstructorBean.class);
        assertEquals(exp, bean.d);
    }

    public void testSimpleBooleanConstructor() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        BooleanConstructorBean bean = m.readValue(" true ", BooleanConstructorBean.class);
        assertEquals(Boolean.TRUE, bean.b);

        BooleanConstructorBean2 bean2 = m.readValue(" true ", BooleanConstructorBean2.class);
        assertTrue(bean2.b);
    }

    public void testSimpleFactory() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        FactoryBean bean = m.readValue("{ \"f\" : 0.25 }", FactoryBean.class);
        assertEquals(0.25, bean.d);
    }

    public void testLongFactory() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        long VALUE = 123456789000L;
        LongFactoryBean bean = m.readValue(String.valueOf(VALUE), LongFactoryBean.class);
        assertEquals(VALUE, bean.value);
    }

    public void testStringFactory() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String str = "abc";
        StringFactoryBean bean = m.readValue(quote(str), StringFactoryBean.class);
        assertEquals(str, bean.value);
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

    /**
     * Test to verify that multiple creators may co-exist, iff
     * they use different JSON type as input
     */
    public void testMultipleCreators() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        MultiBean bean = m.readValue("123", MultiBean.class);
        assertEquals(Integer.valueOf(123), bean.value);
        bean = m.readValue(quote("abc"), MultiBean.class);
        assertEquals("abc", bean.value);
        bean = m.readValue("0.25", MultiBean.class);
        assertEquals(Double.valueOf(0.25), bean.value);
    }

    /*
    /**********************************************************
    /* Test methods, valid cases, deferred, no mixins
    /**********************************************************
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

    public void testFactoryCreatorWithMixin() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(CreatorBean.class, MixIn.class);
        CreatorBean bean = m.readValue
            ("{ \"a\" : \"xyz\", \"x\" : 12 }", CreatorBean.class);
        assertEquals(11, bean.x);
        assertEquals("factory:xyz", bean.a);
    }

    public void testFactoryCreatorWithRenamingMixin() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(FactoryBean.class, FactoryBeanMixIn.class);
        // override changes property name from "f" to "mixed"
        FactoryBean bean = m.readValue("{ \"mixed\" :  20.5 }", FactoryBean.class);
        assertEquals(20.5, bean.d);
    }

    /*
    /////////////////////////////////////////////////////
    // Test methods, valid cases, Map with creator
    // (to test [JACKSON-153])
    /////////////////////////////////////////////////////
     */

    public void testMapWithConstructor() throws Exception
    {
        MapWithCtor result = new ObjectMapper().readValue
            ("{\"text\":\"abc\", \"entry\":true, \"number\":123, \"xy\":\"yx\"}",
             MapWithCtor.class);
        // regular Map entries:
        assertEquals(Boolean.TRUE, result.get("entry"));
        assertEquals("yx", result.get("xy"));
        assertEquals(2, result.size());
        // then ones passed via constructor
        assertEquals("abc", result._text);
        assertEquals(123, result._number);
    }

    public void testMapWithFactory() throws Exception
    {
        MapWithFactory result = new ObjectMapper().readValue
            ("{\"x\":\"...\",\"b\":true  }",
             MapWithFactory.class);
        assertEquals("...", result.get("x"));
        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result._b);
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
            /*BrokenBean bean =*/ m.readValue("{ \"x\" : 42 }", BrokenBean.class);
        } catch (JsonMappingException je) {
            verifyException(je, "has no property name");
        }
    }
}
