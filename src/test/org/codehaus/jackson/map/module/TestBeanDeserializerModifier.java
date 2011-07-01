package org.codehaus.jackson.map.module;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.BeanDeserializerModifier;
import org.codehaus.jackson.map.deser.ValueInstantiator;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;

public class TestBeanDeserializerModifier extends BaseMapTest
{
    static class MyBean
    {
        String _secret;
        
        public MyBean(String s, boolean bogus) {
            _secret = s;
        }
    }
    
    static class MyInstantiator extends ValueInstantiator
    {
        public MyInstantiator() { }

        @Override
        public String getValueTypeDesc() {
            return MyBean.class.getName();
        }
        
        @Override
        public boolean canCreateUsingDefault() { return true; }

        @Override
        public MyBean createInstanceFromObject() {
            return new MyBean("secret!", true);
        }
    }
    
    static class MyBeanDeserializerModifier extends BeanDeserializerModifier
    {
        @Override
        public ValueInstantiator modifyValueInstantiator(DeserializationConfig config,
                BasicBeanDescription beanDesc, ValueInstantiator instantiator) {
            if (beanDesc.getBeanClass() == MyBean.class) {
                return new MyInstantiator();
            }
            return instantiator;
        }
    }

    static class MyModule extends SimpleModule
    {
        public MyModule() {
            super("Test", Version.unknownVersion());
        }

        @Override
        public void setupModule(SetupContext context)
        {
            context.addBeanDeserializerModifier(new MyBeanDeserializerModifier());
        }        
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    // [JACKSON-580] Allow specifying custom instantiators
    public void testCustomInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule());
        MyBean bean = mapper.readValue("{}", MyBean.class);
        assertNotNull(bean);
        assertEquals("secret!", bean._secret);
    }
}
