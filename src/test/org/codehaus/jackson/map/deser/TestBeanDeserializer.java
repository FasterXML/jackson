package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.BeanDeserializerModifier;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.module.SimpleModule;

public class TestBeanDeserializer extends BaseMapTest
{
    /*
    /********************************************************
    /* Helper types
    /********************************************************
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
        public List<SettableBeanProperty> changeProperties(DeserializationConfig config, BasicBeanDescription beanDesc,
                List<SettableBeanProperty> beanProperties)
        {
            Iterator<SettableBeanProperty> it = beanProperties.iterator();
            while (it.hasNext()) {
                SettableBeanProperty prop = it.next();
                if (prop.getName().equals(_removedProperty)) {
                    it.remove();
                }
            }
            return beanProperties;
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
        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            return new Bean("foo", "bar");
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
        Bean bean = mapper.readValue("{\"a\":\"1\", \"b\":\"2\"}", Bean.class);
        assertEquals("1", bean.a);
        assertEquals("2", bean.b);
    } 

    public void testDeserializerReplacement() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ModuleImpl(new ReplacingModifier(new BogusBeanDeserializer())));
        Bean bean = mapper.readValue("{\"a\":\"xyz\"}", Bean.class);
        // custom deserializer always produces instance like this:
        assertEquals("foo", bean.a);
        assertEquals("bar", bean.b);
    }
    
}
