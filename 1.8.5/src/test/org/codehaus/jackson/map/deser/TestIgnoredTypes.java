package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.annotate.JsonIgnoreType;
import org.codehaus.jackson.map.*;

/**
 * Test for [JACKSON-429]
 */
public class TestIgnoredTypes extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */
    
    @JsonIgnoreType
    class IgnoredType { // note: non-static, can't be deserializer
        public IgnoredType(IgnoredType src) { }
    }

    @JsonIgnoreType(false)
    static class NonIgnoredType
    {
        public int value = 13;
        public IgnoredType ignored;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testIgnoredType() throws Exception
    {
        // First: should be ok in general, even though couldn't build deserializer (due to non-static inner class):
        ObjectMapper mapper = new ObjectMapper();
        NonIgnoredType bean = mapper.readValue("{\"value\":13}", NonIgnoredType.class);
        assertNotNull(bean);
        assertEquals(13, bean.value);

        // And also ok to see something with that value; will just get ignored
        bean = mapper.readValue("{ \"ignored\":[1,2,{}], \"value\":9 }", NonIgnoredType.class);
        assertNotNull(bean);
        assertEquals(9, bean.value);
    }
}
