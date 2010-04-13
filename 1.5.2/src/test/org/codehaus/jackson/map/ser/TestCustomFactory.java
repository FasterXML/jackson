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
        public void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            jgen.writeString("XXX");
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
}
