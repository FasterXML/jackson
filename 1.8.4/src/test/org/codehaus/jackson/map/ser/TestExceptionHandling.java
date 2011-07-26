package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit test for verifying that exceptions are properly handled (caught,
 * re-thrown or wrapped, depending)
 * with Object serialization.
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
        @Override
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
        } catch (IOException e) {
            // should contain original message somewhere
            verifyException(e, "test string");
            Throwable root = e.getCause();
            assertNotNull(root);

            if (!(root instanceof IllegalArgumentException)) {
                fail("Wrapped exception not IAE, but "+root.getClass());
            }
        }
    }

    /**
     * Unit test for verifying that regular IOExceptions are not wrapped
     * but are passed through as is.
     */
    public void testExceptionWithSimpleMapper()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            BrokenStringWriter sw = new BrokenStringWriter("TEST");
            mapper.writeValue(sw, createLongObject());
            fail("Should have gotten an exception");
        } catch (IOException e) {
            verifyException(e, IOException.class, "TEST");
        }
    }

    public void testExceptionWithMapperAndGenerator()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = new MappingJsonFactory();
        BrokenStringWriter sw = new BrokenStringWriter("TEST");
        JsonGenerator jg = f.createJsonGenerator(sw);

        try {
            mapper.writeValue(jg, createLongObject());
            fail("Should have gotten an exception");
        } catch (IOException e) {
            verifyException(e, IOException.class, "TEST");
        }
    }

    public void testExceptionWithGeneratorMapping()
        throws Exception
    {
        JsonFactory f = new MappingJsonFactory();
        JsonGenerator jg = f.createJsonGenerator(new BrokenStringWriter("TEST"));
        try {
            jg.writeObject(createLongObject());
            fail("Should have gotten an exception");
        } catch (Exception e) {
            verifyException(e, IOException.class, "TEST");
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////////////////
     */

    void verifyException(Exception e, Class<?> expType, String expMsg)
        throws Exception
    {
        if (e.getClass() != expType) {
            fail("Expected exception of type "+expType.getName()+", got "+e.getClass().getName());
        }
        if (expMsg != null) {
            verifyException(e, expMsg);
        }
    }

    Object createLongObject()
    {
        List<Object> leaf = new ArrayList<Object>();
        for (int i = 0; i < 256; ++i) {
            leaf.add(Integer.valueOf(i));
        }
        List<Object> root = new ArrayList<Object>(256);
        for (int i = 0; i < 256; ++i) {
            root.add(leaf);
        }
        return root;
    }
}

