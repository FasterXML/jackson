package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.Date;
import java.sql.Timestamp;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestCustomFactory
    extends BaseMapTest
{
    static class DummySerializer<T>
        extends JsonSerializer<T>
    {
        @Override
        public void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            jgen.writeString("XXX");
        }
    }

    interface BeanInterface {
        public int foo();
    }

    static class BeanImpl implements BeanInterface {
        public int foo() { return 13; }
    }

    static class BeanSerializer
        extends JsonSerializer<BeanInterface>
    {
        @Override
        public void serialize(BeanInterface value,
                JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            jgen.writeBoolean(true);
        }
    }
    
    /**
     * Simple test to verify specific mappings working
     */
    public void testSpecificOverrideDate() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();
        sf.addSpecificMapping(Date.class, new DummySerializer<Date>());
        mapper.setSerializerFactory(sf);

        String result = serializeAsString(mapper, new Date());
        assertEquals("\"XXX\"", result);

        // But also: sub-classes should NOT be affected.
        result = serializeAsString(mapper, new Timestamp(0L));
        assertEquals("0", result);
    }

    /**
     * Test to verify generic mapping by using super-class
     */
    public void testGenericOverrideDate() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();

        sf.addGenericMapping(Date.class, new DummySerializer<Date>());
        mapper.setSerializerFactory(sf);

        String result = serializeAsString(mapper, new Timestamp(0L));

        assertEquals("\"XXX\"", result);
    }

    // Unit test for [JACKSON-327]
    public void testRegisterForInterface() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();
        sf.addGenericMapping(BeanInterface.class, new BeanSerializer());
        mapper.setSerializerFactory(sf);

        ObjectWriter w = mapper.typedWriter(BeanInterface.class);
        String result = w.writeValueAsString(new BeanImpl());

        assertEquals("true", result);
    }
}
