package org.codehaus.jackson.map.module;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ser.std.ScalarSerializerBase;
import org.codehaus.jackson.map.ser.std.SerializerBase;

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

    interface Base {
        public String getText();
    }
    
    static class Impl1 implements Base {
        @Override
        public String getText() { return "1"; }
    }

    static class Impl2 extends Impl1 {
        @Override
        public String getText() { return "2"; }
    }

    static class BaseSerializer extends ScalarSerializerBase<Base>
    {
        public BaseSerializer() { super(Base.class); }
        
        @Override
        public void serialize(Base value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString("Base:"+value.getText());
        }
    }

    static class MixableBean {
        public int a = 1;
        public int b = 2;
        public int c = 3;
    }

    @JsonPropertyOrder({"c", "a", "b"})
    static class MixInForOrder { }
    
    /*
    /**********************************************************
    /* Unit tests; first, verifying need for custom handlers
    /**********************************************************
     */

    /**
     * Basic test to ensure we do not have functioning default
     * serializers for custom types used in tests.
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

    protected static class MySimpleSerializers extends SimpleSerializers { }
    protected static class MySimpleDeserializers extends SimpleDeserializers { }

    /**
     * Test module which uses custom 'serializers' and 'deserializers' container; used
     * to trigger type problems.
     */
    protected static class MySimpleModule extends SimpleModule
    {
        public MySimpleModule(String name, Version version) {
            super(name, version);
            _deserializers = new MySimpleDeserializers();
            _serializers = new MySimpleSerializers();
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

    // for [JACKSON-550]
    public void testSimpleInterfaceSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", new Version(1, 0, 0, null));
        mod.addSerializer(new BaseSerializer());
        mapper.registerModule(mod);
        assertEquals(quote("Base:1"), mapper.writeValueAsString(new Impl1()));
        assertEquals(quote("Base:2"), mapper.writeValueAsString(new Impl2()));
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
 
    // Simple verification of [JACKSON-455]
    public void testMultipleModules() throws Exception
    {
        MySimpleModule mod1 = new MySimpleModule("test1", new Version(1, 0, 0, null));
        SimpleModule mod2 = new SimpleModule("test2", new Version(1, 0, 0, null));
        mod1.addSerializer(SimpleEnum.class, new SimpleEnumSerializer());
        mod1.addDeserializer(CustomBean.class, new CustomBeanDeserializer());
        mod2.addDeserializer(SimpleEnum.class, new SimpleEnumDeserializer());
        mod2.addSerializer(CustomBean.class, new CustomBeanSerializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(mod1);
        mapper.registerModule(mod2);
        assertEquals(quote("b"), mapper.writeValueAsString(SimpleEnum.B));
        SimpleEnum result = mapper.readValue(quote("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);

        // also let's try it with different order of registration, just in case
        mapper = new ObjectMapper();
        mapper.registerModule(mod2);
        mapper.registerModule(mod1);
        assertEquals(quote("b"), mapper.writeValueAsString(SimpleEnum.B));
        result = mapper.readValue(quote("a"), SimpleEnum.class);
        assertSame(SimpleEnum.A, result);
    }

    /*
    /**********************************************************
    /* Unit tests; other
    /**********************************************************
     */
    
    // [JACKSON-644]: ability to register mix-ins
    public void testMixIns() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.setMixInAnnotation(MixableBean.class, MixInForOrder.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        Map<String,Object> props = this.writeAndMap(mapper, new MixableBean());
        assertEquals(3, props.size());
        assertEquals(Integer.valueOf(3), props.get("c"));
        assertEquals(Integer.valueOf(1), props.get("a"));
        assertEquals(Integer.valueOf(2), props.get("b"));
    }
}

