package org.codehaus.jackson.map.type;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import main.BaseTest;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

/**
 * Simple tests to verify that the {@link TypeFactory} constructs
 * type information as expected.
 */
public class TestTypeFactory
    extends BaseTest
{    
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    enum EnumForCanonical { YES, NO; }

    static class SingleArgGeneric<X> { }

    abstract static class MyMap extends IntermediateMap<String,Long> { }
    abstract static class IntermediateMap<K,V> implements Map<K,V> { }

    abstract static class MyList extends IntermediateList<Long> { }
    abstract static class IntermediateList<E> implements List<E> { }

    @SuppressWarnings("serial")
    static class GenericList<T> extends ArrayList<T> { }
    
    interface MapInterface extends Cloneable, IntermediateInterfaceMap<String> { }
    interface IntermediateInterfaceMap<FOO> extends Map<FOO, Integer> { }

    @SuppressWarnings("serial")
    static class MyStringIntMap extends MyStringXMap<Integer> { }
    @SuppressWarnings("serial")
    static class MyStringXMap<V> extends HashMap<String,V> { }

    // And one more, now with obfuscated type names; essentially it's just Map<Int,Long>
    static abstract class IntLongMap extends XLongMap<Integer> { }
    // trick here is that V now refers to key type, not value type
    static abstract class XLongMap<V> extends XXMap<V,Long> { }
    static abstract class XXMap<K,V> implements Map<K,V> { }

    static class SneakyBean {
        public IntLongMap intMap;
        public MyList longList;
    }

    static class SneakyBean2 {
        // self-reference; should be resolved as "Comparable<Object>"
        public <T extends Comparable<T>> T getFoobar() { return null; }
    }
    
    @SuppressWarnings("serial")
    public static class LongValuedMap<K> extends HashMap<K, Long> { }

    static class StringLongMapBean {
        public LongValuedMap<String> value;
    }

    static class StringListBean {
        public GenericList<String> value;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
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

        TypeFactory tf = TypeFactory.defaultInstance();
        for (Class<?> clz : classes) {
            assertSame(clz, tf.constructType(clz).getRawClass());
            assertSame(clz, tf.constructType(clz).getRawClass());
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

        TypeFactory tf = TypeFactory.defaultInstance();
        for (Class<?> clz : classes) {
            assertSame(clz, tf.constructType(clz).getRawClass());
            Class<?> elemType = clz.getComponentType();
            assertSame(clz, tf.constructArrayType(elemType).getRawClass());
        }
    }

    public void testCollections()
    {
        // Ok, first: let's test what happens when we pass 'raw' Collection:
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType t = tf.constructType(ArrayList.class);
        assertEquals(CollectionType.class, t.getClass());
        assertSame(ArrayList.class, t.getRawClass());

        // And then the proper way
        t = tf.constructType(new TypeReference<ArrayList<String>>() { });
        assertEquals(CollectionType.class, t.getClass());
        assertSame(ArrayList.class, t.getRawClass());

        JavaType elemType = ((CollectionType) t).getContentType();
        assertNotNull(elemType);
        assertSame(SimpleType.class, elemType.getClass());
        assertSame(String.class, elemType.getRawClass());

        // And alternate method too
        t = tf.constructCollectionType(ArrayList.class, String.class);
        assertEquals(CollectionType.class, t.getClass());
        assertSame(String.class, ((CollectionType) t).getContentType().getRawClass());
    }
    
    public void testMaps()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        // Ok, first: let's test what happens when we pass 'raw' Map:
        JavaType t = tf.constructType(HashMap.class);
        assertEquals(MapType.class, t.getClass());
        assertSame(HashMap.class, t.getRawClass());

        // Then explicit construction
        t = tf.constructMapType(TreeMap.class, String.class, Integer.class);
        assertEquals(MapType.class, t.getClass());
        assertSame(String.class, ((MapType) t).getKeyType().getRawClass());
        assertSame(Integer.class, ((MapType) t).getContentType().getRawClass());

        // And then with TypeReference
        t = tf.constructType(new TypeReference<HashMap<String,Integer>>() { });
        assertEquals(MapType.class, t.getClass());
        assertSame(HashMap.class, t.getRawClass());
        MapType mt = (MapType) t;
        assertEquals(tf.constructType(String.class), mt.getKeyType());
        assertEquals(tf.constructType(Integer.class), mt.getContentType());

        t = tf.constructType(new TypeReference<LongValuedMap<Boolean>>() { });
        assertEquals(MapType.class, t.getClass());
        assertSame(LongValuedMap.class, t.getRawClass());
        mt = (MapType) t;
        assertEquals(tf.constructType(Boolean.class), mt.getKeyType());
        assertEquals(tf.constructType(Long.class), mt.getContentType());
    }

    public void testIterator()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType t = tf.constructType(new TypeReference<Iterator<String>>() { });
        assertEquals(SimpleType.class, t.getClass());
        assertSame(Iterator.class, t.getRawClass());
        assertEquals(1, t.containedTypeCount());
        assertEquals(tf.constructType(String.class), t.containedType(0));
        assertNull(t.containedType(1));
    }

    /**
     * Test for verifying that parametric types can be constructed
     * programmatically
     * 
     * @since 1.5
     */
    public void testParametricTypes()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        // first, simple class based
        JavaType t = tf.constructParametricType(ArrayList.class, String.class); // ArrayList<String>
        assertEquals(CollectionType.class, t.getClass());
        JavaType strC = tf.constructType(String.class);
        assertEquals(1, t.containedTypeCount());
        assertEquals(strC, t.containedType(0));
        assertNull(t.containedType(1));

        // Then using JavaType
        JavaType t2 = tf.constructParametricType(Map.class, strC, t); // Map<String,ArrayList<String>>
        // should actually produce a MapType
        assertEquals(MapType.class, t2.getClass());
        assertEquals(2, t2.containedTypeCount());
        assertEquals(strC, t2.containedType(0));
        assertEquals(t, t2.containedType(1));
        assertNull(t2.containedType(2));

        // and then custom generic type as well
        JavaType custom = tf.constructParametricType(SingleArgGeneric.class, String.class);
        assertEquals(SimpleType.class, custom.getClass());
        assertEquals(1, custom.containedTypeCount());
        assertEquals(strC, custom.containedType(0));
        assertNull(custom.containedType(1));
        // should also be able to access variable name:
        assertEquals("X", custom.containedTypeName(0));

        // And finally, ensure that we can't create invalid combinations
        try {
            // Maps must take 2 type parameters, not just one
            tf.constructParametricType(Map.class, strC);
        } catch (IllegalArgumentException e) {
            verifyException(e, "Need exactly 2 parameter types for Map types");
        }

        try {
            // Type only accepts one type param
            tf.constructParametricType(SingleArgGeneric.class, strC, strC);
        } catch (IllegalArgumentException e) {
            verifyException(e, "expected 1 parameters, was given 2");
        }
    }

    /**
     * Test for checking that canonical name handling works ok
     * 
     * @since 1.5
     */
    public void testCanonicalNames()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType t = tf.constructType(java.util.Calendar.class);
        String can = t.toCanonical();
        assertEquals("java.util.Calendar", can);
        assertEquals(t, TypeFactory.fromCanonical(can));

        // Generic maps and collections will default to Object.class if type-erased
        t = tf.constructType(java.util.ArrayList.class);
        can = t.toCanonical();
        assertEquals("java.util.ArrayList<java.lang.Object>", can);
        assertEquals(t, TypeFactory.fromCanonical(can));

        t = tf.constructType(java.util.TreeMap.class);
        can = t.toCanonical();
        assertEquals("java.util.TreeMap<java.lang.Object,java.lang.Object>", can);
        assertEquals(t, TypeFactory.fromCanonical(can));

        // And then EnumMap (actual use case for us)
        t = tf.constructMapType(EnumMap.class, EnumForCanonical.class, String.class);
        can = t.toCanonical();
        assertEquals("java.util.EnumMap<org.codehaus.jackson.map.type.TestTypeFactory$EnumForCanonical,java.lang.String>",
                can);
        assertEquals(t, TypeFactory.fromCanonical(can));
        
    }

    /*
    /**********************************************************
    /* Unit tests: low-level inheritance resolution
    /**********************************************************
     */
    
    /**
     * @since 1.6
     */
    public void testSuperTypeDetectionClass()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        HierarchicType sub = tf._findSuperTypeChain(MyStringIntMap.class, HashMap.class);
        assertNotNull(sub);
        assertEquals(2, _countSupers(sub));
        assertSame(MyStringIntMap.class, sub.getRawClass());
        HierarchicType sup = sub.getSuperType();
        assertSame(MyStringXMap.class, sup.getRawClass());
        HierarchicType sup2 = sup.getSuperType();
        assertSame(HashMap.class, sup2.getRawClass());
        assertNull(sup2.getSuperType());
    }
    
    /**
     * @since 1.6
     */
    public void testSuperTypeDetectionInterface()
    {
        // List first
        TypeFactory tf = TypeFactory.defaultInstance();
        HierarchicType sub = tf._findSuperTypeChain(MyList.class, List.class);
        assertNotNull(sub);
        assertEquals(2, _countSupers(sub));
        assertSame(MyList.class, sub.getRawClass());
        HierarchicType sup = sub.getSuperType();
        assertSame(IntermediateList.class, sup.getRawClass());
        HierarchicType sup2 = sup.getSuperType();
        assertSame(List.class, sup2.getRawClass());
        assertNull(sup2.getSuperType());
        
        // Then Map
        sub = tf._findSuperTypeChain(MyMap.class, Map.class);
        assertNotNull(sub);
        assertEquals(2, _countSupers(sub));
        assertSame(MyMap.class, sub.getRawClass());
        sup = sub.getSuperType();
        assertSame(IntermediateMap.class, sup.getRawClass());
        sup2 = sup.getSuperType();
        assertSame(Map.class, sup2.getRawClass());
        assertNull(sup2.getSuperType());
    }

    /**
     * @since 1.6
     */
    public void testAtomicArrayRefParameterDetection()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(new TypeReference<AtomicReference<long[]>>() { });
        HierarchicType sub = tf._findSuperTypeChain(type.getRawClass(), AtomicReference.class);
        assertNotNull(sub);
        assertEquals(0, _countSupers(sub));
        assertTrue(AtomicReference.class.isAssignableFrom(type.getRawClass()));
        assertNull(sub.getSuperType());
    }

    private int _countSupers(HierarchicType t)
    {
        int depth = 0;
        for (HierarchicType sup = t.getSuperType(); sup != null; sup = sup.getSuperType()) {
            ++depth;
        }
        return depth;
    }
    
    /*
    /**********************************************************
    /* Unit tests: map/collection type parameter resolution
    /**********************************************************
     */
    
    /**
     * @since 1.6
     */
    public void testMapTypesSimple()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(new TypeReference<Map<String,Boolean>>() { });
        MapType mapType = (MapType) type;
        assertEquals(tf.constructType(String.class), mapType.getKeyType());
        assertEquals(tf.constructType(Boolean.class), mapType.getContentType());
    }

    /**
     * @since 1.6
     */
    public void testMapTypesRaw()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(HashMap.class);
        MapType mapType = (MapType) type;
        assertEquals(tf.constructType(Object.class), mapType.getKeyType());
        assertEquals(tf.constructType(Object.class), mapType.getContentType());        
    }

    /**
     * @since 1.6
     */
    public void testMapTypesAdvanced()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(MyMap.class);
        MapType mapType = (MapType) type;
        assertEquals(tf.constructType(String.class), mapType.getKeyType());
        assertEquals(tf.constructType(Long.class), mapType.getContentType());

        type = tf.constructType(MapInterface.class);
        mapType = (MapType) type;
        assertEquals(tf.constructType(String.class), mapType.getKeyType());
        assertEquals(tf.constructType(Integer.class), mapType.getContentType());

        type = tf.constructType(MyStringIntMap.class);
        mapType = (MapType) type;
        assertEquals(tf.constructType(String.class), mapType.getKeyType());
        assertEquals(tf.constructType(Integer.class), mapType.getContentType());
    }

    /**
     * Specific test to verify that complicate name mangling schemes
     * do not fool type resolver
     * 
     * @since 1.6
     */
    public void testMapTypesSneaky()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(IntLongMap.class);
        MapType mapType = (MapType) type;
        assertEquals(tf.constructType(Integer.class), mapType.getKeyType());
        assertEquals(tf.constructType(Long.class), mapType.getContentType());
    }    
    
    /**
     * Plus sneaky types may be found via introspection as well.
     * 
     * @since 1.7
     */
    public void testSneakyFieldTypes() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        Field field = SneakyBean.class.getDeclaredField("intMap");
        JavaType type = tf.constructType(field.getGenericType());
        assertTrue(type instanceof MapType);
        MapType mapType = (MapType) type;
        assertEquals(tf.constructType(Integer.class), mapType.getKeyType());
        assertEquals(tf.constructType(Long.class), mapType.getContentType());

        field = SneakyBean.class.getDeclaredField("longList");
        type = tf.constructType(field.getGenericType());
        assertTrue(type instanceof CollectionType);
        CollectionType collectionType = (CollectionType) type;
        assertEquals(tf.constructType(Long.class), collectionType.getContentType());
    }    
    
    /**
     * Looks like type handling actually differs for properties, too.
     * 
     * @since 1.7
     */
    public void testSneakyBeanProperties() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringLongMapBean bean = mapper.readValue("{\"value\":{\"a\":123}}", StringLongMapBean.class);
        assertNotNull(bean);
        Map<String,Long> map = bean.value;
        assertEquals(1, map.size());
        assertEquals(Long.valueOf(123), map.get("a"));

        StringListBean bean2 = mapper.readValue("{\"value\":[\"...\"]}", StringListBean.class);
        assertNotNull(bean2);
        List<String> list = bean2.value;
        assertSame(GenericList.class, list.getClass());
        assertEquals(1, list.size());
        assertEquals("...", list.get(0));
    }
    
    public void testAtomicArrayRefParameters()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructType(new TypeReference<AtomicReference<long[]>>() { });
        JavaType[] params = tf.findTypeParameters(type, AtomicReference.class);
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(tf.constructType(long[].class), params[0]);
    }

    public void testSneakySelfRefs() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new SneakyBean2());
        assertEquals("{\"foobar\":null}", json);
    }

    /*
    /**********************************************************
    /* Unit tests: construction of "raw" types
    /**********************************************************
     */

    public void testRawCollections()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructRawCollectionType(ArrayList.class);
        assertTrue(type.isContainerType());
        assertEquals(TypeFactory.unknownType(), type.getContentType());

        type = tf.constructRawCollectionLikeType(String.class); // class doesn't really matter
        assertTrue(type.isCollectionLikeType());
        assertEquals(TypeFactory.unknownType(), type.getContentType());
    }

    public void testRawMaps()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType type = tf.constructRawMapType(HashMap.class);
        assertTrue(type.isContainerType());
        assertEquals(TypeFactory.unknownType(), type.getKeyType());
        assertEquals(TypeFactory.unknownType(), type.getContentType());

        type = tf.constructRawMapLikeType(String.class); // class doesn't really matter
        assertTrue(type.isMapLikeType());
        assertEquals(TypeFactory.unknownType(), type.getKeyType());
        assertEquals(TypeFactory.unknownType(), type.getContentType());
    }
}

