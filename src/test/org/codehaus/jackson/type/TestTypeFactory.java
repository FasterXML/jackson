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
            assertSame(clz, TypeFactory.fromClass(clz).getRawClass());
            assertSame(clz, TypeFactory.fromType(clz).getRawClass());
        }
    }

    public void testArrays()
    {
        Class<?>[] classes = new Class<?>[] {
            boolean[].class, byte[].class, char[].class,
                short[].class, int[].class, long[].class,
                float[].class, double[].class,

                String[].class, Object[].class,
                Calendar[].class,
        };

        for (Class<?> clz : classes) {
            assertSame(clz, TypeFactory.fromClass(clz).getRawClass());
            assertSame(clz, TypeFactory.fromType(clz).getRawClass());
        }
    }

    public void testCollections()
    {
        // Ok, first: let's test what happens when we pass 'raw' Collection:
        JavaType t = TypeFactory.fromClass(ArrayList.class);
        assertEquals(CollectionType.class, t.getClass());
        assertSame(ArrayList.class, t.getRawClass());

        // And then the proper way
        t = TypeFactory.fromTypeReference(new TypeReference<ArrayList<String>>() { });
        assertEquals(CollectionType.class, t.getClass());
        assertSame(ArrayList.class, t.getRawClass());

        JavaType elemType = ((CollectionType) t).getContentType();
        assertNotNull(elemType);
        assertSame(SimpleType.class, elemType.getClass());
        assertSame(String.class, elemType.getRawClass());
    }

    public void testMaps()
    {
        // Ok, first: let's test what happens when we pass 'raw' Map:
        JavaType t = TypeFactory.fromClass(HashMap.class);
        assertEquals(MapType.class, t.getClass());
        assertSame(HashMap.class, t.getRawClass());

        // And then the proper way
        t = TypeFactory.fromTypeReference(new TypeReference<HashMap<String,Integer>>() { });
        assertEquals(MapType.class, t.getClass());
        assertSame(HashMap.class, t.getRawClass());
        MapType mt = (MapType) t;
        assertEquals(TypeFactory.fromClass(String.class), mt.getKeyType());
        assertEquals(TypeFactory.fromClass(Integer.class), mt.getContentType());
    }
}
