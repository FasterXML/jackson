package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.Date;
import java.sql.Timestamp;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestCustomFactory
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

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
        @Override
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

    // for [JACKSON-373]
    interface BaseInterface {
        public int getX();
        public void setX(int value);
    }

    interface SubInterface extends BaseInterface { }

    class SubImpl implements SubInterface
    {
        protected int x;
        @Override
        public int getX() { return x; }
        @Override
        public void setX(int value) { x = value; }
    }

    static class BaseInterfaceSerializer extends JsonSerializer<BaseInterface> 
    {
        @Override
        public void serialize(BaseInterface value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            jgen.writeBoolean(true);
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
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

        ObjectWriter w = mapper.writerWithType(BeanInterface.class);
        String result = w.writeValueAsString(new BeanImpl());

        assertEquals("true", result);
    }

    // Unit test for [JACKSON-373]
    public void testRegisterForTransitiveInterface() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomSerializerFactory sf = new CustomSerializerFactory();
        sf.addGenericMapping(BaseInterface.class, new BaseInterfaceSerializer());
        mapper.setSerializerFactory(sf);

        String result = mapper.writeValueAsString(new SubImpl());

        assertEquals("true", result);
    }

}
