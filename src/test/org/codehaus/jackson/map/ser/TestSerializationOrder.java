package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that constraints on ordering of serialized
 * properties are held.
 */
public class TestSerializationOrder
    extends BaseMapTest
{
    /*
    /*********************************************
    /* Annotated helper classes
    /*********************************************
     */

    static class BeanWithCreator
    {
        public int a;
        public int b;
        public int c;

        @JsonCreator public BeanWithCreator(@JsonProperty("c") int c, @JsonProperty("a") int a) {
            this.a = a;
            this.c = c;
        }
    }

    @JsonPropertyOrder({"c", "a", "b"})
    static class BeanWithOrder
    {
        public int d, b, a, c;
        
        public BeanWithOrder(int a, int b, int c, int d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

    @JsonPropertyOrder(value={"d"}, alphabetic=true)
    static class SubBeanWithOrder extends BeanWithOrder
    {
        public SubBeanWithOrder(int a, int b, int c, int d) {
            super(a, b, c, d);
        }
    }

    @JsonPropertyOrder({"b", "a",
        // note: including non-existant properties is fine (has no effect, but not an error)
        "foobar",
        "c"
    })
    static class OrderMixIn { }

    @JsonPropertyOrder(value={"a","b","x","z"})
    static class BeanFor268 { // testing [JACKSON-268]
    	@JsonProperty("a") public String xA = "a";
    	@JsonProperty("z") public String aZ = "z";
    	@JsonProperty("b") public String xB() { return "b"; }
    	@JsonProperty("x") public String aX() { return "x"; }
    }

    static class BeanFor459 {
        public int d = 4;
        public int c = 3;
        public int b = 2;
        public int a = 1;
    }
    
    /*
    /*********************************************
    /* Unit tests
    /*********************************************
     */

    // Test for [JACKSON-170]
    public void testImplicitOrderByCreator() throws Exception
    {
        assertEquals("{\"c\":1,\"a\":2,\"b\":0}", serializeAsString(new BeanWithCreator(1, 2)));
    }

    public void testExplicitOrder() throws Exception
    {
        assertEquals("{\"c\":3,\"a\":1,\"b\":2,\"d\":4}", serializeAsString(new BeanWithOrder(1, 2, 3, 4)));
    }

    public void testAlphabeticOrder() throws Exception
    {
        assertEquals("{\"d\":4,\"a\":1,\"b\":2,\"c\":3}", serializeAsString(new SubBeanWithOrder(1, 2, 3, 4)));
    }


    public void testOrderWithMixins() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.getSerializationConfig().addMixInAnnotations(BeanWithOrder.class, OrderMixIn.class);
        assertEquals("{\"b\":2,\"a\":1,\"c\":3,\"d\":4}", serializeAsString(m, new BeanWithOrder(1, 2, 3, 4)));
    }

    // Test for [JACKSON-268]
    public void testOrderWrt268() throws Exception
    {
        assertEquals("{\"a\":\"a\",\"b\":\"b\",\"x\":\"x\",\"z\":\"z\"}",
        		serializeAsString(new BeanFor268()));
    }

    // Test for [JACKSON-459]
    public void testOrderWithFeature() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
        assertEquals("{\"a\":1,\"b\":2,\"c\":3,\"d\":4}", serializeAsString(m, new BeanFor459()));
    }
}
