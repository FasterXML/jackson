package org.codehaus.jackson.map.type;

import java.util.*;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

import main.BaseTest;

@SuppressWarnings("serial")
public class TestTypeResolution extends BaseTest
{
    public static class LongValuedMap<K> extends HashMap<K, Long> { }

    static class GenericList<X> extends ArrayList<X> { }
    static class GenericList2<Y> extends GenericList<Y> { }

    static class LongList extends GenericList2<Long> { }
    static class MyLongList<T> extends LongList { }
    
    public void testMaps()
    {
        JavaType t = TypeFactory.type(new TypeReference<LongValuedMap<String>>() { });
        MapType type = (MapType) t;
        assertSame(LongValuedMap.class, type.getRawClass());
        assertEquals(TypeFactory.type(String.class), type.getKeyType());
        assertEquals(TypeFactory.type(Long.class), type.getContentType());        
    }

    public void testList()
    {
        JavaType t;

        t = TypeFactory.type(new TypeReference<MyLongList<Integer>>() {});
        CollectionType type = (CollectionType) t;
        assertSame(MyLongList.class, type.getRawClass());
        assertEquals(TypeFactory.type(Long.class), type.getContentType());        

        t = TypeFactory.type(LongList.class);
        type = (CollectionType) t;
        assertSame(LongList.class, type.getRawClass());
        assertEquals(TypeFactory.type(Long.class), type.getContentType());        
    }
}
