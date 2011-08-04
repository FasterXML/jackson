package org.codehaus.jackson.jaxb;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Unit tests to ensure that handling of writing of null properties (or not)
 * works when using JAXB annotation introspector.
 * Mostly to test out [JACKSON-309].
 */
public class TestJaxbNullProperties
    extends BaseJaxbTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    public static class Bean
    {
       public String empty;

       public String x = "y";
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // Testing [JACKSON-309]
     public void testNullProps() throws Exception
     {
         ObjectMapper mapper = getJaxbMapper();
         mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
         assertEquals("{\"x\":\"y\"}", mapper.writeValueAsString(new Bean()));
     }
}
