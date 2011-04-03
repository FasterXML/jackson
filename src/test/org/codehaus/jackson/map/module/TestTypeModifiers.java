package org.codehaus.jackson.map.module;

import java.lang.reflect.Type;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.type.TypeModifier;
import org.codehaus.jackson.type.JavaType;

public class TestTypeModifiers extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class MyMapLikeType {
        public String key, value;

        public MyMapLikeType() { }
        public MyMapLikeType(String k, String v) {
            key = k;
            value = v;
        }
    }

    static class MyCollectionLikeType {
        public Integer value;

        public MyCollectionLikeType() { }
        public MyCollectionLikeType(Integer v) {
            value = v;
        }
    }
    
    static class MyTypeModifier extends TypeModifier
    {
        @Override
        public JavaType modifyType(JavaType type, Type jdkType, TypeBindings context, TypeFactory typeFactory)
        {
            Class<?> raw = type.getRawClass();
            if (raw == MyMapLikeType.class) {
                JavaType stringType = typeFactory.uncheckedSimpleType(String.class);
                return typeFactory.constructMapLikeType(raw, stringType, stringType);
            }
            if (raw == MyCollectionLikeType.class) {
                JavaType valueType = typeFactory.uncheckedSimpleType(Integer.class);
                return typeFactory.constructCollectionLikeType(raw, valueType);
            }
            return type;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * Basic test for ensuring that we can get "xxx-like" types recognized.
     */
    public void testLikeTypes() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTypeFactory(mapper.getTypeFactory().withModifier(new MyTypeModifier()));
        JavaType type = mapper.constructType(MyMapLikeType.class);
        assertTrue(type.isMapLikeType());
        type = mapper.constructType(MyCollectionLikeType.class);
        assertTrue(type.isCollectionLikeType());
    }
}
