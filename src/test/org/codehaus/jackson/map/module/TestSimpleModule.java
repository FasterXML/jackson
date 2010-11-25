package org.codehaus.jackson.map.module;

import java.io.IOException;
import java.lang.reflect.Type;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ser.SerializerBase;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.type.JavaType;

public class TestSimpleModule extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes; simple beans and their handlers
    /**********************************************************
     */
    
    /**
     * Trivial bean that requires custom serializer and deserializer
     */
    final static class CustomBean
    {
        protected String str;
        protected int num;
        
        public CustomBean(String s, int i) {
            str = s;
            num = i;
        }
    }

    static enum SimpleEnum { A, B; }
    
    // Extend SerializerBase to get access to declared handledType
    static class CustomBeanSerializer extends SerializerBase<CustomBean>
    {
        public CustomBeanSerializer() { super(CustomBean.class); }

        @Override
        public void serialize(CustomBean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            // We will write it as a String, with '|' as delimiter
            jgen.writeString(value.str + "|" + value.num);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException {
            return null;
        }
    }
    
    static class CustomBeanDeserializer extends JsonDeserializer<CustomBean>
    {
        @Override
        public CustomBean deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            String text = jp.getText();
            int ix = text.indexOf('|');
            if (ix < 0) {
                throw new IOException("Failed to parse String value of \""+text+"\"");
            }
            String str = text.substring(0, ix);
            int num = Integer.parseInt(text.substring(ix+1));
            return new CustomBean(str, num);
        }
    }

    static class SimpleEnumSerializer extends SerializerBase<SimpleEnum>
    {
        public SimpleEnumSerializer() { super(SimpleEnum.class); }

        @Override
        public void serialize(SimpleEnum value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            jgen.writeString(value.name().toLowerCase());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException {
            return null;
        }
    }

    static class SimpleEnumDeserializer extends JsonDeserializer<SimpleEnum>
    {
        @Override
        public SimpleEnum deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return SimpleEnum.valueOf(jp.getText().toUpperCase());
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests; first, verifying need for custom handlers
    /**********************************************************
     */

    /**
     * Basic test to ensure we do not have functioning default
     * serializers for custom types used in tets.
     */
    public void testWithoutModule()
    {
        ObjectMapper mapper = new ObjectMapper();
        // first: serialization failure:
        try {
            mapper.writeValueAsString(new CustomBean("foo", 3));
            fail("Should have caused an exception");
        } catch (IOException e) {
            verifyException(e, "No serializer found");
        }

        // then deserialization
        try {
            mapper.readValue("{\"str\":\"ab\",\"num\":2}", CustomBean.class);
            fail("Should have caused an exception");
        } catch (IOException e) {
            verifyException(e, "No suitable constructor found");
        }
    }

    /*
    /**********************************************************
    /* Unit tests; simple serializers
    /**********************************************************
     */
    
    public void testSimpleBeanSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", new Version(1, 0, 0, null));
        mod.addSerializer(new CustomBeanSerializer());
        mapper.registerModule(mod);
        assertEquals(quote("abcde|5"), mapper.writeValueAsString(new CustomBean("abcde", 5)));
    }

    public void testSimpleEnumSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", new Version(1, 0, 0, null));
        mod.addSerializer(new SimpleEnumSerializer());
        mapper.registerModule(mod);
        assertEquals(quote("b"), mapper.writeValueAsString(SimpleEnum.B));
    }
    
    /*
    /**********************************************************
    /* Unit tests; simple deserializers
    /**********************************************************
     */
    
    public void testSimpleBeanDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", new Version(1, 0, 0, null));
        mod.addDeserializer(CustomBean.class, new CustomBeanDeserializer());
        mapper.registerModule(mod);
        CustomBean bean = mapper.readValue(quote("xyz|3"), CustomBean.class);
        assertEquals("xyz", bean.str);
        assertEquals(3, bean.num);
    }

    public void testSimpleEnumDeserializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", new Version(1, 0, 0, null));
        mod.addDeserializer(SimpleEnum.class, new SimpleEnumDeserializer());
        mapper.registerModule(mod);
        SimpleEnum result = mapper.readValue(quote("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);
    }
    
}
