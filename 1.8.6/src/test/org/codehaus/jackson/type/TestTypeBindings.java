package org.codehaus.jackson.type;

import java.util.*;

import org.codehaus.jackson.map.type.*;

/**
 * Simple tests to verify for generic type binding functionality
 * implemented by {@link TypeBindings} class.
 *
 * @since 1.5
 */
public class TestTypeBindings
    extends main.BaseTest
{    
    static class AbstractType<A,B> { }
    
    static class LongStringType extends AbstractType<Long,String> { }

    static class InnerGenericTyping<K, V> extends AbstractMap<K, Collection<V>>
    {
        @Override
        public Set<java.util.Map.Entry<K, Collection<V>>> entrySet() {
            return null;
        }
        public class InnerClass extends AbstractMap<K, Collection<V>> {
            @Override
            public Set<java.util.Map.Entry<K, Collection<V>>> entrySet() {
                return null;
            }
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    public void testAbstract() throws Exception
    {
        /* Abstract type does declare type parameters, but they are only
         * known as 'Object.class' (via lower bound)
         */
        TypeFactory tf = TypeFactory.defaultInstance();
        TypeBindings b = new TypeBindings(tf, AbstractType.class);
        assertEquals(2, b.getBindingCount());
        JavaType obType = tf.constructType(Object.class);
        assertEquals(obType, b.findType("A"));
        assertEquals(obType, b.findType("B"));
    }

    public void testSimple() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        // concrete class does have bindings however
        TypeBindings b = new TypeBindings(tf, LongStringType.class);
        assertEquals(2, b.getBindingCount());
        assertEquals(tf.constructType(Long.class), b.findType("A"));
        assertEquals(tf.constructType(String.class), b.findType("B"));
    }


    // [JACKSON-677]
    public void testInnerType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(InnerGenericTyping.InnerClass.class);
        assertEquals(MapType.class, type.getClass());
        JavaType keyType = type.getKeyType();
        assertEquals(Object.class, keyType.getRawClass());
        JavaType valueType = type.getContentType();
        assertEquals(Collection.class, valueType.getRawClass());
        JavaType vt2 = valueType.getContentType();
        assertEquals(Object.class, vt2.getRawClass());
    }

}
