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
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType t = tf.constructType(new TypeReference<LongValuedMap<String>>() { });
        MapType type = (MapType) t;
        assertSame(LongValuedMap.class, type.getRawClass());
        assertEquals(tf.constructType(String.class), type.getKeyType());
        assertEquals(tf.constructType(Long.class), type.getContentType());        
    }

    public void testList()
    {
        JavaType t;

        TypeFactory tf = TypeFactory.defaultInstance();
        t = tf.constructType(new TypeReference<MyLongList<Integer>>() {});
        CollectionType type = (CollectionType) t;
        assertSame(MyLongList.class, type.getRawClass());
        assertEquals(tf.constructType(Long.class), type.getContentType());        

        t = tf.constructType(LongList.class);
        type = (CollectionType) t;
        assertSame(LongList.class, type.getRawClass());
        assertEquals(tf.constructType(Long.class), type.getContentType());        
    }
}
