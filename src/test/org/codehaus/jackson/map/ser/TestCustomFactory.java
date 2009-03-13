package org.codehaus.jackson.map.ser;

import main.BaseTest;

import java.io.*;
import java.util.Date;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ser.*;

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

    public void testUtilDateOverride() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();
        sf.addSpecificMapping(Date.class, new DummySerializer<Date>());
        mapper.setSerializerFactory(sf);

        String result = serializeAsString(mapper, new Date());

        assertEquals("\"XXX\"", result);
    }
}
