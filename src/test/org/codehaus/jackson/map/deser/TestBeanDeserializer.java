package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.BeanDeserializerModifier;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.module.SimpleModule;

public class TestBeanDeserializer extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    static class Bean {
        public String b = "b";
        public String a = "a";

        public Bean() { }
        public Bean(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    static class ModuleImpl extends SimpleModule
    {
        protected BeanDeserializerModifier modifier;
        
        public ModuleImpl(BeanDeserializerModifier modifier)
        {
            super("test", Version.unknownVersion());
            this.modifier = modifier;
        }
        
        @Override
        public void setupModule(SetupContext context)
        {
            super.setupModule(context);
            if (modifier != null) {
                context.addBeanDeserializerModifier(modifier);
            }
        }
    }

    static class RemovingModifier extends BeanDeserializerModifier
    {
        private final String _removedProperty;
        
        public RemovingModifier(String remove) { _removedProperty = remove; }
        
        @Override
        public BeanDeserializerBuilder updateBuilder(DeserializationConfig config,
                BasicBeanDescription beanDesc, BeanDeserializerBuilder builder) {
            builder.addIgnorable(_removedProperty);
            return builder;
        }
    }
    
    static class ReplacingModifier extends BeanDeserializerModifier
    {
        private final JsonDeserializer<?> _deserializer;
        
        public ReplacingModifier(JsonDeserializer<?> s) { _deserializer = s; }
        
        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BasicBeanDescription beanDesc,
                JsonDeserializer<?> deserializer) {
            return _deserializer;
        }
    }

    static class BogusBeanDeserializer extends JsonDeserializer<Object>
    {
        private final String a, b;
        
        public BogusBeanDeserializer(String a, String b) {
            this.a = a;
            this.b = b;
        }
        
        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            return new Bean(a, b);
        }
    }
    
    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    public void testPropertyRemoval() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ModuleImpl(new RemovingModifier("a")));
        Bean bean = mapper.readValue("{\"b\":\"2\"}", Bean.class);
        assertEquals("2", bean.b);
        // and 'a' has its default value:
        assertEquals("a", bean.a);
    } 

    public void testDeserializerReplacement() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ModuleImpl(new ReplacingModifier(new BogusBeanDeserializer("foo", "bar"))));
        Bean bean = mapper.readValue("{\"a\":\"xyz\"}", Bean.class);
        // custom deserializer always produces instance like this:
        assertEquals("foo", bean.a);
        assertEquals("bar", bean.b);
    }
    
}
