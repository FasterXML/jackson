package org.codehaus.jackson.type;

import java.util.*;

import main.BaseTest;

import org.codehaus.jackson.map.type.*;

/**
 * Simple tests to verify that {@link JavaType} types work to
 * some degree
 */
public class TestJavaType
    extends BaseTest
{
    static class BaseType { }

    static class SubType extends BaseType { }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    public void testSimpleClass()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType baseType = tf.constructType(BaseType.class);
        assertSame(BaseType.class, baseType.getRawClass());
        assertTrue(baseType.hasRawClass(BaseType.class));

        assertFalse(baseType.isArrayType());
        assertFalse(baseType.isContainerType());
        assertFalse(baseType.isEnumType());
        assertFalse(baseType.isInterface());
        assertFalse(baseType.isPrimitive());

        assertNull(baseType.getContentType());
        assertNull(baseType.getValueHandler());

        /* both narrow and widen just return type itself (exact, not just
         * equal)
         * (also note that widen/narrow wouldn't work on basic simple
         * class type otherwise)
         */
        assertSame(baseType, baseType.narrowBy(BaseType.class));
        assertSame(baseType, baseType.widenBy(BaseType.class));

        // Also: no narrowing for simple types (but should there be?)
        try {
            baseType.narrowBy(SubType.class);
        } catch (IllegalArgumentException e) {
            verifyException(e, "should never be called");
        }

        // Also, let's try assigning bogus handler
        /*
        baseType.setValueHandler("xyz"); // untyped
        assertEquals("xyz", baseType.getValueHandler());
        // illegal to re-set
        try {
            baseType.setValueHandler("foobar");
            fail("Shouldn't allow re-setting value handler");
        } catch (IllegalStateException iae) {
            verifyException(iae, "Trying to reset");
        }
        */
    }

    public void testMapType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType keyT = tf.constructType(String.class);
        JavaType baseT = tf.constructType(BaseType.class);

        MapType mapT = MapType.construct(Map.class, keyT, baseT);
        assertNotNull(mapT);
        assertTrue(mapT.isContainerType());

        // NOPs:
        assertSame(mapT, mapT.narrowContentsBy(BaseType.class));
        assertSame(mapT, mapT.narrowKey(String.class));

        assertTrue(mapT.equals(mapT));
        assertFalse(mapT.equals(null));
        assertFalse(mapT.equals("xyz"));

        MapType mapT2 = MapType.construct(HashMap.class, keyT, baseT);
        assertFalse(mapT.equals(mapT2));

        // Also, must use map type constructor, not simple...
        try {
            SimpleType.construct(HashMap.class);
        } catch (IllegalArgumentException e) {
            verifyException(e, "for a Map");
        }
    }

    public void testArrayType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType arrayT = ArrayType.construct(tf.constructType(String.class), null, null);
        assertNotNull(arrayT);
        assertTrue(arrayT.isContainerType());

        // NOPs:
        assertSame(arrayT, arrayT.narrowContentsBy(String.class));

        assertNotNull(arrayT.toString());

        assertTrue(arrayT.equals(arrayT));
        assertFalse(arrayT.equals(null));
        assertFalse(arrayT.equals("xyz"));

        assertTrue(arrayT.equals(ArrayType.construct(tf.constructType(String.class), null, null)));
        assertFalse(arrayT.equals(ArrayType.construct(tf.constructType(Integer.class), null, null)));

        // Also, must NOT try to create using simple type
        try {
            SimpleType.construct(String[].class);
        } catch (IllegalArgumentException e) {
            verifyException(e, "for an array");
        }
    }

    public void testCollectionType()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        // List<String>
        JavaType collectionT = CollectionType.construct(List.class, tf.constructType(String.class));
        assertNotNull(collectionT);
        assertTrue(collectionT.isContainerType());

        // NOPs:
        assertSame(collectionT, collectionT.narrowContentsBy(String.class));

        assertNotNull(collectionT.toString());

        assertTrue(collectionT.equals(collectionT));
        assertFalse(collectionT.equals(null));
        assertFalse(collectionT.equals("xyz"));

        assertTrue(collectionT.equals(CollectionType.construct(List.class, tf.constructType(String.class))));
        assertFalse(collectionT.equals(CollectionType.construct(Set.class, tf.constructType(String.class))));

        // Also, must NOT try to create using simple type
        try {
            SimpleType.construct(ArrayList.class);
        } catch (IllegalArgumentException e) {
            verifyException(e, "for a Collection");
        }
    }

    public void testClassKey()
    {
        ClassKey key = new ClassKey(String.class);
        assertEquals(0, key.compareTo(key));
        assertTrue(key.equals(key));
        assertFalse(key.equals(null));
        assertFalse(key.equals("foo"));
        assertFalse(key.equals(new ClassKey(Integer.class)));
        assertEquals(String.class.getName(), key.toString());
    }
}

