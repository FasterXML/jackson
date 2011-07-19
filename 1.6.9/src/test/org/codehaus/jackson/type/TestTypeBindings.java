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
        TypeBindings b = new TypeBindings(AbstractType.class);
        assertEquals(2, b.getBindingCount());
        JavaType obType = TypeFactory.type(Object.class);
        assertEquals(obType, b.findType("A"));
        assertEquals(obType, b.findType("B"));
    }

    public void testSimple() throws Exception
    {
        // concrete class does have bindings however
        TypeBindings b = new TypeBindings(LongStringType.class);
        assertEquals(2, b.getBindingCount());
        assertEquals(TypeFactory.type(Long.class), b.findType("A"));
        assertEquals(TypeFactory.type(String.class), b.findType("B"));
    }
}
