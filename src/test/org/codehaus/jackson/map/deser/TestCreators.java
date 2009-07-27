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
    // Annotated helper classes
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

    // Class for sole purpose of hosting mix-in annotations
    abstract class MixIn {
        @JsonIgnore private MixIn(String a, int x) { }
    }

    /*
    //////////////////////////////////////////////
    // Test methods, valid cases
    //////////////////////////////////////////////
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

    public void testFactoryCreator() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(CreatorBean.class, MixIn.class);
        CreatorBean bean = m.readValue
            ("{ \"a\" : \"xyz\", \"x\" : 12 }", CreatorBean.class);
        assertEquals(11, bean.x);
        assertEquals("ctor:xyz", bean.a);
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
