package org.codehaus.jackson.type;

import main.BaseTest;

import org.codehaus.jackson.map.type.*;

/**
 * Simple tests to verify for generic type binding functionality
 * implemented by {@link TypeBindings} class.
 *
 * @since 1.5
 */
public class TestTypeBindings
    extends BaseTest
{    
    static class AbstractType<A,B> {
    }
    
    static class LongStringType extends AbstractType<Long,String> { }

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
}
