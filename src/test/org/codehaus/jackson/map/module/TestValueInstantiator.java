package org.codehaus.jackson.map.module;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.BeanDescription;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.*;

public class TestValueInstantiator extends BaseMapTest
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

    static class MyModule extends SimpleModule
    {
        public MyModule() {
            super("Test", Version.unknownVersion());
            this.addValueInstantiator(MyBean.class, new MyInstantiator());
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    // [JACKSON-580] Allow specifying custom instantiators
    public void testCustomValueInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule());
        MyBean bean = mapper.readValue("{}", MyBean.class);
        assertNotNull(bean);
        assertEquals("secret!", bean._secret);
    }
}
