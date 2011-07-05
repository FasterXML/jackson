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

    static class MysteryBean
    {
        Object value;
        
        public MysteryBean(Object v) { value = v; }
    }
    
    static class CreatorBean
    {
        String _secret;

        public String value;
        
        protected CreatorBean(String s) {
            _secret = s;
        }
    }

    static abstract class InstantiatorBase extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() {
            return "UNKNOWN";
        }
    }
    
    static abstract class PolymorphicBeanBase { }
    
    static class PolymorphicBean extends PolymorphicBeanBase
    {
        public String name;
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
        public MyMap(String name) {
            super();
            put(name, name);
        }
    }
    
    static class MyBeanInstantiator extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() {
            return MyBean.class.getName();
        }
        
        @Override
        public boolean canCreateFromObjectUsingDefault() { return true; }

        @Override
        public MyBean createFromObject() {
            return new MyBean("secret!", true);
        }
    }

    static class CreatorBeanInstantiator extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() {
            return CreatorBean.class.getName();
        }
        
        @Override
        public boolean canCreateFromObjectWithArgs() { return true; }

        @Override
        public CreatorProperty[] getFromObjectArguments() {
            return  new CreatorProperty[] {
                    new CreatorProperty("secret", TypeFactory.defaultInstance().constructType(String.class),
                            null, null, null, 0)
            };
        }

        @Override
        public Object createFromObjectWith(Object[] args) {
            return new CreatorBean((String) args[0]);
        }
    }

    /**
     * Something more ambitious: semi-automated approach to polymorphic
     * deserialization, using ValueInstantiator; from Object to any
     * type...
     */
    static class PolymorphicBeanInstantiator extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() {
            return Object.class.getName();
        }
        
        @Override
        public boolean canCreateFromObjectWithArgs() { return true; }

        @Override
        public CreatorProperty[] getFromObjectArguments() {
            return  new CreatorProperty[] {
                    new CreatorProperty("type", TypeFactory.defaultInstance().constructType(Class.class),
                            null, null, null, 0)
            };
        }

        @Override
        public Object createFromObjectWith(Object[] args) {
            try {
                Class<?> cls = (Class<?>) args[0];
                return cls.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    static class CreatorMapInstantiator extends ValueInstantiator
    {
        @Override
        public String getValueTypeDesc() {
            return MyMap.class.getName();
        }
        
        @Override
        public boolean canCreateFromObjectWithArgs() { return true; }

        @Override
        public CreatorProperty[] getFromObjectArguments() {
            return  new CreatorProperty[] {
                    new CreatorProperty("name", TypeFactory.defaultInstance().constructType(String.class),
                            null, null, null, 0)
            };
        }

        @Override
        public Object createFromObjectWith(Object[] args) {
            return new MyMap((String) args[0]);
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
        public Object createUsingDelegate(Object delegate) {
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
        public boolean canCreateFromObjectUsingDefault() { return true; }

        @Override
        public MyList createFromObject() {
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
        public Object createUsingDelegate(Object delegate) {
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
        public boolean canCreateFromObjectUsingDefault() { return true; }

        @Override
        public MyMap createFromObject() {
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
        public Object createUsingDelegate(Object delegate) {
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
    /* Unit tests for default creators
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

    public void testCustomListInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyList.class, new MyListInstantiator()));
        MyList result = mapper.readValue("[]", MyList.class);
        assertNotNull(result);
        assertEquals(MyList.class, result.getClass());
        assertEquals(0, result.size());
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
    
    /*
    /**********************************************************
    /* Unit tests for delegate creators
    /**********************************************************
     */

    public void testDelegateBeanInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyBean.class, new MyDelegateBeanInstantiator()));
        MyBean bean = mapper.readValue("123", MyBean.class);
        assertNotNull(bean);
        assertEquals("123", bean._secret);
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
    
    public void testDelegateMapInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyMap.class, new MyDelegateMapInstantiator()));
        MyMap result = mapper.readValue("123", MyMap.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(123), result.values().iterator().next());
    }

    /*
    /**********************************************************
    /* Unit tests for property-based creators
    /**********************************************************
     */

    public void testPropertyBasedBeanInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(CreatorBean.class, new CreatorBeanInstantiator()));
        CreatorBean bean = mapper.readValue("{\"secret\":123,\"value\":37}", CreatorBean.class);
        assertNotNull(bean);
        assertEquals("123", bean._secret);
    }

    public void testPropertyBasedMapInstantiator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MyMap.class, new CreatorMapInstantiator()));
        MyMap result = mapper.readValue("{\"name\":\"bob\", \"x\":\"y\"}", MyMap.class);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("bob", result.get("bob"));
        assertEquals("y", result.get("x"));
    }

    /*
    /**********************************************************
    /* Unit tests for scalar-delegates
    /**********************************************************
     */

    public void testBeanFromString() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(MysteryBean.class,
                new InstantiatorBase() {
                    @Override
                    public boolean canCreateFromString() { return true; }
                    
                    @Override
                    public Object createFromString(String value) {
                        return new MysteryBean(value);
                    }
        }));
        MysteryBean result = mapper.readValue(quote("abc"), MysteryBean.class);
        assertNotNull(result);
        assertEquals("abc", result.value);
    }
    
    /*
    /**********************************************************
    /* Other tests
    /**********************************************************
     */

    
    /**
     * Beyond basic features, it should be possible to even implement
     * polymorphic handling...
     */
    public void testPolymorphicCreatorBean() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule(PolymorphicBeanBase.class, new PolymorphicBeanInstantiator()));
        String JSON = "{\"type\":"+quote(PolymorphicBean.class.getName())+",\"name\":\"Axel\"}";
        PolymorphicBeanBase result = mapper.readValue(JSON, PolymorphicBeanBase.class);
        assertNotNull(result);
        assertSame(PolymorphicBean.class, result.getClass());
        assertEquals("Axel", ((PolymorphicBean) result).name);
    }
}
