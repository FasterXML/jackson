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

        for (Class<?> clz : classes) {
            assertSame(clz, TypeFactory.type(clz).getRawClass());
            assertSame(clz, TypeFactory.type(clz).getRawClass());
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
            assertSame(clz, TypeFactory.type(clz).getRawClass());
            Class<?> elemType = clz.getComponentType();
            assertSame(clz, TypeFactory.arrayType(elemType).getRawClass());
        }
    }

    public void testCollections()
    {
        // Ok, first: let's test what happens when we pass 'raw' Collection:
        JavaType t = TypeFactory.type(ArrayList.class);
        assertEquals(CollectionType.class, t.getClass());
        assertSame(ArrayList.class, t.getRawClass());

        // And then the proper way
        t = TypeFactory.type(new TypeReference<ArrayList<String>>() { });
        assertEquals(CollectionType.class, t.getClass());
        assertSame(ArrayList.class, t.getRawClass());

        JavaType elemType = ((CollectionType) t).getContentType();
        assertNotNull(elemType);
        assertSame(SimpleType.class, elemType.getClass());
        assertSame(String.class, elemType.getRawClass());

        // And alternate method too
        t = TypeFactory.collectionType(ArrayList.class, String.class);
        assertEquals(CollectionType.class, t.getClass());
        assertSame(String.class, ((CollectionType) t).getContentType().getRawClass());
    }
    
    public void testMaps()
    {
        // Ok, first: let's test what happens when we pass 'raw' Map:
        JavaType t = TypeFactory.type(HashMap.class);
        assertEquals(MapType.class, t.getClass());
        assertSame(HashMap.class, t.getRawClass());

        // Then explicit construction
        t = TypeFactory.mapType(TreeMap.class, String.class, Integer.class);
        assertEquals(MapType.class, t.getClass());
        assertSame(String.class, ((MapType) t).getKeyType().getRawClass());
        assertSame(Integer.class, ((MapType) t).getContentType().getRawClass());

        // And then with TypeReference
        t = TypeFactory.type(new TypeReference<HashMap<String,Integer>>() { });
        assertEquals(MapType.class, t.getClass());
        assertSame(HashMap.class, t.getRawClass());
        MapType mt = (MapType) t;
        assertEquals(TypeFactory.type(String.class), mt.getKeyType());
        assertEquals(TypeFactory.type(Integer.class), mt.getContentType());

        t = TypeFactory.type(new TypeReference<LongValuedMap<Boolean>>() { });
        assertEquals(MapType.class, t.getClass());
        assertSame(LongValuedMap.class, t.getRawClass());
        mt = (MapType) t;
        assertEquals(TypeFactory.type(Boolean.class), mt.getKeyType());
        assertEquals(TypeFactory.type(Long.class), mt.getContentType());
    }

    public void testIterator()
    {
        JavaType t = TypeFactory.type(new TypeReference<Iterator<String>>() { });
        assertEquals(SimpleType.class, t.getClass());
        assertSame(Iterator.class, t.getRawClass());
        assertEquals(1, t.containedTypeCount());
        assertEquals(TypeFactory.type(String.class), t.containedType(0));
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
        // first, simple class based
        JavaType t = TypeFactory.parametricType(ArrayList.class, String.class); // ArrayList<String>
        assertEquals(CollectionType.class, t.getClass());
        JavaType strC = TypeFactory.type(String.class);
        assertEquals(1, t.containedTypeCount());
        assertEquals(strC, t.containedType(0));
        assertNull(t.containedType(1));

        // Then using JavaType
        JavaType t2 = TypeFactory.parametricType(Map.class, strC, t); // Map<String,ArrayList<String>>
        // should actually produce a MapType
        assertEquals(MapType.class, t2.getClass());
        assertEquals(2, t2.containedTypeCount());
        assertEquals(strC, t2.containedType(0));
        assertEquals(t, t2.containedType(1));
        assertNull(t2.containedType(2));

        // and then custom generic type as well
        JavaType custom = TypeFactory.parametricType(SingleArgGeneric.class, String.class);
        assertEquals(SimpleType.class, custom.getClass());
        assertEquals(1, custom.containedTypeCount());
        assertEquals(strC, custom.containedType(0));
        assertNull(custom.containedType(1));
        // should also be able to access variable name:
        assertEquals("X", custom.containedTypeName(0));

        // And finally, ensure that we can't create invalid combinations
        try {
            // Maps must take 2 type parameters, not just one
            TypeFactory.parametricType(Map.class, strC);
        } catch (IllegalArgumentException e) {
            verifyException(e, "Need exactly 2 parameter types for Map types");
        }

        try {
            // Type only accepts one type param
            TypeFactory.parametricType(SingleArgGeneric.class, strC, strC);
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
        JavaType t = TypeFactory.type(java.util.Calendar.class);
        String can = t.toCanonical();
        assertEquals("java.util.Calendar", can);
        assertEquals(t, TypeFactory.fromCanonical(can));

        // Generic maps and collections will default to Object.class if type-erased
        t = TypeFactory.type(java.util.ArrayList.class);
        can = t.toCanonical();
        assertEquals("java.util.ArrayList<java.lang.Object>", can);
        assertEquals(t, TypeFactory.fromCanonical(can));

        t = TypeFactory.type(java.util.TreeMap.class);
        can = t.toCanonical();
        assertEquals("java.util.TreeMap<java.lang.Object,java.lang.Object>", can);
        assertEquals(t, TypeFactory.fromCanonical(can));

        // And then EnumMap (actual use case for us)
        t = TypeFactory.mapType(EnumMap.class, EnumForCanonical.class, String.class);
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
        HierarchicType sub = TypeFactory._findSuperTypeChain(MyStringIntMap.class, HashMap.class);
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
        HierarchicType sub = TypeFactory._findSuperTypeChain(MyList.class, List.class);
        assertNotNull(sub);
        assertEquals(2, _countSupers(sub));
        assertSame(MyList.class, sub.getRawClass());
        HierarchicType sup = sub.getSuperType();
        assertSame(IntermediateList.class, sup.getRawClass());
        HierarchicType sup2 = sup.getSuperType();
        assertSame(List.class, sup2.getRawClass());
        assertNull(sup2.getSuperType());
        
        // Then Map
        sub = TypeFactory._findSuperTypeChain(MyMap.class, Map.class);
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
        JavaType type = TypeFactory.type(new TypeReference<AtomicReference<long[]>>() { });
        HierarchicType sub = TypeFactory._findSuperTypeChain(type.getRawClass(), AtomicReference.class);
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
        JavaType type = TypeFactory.type(new TypeReference<Map<String,Boolean>>() { });
        MapType mapType = (MapType) type;
        assertEquals(TypeFactory.type(String.class), mapType.getKeyType());
        assertEquals(TypeFactory.type(Boolean.class), mapType.getContentType());
    }

    /**
     * @since 1.6
     */
    public void testMapTypesRaw()
    {
        JavaType type = TypeFactory.type(HashMap.class);
        MapType mapType = (MapType) type;
        assertEquals(TypeFactory.type(Object.class), mapType.getKeyType());
        assertEquals(TypeFactory.type(Object.class), mapType.getContentType());        
    }

    /**
     * @since 1.6
     */
    public void testMapTypesAdvanced()
    {
        JavaType type = TypeFactory.type(MyMap.class);
        MapType mapType = (MapType) type;
        assertEquals(TypeFactory.type(String.class), mapType.getKeyType());
        assertEquals(TypeFactory.type(Long.class), mapType.getContentType());

        type = TypeFactory.type(MapInterface.class);
        mapType = (MapType) type;
        assertEquals(TypeFactory.type(String.class), mapType.getKeyType());
        assertEquals(TypeFactory.type(Integer.class), mapType.getContentType());

        type = TypeFactory.type(MyStringIntMap.class);
        mapType = (MapType) type;
        assertEquals(TypeFactory.type(String.class), mapType.getKeyType());
        assertEquals(TypeFactory.type(Integer.class), mapType.getContentType());
    }

    /**
     * Specific test to verify that complicate name mangling schemes
     * do not fool type resolver
     * 
     * @since 1.6
     */
    public void testMapTypesSneaky()
    {
        JavaType type = TypeFactory.type(IntLongMap.class);
        MapType mapType = (MapType) type;
        assertEquals(TypeFactory.type(Integer.class), mapType.getKeyType());
        assertEquals(TypeFactory.type(Long.class), mapType.getContentType());
    }    
    
    /**
     * Plus sneaky types may be found via introspection as well.
     * 
     * @since 1.7
     */
    public void testSneakyFieldTypes() throws Exception
    {
        Field field = SneakyBean.class.getDeclaredField("intMap");
        JavaType type = TypeFactory.type(field.getGenericType());
        assertTrue(type instanceof MapType);
        MapType mapType = (MapType) type;
        assertEquals(TypeFactory.type(Integer.class), mapType.getKeyType());
        assertEquals(TypeFactory.type(Long.class), mapType.getContentType());

        field = SneakyBean.class.getDeclaredField("longList");
        type = TypeFactory.type(field.getGenericType());
        assertTrue(type instanceof CollectionType);
        CollectionType collectionType = (CollectionType) type;
        assertEquals(TypeFactory.type(Long.class), collectionType.getContentType());
    }    
    
    /**
     * Looks like type handling actually differs for properties, too.
     * 
     * @since 1.7
     */
    public void testSneakyBeamProperties() throws Exception
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
        JavaType type = TypeFactory.type(new TypeReference<AtomicReference<long[]>>() { });
        JavaType[] params = TypeFactory.findParameterTypes(type, AtomicReference.class);
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(TypeFactory.type(long[].class), params[0]);
    }

    public void testSneakySelfRefs() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new SneakyBean2());
        assertEquals("{\"foobar\":null}", json);
    }
}

