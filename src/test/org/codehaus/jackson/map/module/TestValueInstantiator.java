package org.codehaus.jackson.map.module;

import java.util.*;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Test related to [JACKSON-580] (allow specifying custom instantiators)
 */
public class TestValueInstantiator extends BaseMapTest
{
    static class MyBean
    {
        String _secret;
        
        public MyBean(String s, boolean bogus) {
            _secret = s;
        }
    }
    
    @SuppressWarnings("serial")
    static class MyList extends ArrayList<Object>
    {
        public MyList(boolean b) { super(); }
    }

    @SuppressWarnings("serial")
    static class MyMap extends HashMap<String,Object>
    {
        public MyMap(boolean b) { super(); }
    }
    
    static class MyBeanInstantiator extends ValueInstantiator
    {
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

    static class MyDelegateBeanInstantiator extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() { return "xxx"; }
        
        @Override
        public boolean canCreateUsingDelegate() { return true; }

        @Override
        public JavaType getDelegateType() {
            return TypeFactory.defaultInstance().constructType(Object.class);
        }
        
        @Override
        public Object createInstanceFromObjectUsing(Object delegate) {
            return new MyBean(""+delegate, true);
        }
    }
    
    static class MyListInstantiator extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() {
            return MyList.class.getName();
        }
        
        @Override
        public boolean canCreateUsingDefault() { return true; }

        @Override
        public MyList createInstanceFromObject() {
            return new MyList(true);
        }
    }

    static class MyDelegateListInstantiator extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() { return "xxx"; }
        
        @Override
        public boolean canCreateUsingDelegate() { return true; }

        @Override
        public JavaType getDelegateType() {
            return TypeFactory.defaultInstance().constructType(Object.class);
        }
        
        @Override
        public Object createInstanceFromObjectUsing(Object delegate) {
            MyList list = new MyList(true);
            list.add(delegate);
            return list;
        }
    }
    
    static class MyMapInstantiator extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() {
            return MyMap.class.getName();
        }
        
        @Override
        public boolean canCreateUsingDefault() { return true; }

        @Override
        public MyMap createInstanceFromObject() {
            return new MyMap(true);
        }
    }

    static class MyDelegateMapInstantiator extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() { return "xxx"; }
        
        @Override
        public boolean canCreateUsingDelegate() { return true; }

        @Override
        public JavaType getDelegateType() {
            return TypeFactory.defaultInstance().constructType(Object.class);
        }
        
        @Override
        public Object createInstanceFromObjectUsing(Object delegate) {
            MyMap map = new MyMap(true);
            map.put("value", delegate);
            return map;
        }
    }
    
    static class MyModule extends SimpleModule
    {
        public MyModule(Class<?> cls, ValueInstantiator inst)
        {
            super("Test", Version.unknownVersion());
            this.addValueInstantiator(cls, inst);
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testCustomBeanInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyBean.class, new MyBeanInstantiator()));
        MyBean bean = mapper.readValue("{}", MyBean.class);
        assertNotNull(bean);
        assertEquals("secret!", bean._secret);
    }

    public void testDelegateBeanInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyBean.class, new MyDelegateBeanInstantiator()));
        MyBean bean = mapper.readValue("123", MyBean.class);
        assertNotNull(bean);
        assertEquals("123", bean._secret);
    }
    
    public void testCustomListInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyList.class, new MyListInstantiator()));
        MyList result = mapper.readValue("[]", MyList.class);
        assertNotNull(result);
        assertEquals(MyList.class, result.getClass());
        assertEquals(0, result.size());
    }

    public void testDelegateListInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyList.class, new MyDelegateListInstantiator()));
        MyList result = mapper.readValue("123", MyList.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(123), result.get(0));
    }
    
    public void testCustomMapInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyMap.class, new MyMapInstantiator()));
        MyMap result = mapper.readValue("{ \"a\":\"b\" }", MyMap.class);
        assertNotNull(result);
        assertEquals(MyMap.class, result.getClass());
        assertEquals(1, result.size());
    }

    public void testDelegateMapInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyMap.class, new MyDelegateMapInstantiator()));
        MyMap result = mapper.readValue("123", MyMap.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(123), result.values().iterator().next());
    }
}
