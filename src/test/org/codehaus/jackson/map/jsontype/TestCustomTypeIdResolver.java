package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonTypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

public class TestCustomTypeIdResolver extends BaseMapTest
{
    @JsonTypeInfo(use=Id.CUSTOM, include=As.WRAPPER_OBJECT)
    @JsonTypeIdResolver(CustomResolver.class)
    static class CustomBean {
        public int x;
        
        public CustomBean() { }
        public CustomBean(int x) { this.x = x; }
    }
    
    static class CustomResolver implements TypeIdResolver
    {
        static List<JavaType> initTypes;

        public CustomResolver() { }
        
        @Override
        public Id getMechanism() {
            return Id.CUSTOM;
        }

        @Override
        public String idFromValue(Object value)
        {
            if (value.getClass() == CustomBean.class) {
                return "*";
            }
            return "unknown";
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> type) {
            return idFromValue(value);
        }
        
        @Override
        public void init(JavaType baseType) {
            if (initTypes != null) {
                initTypes.add(baseType);
            }
        }

        @Override
        public JavaType typeFromId(String id)
        {
            if ("*".equals(id)) {
                return TypeFactory.defaultInstance().constructType(CustomBean.class);
            }
            return null;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // for [JACKSON-359]
    public void testCustomTypeIdResolver() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        List<JavaType> types = new ArrayList<JavaType>();
        CustomResolver.initTypes = types;
        String json = m.writeValueAsString(new CustomBean[] { new CustomBean(28) });
        assertEquals("[{\"*\":{\"x\":28}}]", json);
        assertEquals(1, types.size());
        assertEquals(CustomBean.class, types.get(0).getRawClass());

        types = new ArrayList<JavaType>();
        CustomResolver.initTypes = types;
        CustomBean[] result = m.readValue(json, CustomBean[].class);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(28, result[0].x);
        assertEquals(1, types.size());
        assertEquals(CustomBean.class, types.get(0).getRawClass());
    }
}
