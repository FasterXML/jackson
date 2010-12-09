package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Testing to verify that {@link JsonTypeInfo} works
 * for properties as well as types (see [JACKSON-280] for details)
 * 
 * @since 1.7
 */
@SuppressWarnings("serial")
public class TestPropertyTypeInfo extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    static class FieldWrapperBean
    {
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_ARRAY)
        public Object value;

        public FieldWrapperBean() { }
        public FieldWrapperBean(Object o) { value = o; }
    }

    static class FieldWrapperBeanList extends ArrayList<FieldWrapperBean> { }
    static class FieldWrapperBeanMap extends HashMap<String,FieldWrapperBean> { }
    
    static class MethodWrapperBean
    {
        private Object value;
        
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_ARRAY)
        public Object getValue() { return value; }

        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_ARRAY)
        public void setValue(Object v) { value = v; }
        
        public MethodWrapperBean() { }
        public MethodWrapperBean(Object o) { value = o; }
    }
    
    static class MethodWrapperBeanList extends ArrayList<MethodWrapperBean> { }
    static class MethodWrapperBeanMap extends HashMap<String,MethodWrapperBean> { }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimpleField() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new FieldWrapperBean(new StringWrapper("foo")));
//        System.out.println("JSON == "+json);
        FieldWrapperBean bean = mapper.readValue(json, FieldWrapperBean.class);
        assertNotNull(bean.value);
        assertEquals(StringWrapper.class, bean.value.getClass());
        assertEquals(((StringWrapper) bean.value).str, "foo");
    }

    public void testSimpleMethod() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new FieldWrapperBean(new IntWrapper(37)));
//        System.out.println("JSON == "+json);
        FieldWrapperBean bean = mapper.readValue(json, FieldWrapperBean.class);
        assertNotNull(bean.value);
        assertEquals(IntWrapper.class, bean.value.getClass());
        assertEquals(((IntWrapper) bean.value).i, 37);
    }

    public void testSimpleListField() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FieldWrapperBeanList list = new FieldWrapperBeanList();
        list.add(new FieldWrapperBean(new StringWrapper("x")));
        String json = mapper.writeValueAsString(list);
//        System.out.println("JSON == "+json);
        FieldWrapperBeanList result = mapper.readValue(json, FieldWrapperBeanList.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        FieldWrapperBean bean = list.get(0);
        assertEquals(StringWrapper.class, bean.value.getClass());
        assertEquals(((StringWrapper) bean.value).str, "x");
    }

    /*
    public void testSimpleListMethod() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        MethodWrapperBeanList list = new MethodWrapperBeanList();
        list.add(new MethodWrapperBean(new StringWrapper("y")));
        String json = mapper.writeValueAsString(list);
//        System.out.println("JSON == "+json);
        MethodWrapperBeanList result = mapper.readValue(json, MethodWrapperBeanList.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        MethodWrapperBean bean = list.get(0);
        assertEquals(StringWrapper.class, bean.value.getClass());
        assertEquals(((StringWrapper) bean.value).str, "y");
    }
    */
    
    public void testSimpleMapField() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FieldWrapperBeanMap map = new FieldWrapperBeanMap();
        map.put("xyz", new FieldWrapperBean(new BooleanWrapper(true)));
        String json = mapper.writeValueAsString(map);
        System.out.println("JSON == "+json);
        FieldWrapperBeanMap result = mapper.readValue(json, FieldWrapperBeanMap.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        FieldWrapperBean bean = result.get("xyz");
        assertNotNull(bean);
        Object ob = bean.value;
        assertEquals(BooleanWrapper.class, ob);
        assertTrue(((BooleanWrapper) ob).b.booleanValue());
    }

    public void testSimpleMapMethod() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        MethodWrapperBeanMap map = new MethodWrapperBeanMap();
        map.put("xyz", new MethodWrapperBean(new BooleanWrapper(true)));
        String json = mapper.writeValueAsString(map);
        System.out.println("JSON == "+json);
        MethodWrapperBeanMap result = mapper.readValue(json, MethodWrapperBeanMap.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        MethodWrapperBean bean = result.get("xyz");
        assertNotNull(bean);
        Object ob = bean.value;
        assertEquals(BooleanWrapper.class, ob);
        assertTrue(((BooleanWrapper) ob).b.booleanValue());
    }
    
}
