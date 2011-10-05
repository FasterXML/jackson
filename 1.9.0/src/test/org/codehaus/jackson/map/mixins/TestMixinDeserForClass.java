package org.codehaus.jackson.map.mixins;

import java.io.*;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.*;

public class TestMixinDeserForClass
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */

    static class BaseClass
    {
        /* property that is always found; but has lower priority than
         * setter method if both found
         */
        @JsonProperty
        public String a;

        // setter that may or may not be auto-detected
        public void setA(String v) { a = "XXX"+v; }
    }

    @JsonAutoDetect(JsonMethod.ALL)
    static class LeafClass
        extends BaseClass { }

    @JsonAutoDetect(JsonMethod.NONE)
    interface MixIn { }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testClassMixInsTopLevel() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        // First: test default behavior: should use setter
        LeafClass result = m.readValue("{\"a\":\"value\"}", LeafClass.class);
        assertEquals("XXXvalue", result.a);

        /* Then with leaf-level mix-in; without (method) auto-detect, should
         * use field
         */
        m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(LeafClass.class, MixIn.class);
        result = m.readValue("{\"a\":\"value\"}", LeafClass.class);
        assertEquals("value", result.a);
    }

    /* and then a test for mid-level mixin; should have no effect
     * when deserializing leaf (but will if deserializing base class)
     */
    public void testClassMixInsMidLevel() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(BaseClass.class, MixIn.class);
        {
            BaseClass result = m.readValue("{\"a\":\"value\"}", BaseClass.class);
            assertEquals("value", result.a);
        }

        // whereas with leaf class, reverts to default
        {
            LeafClass result = m.readValue("{\"a\":\"value\"}", LeafClass.class);
            assertEquals("XXXvalue", result.a);
        }
    }

    /* Also: when mix-in attached to Object.class, will work, if
     * visible (similar to mid-level, basically)
     */
    public void testClassMixInsForObjectClass() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(Object.class, MixIn.class);
        // will be seen for BaseClass
        {
            BaseClass result = m.readValue("{\"a\":\"\"}", BaseClass.class);
            assertEquals("", result.a);
        }

        // but LeafClass still overrides
        {
            LeafClass result = m.readValue("{\"a\":\"\"}", LeafClass.class);
            assertEquals("XXX", result.a);
        }
    }
}
