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
        public CreatorBean buildMeUpButterCup(@JsonProperty("a") String paramA,
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
    // Test methods
    //////////////////////////////////////////////
     */

    public void testConstructorCreator() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        CreatorBean bean = m.readValue
            ("{ \"a\" : \"xyz\", \"x\" : 12 }", CreatorBean.class);
        assertEquals(14, bean.x);
        assertEquals("ctor:xyz", bean.a);
    }

    public void testFactoryCreator() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(CreatorBean.class, MixIn.class);
        CreatorBean bean = m.readValue
            ("{ \"a\" : \"xyz\", \"x\" : 12 }", CreatorBean.class);
        assertEquals(14, bean.x);
        assertEquals("ctor:xyz", bean.a);
    }


}
