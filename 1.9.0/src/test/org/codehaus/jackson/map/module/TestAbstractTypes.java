package org.codehaus.jackson.map.module;

import java.util.*;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

public class TestAbstractTypes extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes; simple beans and their handlers
    /**********************************************************
     */

    static class MyString implements CharSequence
    {
        protected String value;
        
        public MyString(String s) { value = s; }

        @Override
        public char charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public CharSequence subSequence(int arg0, int arg1) { return this; }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testCollectionDefaulting() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        // let's ensure we get hierarchic mapping
        mod.addAbstractTypeMapping(Collection.class, List.class);
        mod.addAbstractTypeMapping(List.class, LinkedList.class);
        mapper.registerModule(mod);
        Collection<?> result = mapper.readValue("[]", Collection.class);
        assertEquals(LinkedList.class, result.getClass());
    }

    public void testMapDefaulting() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        // default is HashMap, so:
        mod.addAbstractTypeMapping(Map.class, TreeMap.class);
        mapper.registerModule(mod);
        Map<?,?> result = mapper.readValue("{}", Map.class);
        assertEquals(TreeMap.class, result.getClass());
    }
    
    public void testInterfaceDefaulting() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        // let's ensure we get hierarchic mapping
        mod.addAbstractTypeMapping(CharSequence.class, MyString.class);
        mapper.registerModule(mod);
        Object result = mapper.readValue(quote("abc"), CharSequence.class);
        assertEquals(MyString.class, result.getClass());
        assertEquals("abc", ((MyString) result).value);
    }
}
