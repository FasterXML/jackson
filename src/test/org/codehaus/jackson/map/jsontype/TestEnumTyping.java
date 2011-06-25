package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

@SuppressWarnings("serial")
public class TestEnumTyping extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    // note: As.WRAPPER_ARRAY worked initially; but as per [JACKSON-485], As.PROPERTY had issues
    @JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY)
    public interface EnumInterface { }

    public enum Tag implements EnumInterface
    { A, B };
    
    static class EnumInterfaceWrapper {
        public EnumInterface value;
    }
    
    static class EnumInterfaceList extends ArrayList<EnumInterface> { }

    static class TagList extends ArrayList<Tag> { }

    static enum TestEnum { A, B, C; }
    
    static class UntypedEnumBean
    {
       @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="__type")
        public Object value;

        public UntypedEnumBean() { }
        public UntypedEnumBean(TestEnum v) { value = v; }

        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="__type")
        public void setValue(Object o) {
            value = o;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testTagList() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        TagList list = new TagList();
        list.add(Tag.A);
        list.add(Tag.B);
        String json = m.writeValueAsString(list);

        TagList result = m.readValue(json, TagList.class);
        assertEquals(2, result.size());
        assertSame(Tag.A, result.get(0));
        assertSame(Tag.B, result.get(1));
    }

    public void testEnumInterface() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String json = m.writeValueAsString(Tag.B);
        
        EnumInterface result = m.readValue(json, EnumInterface.class);
        assertSame(Tag.B, result);
    }

    public void testEnumInterfaceList() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        EnumInterfaceList list = new EnumInterfaceList();
        list.add(Tag.A);
        list.add(Tag.B);
        String json = m.writeValueAsString(list);
        
        EnumInterfaceList result = m.readValue(json, EnumInterfaceList.class);
        assertEquals(2, result.size());
        assertSame(Tag.A, result.get(0));
        assertSame(Tag.B, result.get(1));
    }

    public void testUntypedEnum() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String str = mapper.writeValueAsString(new UntypedEnumBean(TestEnum.B));
        UntypedEnumBean result = mapper.readValue(str, UntypedEnumBean.class);
        assertNotNull(result);
        assertNotNull(result.value);
        Object ob = result.value;
        assertSame(TestEnum.class, ob.getClass());
        assertEquals(TestEnum.B, result.value);
    }
    
}
