package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit test for verifying that exceptions are properly handled (caught,
 * re-thrown or wrapped)
 */
public class TestExceptionHandling
    extends BaseTest
{
    /*
    //////////////////////////////////////////////
    // Helper classes
    //////////////////////////////////////////////
     */

    static class Bean {
        // no methods, we'll use our custom serializer
    }

    static class SerializerWithErrors
        extends JsonSerializer<Bean>
    {
        public void serialize(Bean value, JsonGenerator jgen, SerializerProvider provider)
        {
            throw new IllegalArgumentException("test string");
        }
    }

    /*
    //////////////////////////////////////////////
    // Tests
    //////////////////////////////////////////////
     */

    /**
     * Unit test that verifies that by default all exceptions except for
     * JsonMappingException are caught and wrapped.
     */
    public void testCatchAndRethrow()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();
        sf.addSpecificMapping(Bean.class, new SerializerWithErrors());
        mapper.setSerializerFactory(sf);
        try {
            StringWriter sw = new StringWriter();
            /* And just to make things more interesting, let's create
             * a nested data struct...
             */
            Bean[] b = { new Bean() };
            List<Bean[]> l = new ArrayList<Bean[]>();
            l.add(b);
            mapper.writeValue(sw, l);
            fail("Should have gotten an exception");
        } catch (JsonMappingException e) {
            // should contain original message somewhere
            verifyException(e, "test string");
            Throwable root = e.getCause();
            assertNotNull(root);
            if (!(root instanceof IllegalArgumentException)) {
                fail("Wrapped exception not IAE, but "+root.getClass());
            }
            verifyException(root, "test string");
        }
    }
}

