package org.codehaus.jackson.type;

import java.util.*;

import main.BaseTest;

import org.codehaus.jackson.map.type.*;

/**
 * Simple tests to verify that the {@link TypeFactory} constructs
 * type information as expected.
 */
public class TestTypeFactory
    extends BaseTest
{
    public void testSimpleTypes()
    {
        TypeFactory tf = TypeFactory.instance;

        Class<?>[] classes = new Class<?>[] {
            boolean.class, byte.class, char.class,
                short.class, int.class, long.class,
                float.class, double.class,

            Boolean.class, Byte.class, Character.class,
                Short.class, Integer.class, Long.class,
                Float.class, Double.class,

                String.class,
                Object.class,

                Calendar.class,
                Date.class,
        };

        for (Class<?> clz : classes) {
            assertSame(clz, tf.fromClass(clz).getRawClass());
            assertSame(clz, tf.fromType(clz).getRawClass());
        }
    }

    public void testSimpleTypeRefs()
    {
        TypeFactory tf = TypeFactory.instance;

        // And then via type reference...
        assertSame(tf.fromClass(Integer.class), tf.fromTypeReference(new TypeReference<Integer>() { }));
        assertSame(tf.fromClass(String.class), tf.fromTypeReference(new TypeReference<String>() { }));
    }

    public void testArrays()
    {
        TypeFactory tf = TypeFactory.instance;

        Class<?>[] classes = new Class<?>[] {
            boolean[].class, byte[].class, char[].class,
                short[].class, int[].class, long[].class,
                float[].class, double[].class,

                String[].class, Object[].class,
                Calendar[].class,
        };

        for (Class<?> clz : classes) {
            assertSame(clz, tf.fromClass(clz).getRawClass());
            assertSame(clz, tf.fromType(clz).getRawClass());
        }
    }

    public void testCollections()
    {
        TypeFactory tf = TypeFactory.instance;

        // Ok, first: let's test what happens when we pass 'raw' Collection:
        JavaType t = tf.fromClass(ArrayList.class);
        assertEquals(CollectionType.class, t.getClass());
        assertSame(ArrayList.class, t.getRawClass());
        assertFalse(t.isFullyTyped());

        // And then the proper way
        t = tf.fromTypeReference(new TypeReference<ArrayList<String>>() { });
        assertEquals(CollectionType.class, t.getClass());
        assertSame(ArrayList.class, t.getRawClass());
        assertTrue(t.isFullyTyped());
        JavaType elemType = ((CollectionType) t).getElementType();
        assertNotNull(elemType);
        assertSame(SimpleType.class, elemType.getClass());
        assertSame(String.class, elemType.getRawClass());
    }

    public void testMaps()
    {
        TypeFactory tf = TypeFactory.instance;

        // Ok, first: let's test what happens when we pass 'raw' Map:
        JavaType t = tf.fromClass(HashMap.class);
        assertEquals(MapType.class, t.getClass());
        assertSame(HashMap.class, t.getRawClass());
        assertFalse(t.isFullyTyped());

        // And then the proper way
        t = tf.fromTypeReference(new TypeReference<HashMap<String,Integer>>() { });
        assertEquals(MapType.class, t.getClass());
        assertSame(HashMap.class, t.getRawClass());
        assertTrue(t.isFullyTyped());
        MapType mt = (MapType) t;
        assertEquals(tf.fromClass(String.class), mt.getKeyType());
        assertEquals(tf.fromClass(Integer.class), mt.getValueType());
    }
}
