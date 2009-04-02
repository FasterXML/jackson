package org.codehaus.jackson.map.deser;

import java.io.*;
import java.util.Date;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Test to check that customization using {@link CustomDeserializerFactory}
 * works as expected.
 */
public class TestCustomFactory
    extends BaseMapTest
{
    static class DummyDeserializer<T>
        extends JsonDeserializer<T>
    {
        final T value;

        public DummyDeserializer(T v) { value = v; }

        public T deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // need to skip, if structured...
            jp.skipChildren();
            return value;
        }
    }

    public void testDateOverride() throws Exception
    {
        Date expResult = new Date(3L);
        ObjectMapper mapper = new ObjectMapper();
        CustomDeserializerFactory sf = new CustomDeserializerFactory();
        sf.addSpecificMapping(Date.class, new DummyDeserializer<Date>(expResult));
        mapper.setDeserializerProvider(new StdDeserializerProvider(sf));

        Date result = mapper.readValue("123", Date.class);
        assertEquals(expResult.getTime(), result.getTime());
    }
}
