package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.codehaus.jackson.map.DeserializerFactory;
import org.codehaus.jackson.map.DeserializerProvider;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.type.*;

/**
 * Factory class that can provide deserializers for standard JDK classes,
 * as well as custom classes that extend standard classes or implement
 * one of "well-known" interfaces (such as {@link java.util.Collection}).
 *<p>
 * Since all the deserializers are eagerly instantiated, and there is
 * no additional introspection or customazibility of these types,
 * this factory is stateless. This means that other delegating
 * factories (or {@link DeserializerProvider}s) can just use the
 * shared singleton instance via static {@link #instance} field.
 */
@SuppressWarnings("unchecked")
public class StdDeserializerFactory
    extends DeserializerFactory
{
    // // Can cache some types

    final static JavaType _typeString = TypeFactory.instance.fromClass(String.class);

    /* We do some defaulting for abstract Map classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Maps will do.
     */
    final static HashMap<String, Class<? extends Map>> _mapFallbacks;
    static {
        _mapFallbacks = new HashMap<String, Class<? extends Map>>();

        _mapFallbacks.put(Map.class.getName(), LinkedHashMap.class);
        _mapFallbacks.put(ConcurrentMap.class.getName(), ConcurrentHashMap.class);
        _mapFallbacks.put(SortedMap.class.getName(), TreeMap.class);
        _mapFallbacks.put(NavigableMap.class.getName(), TreeMap.class);
        _mapFallbacks.put(ConcurrentNavigableMap.class.getName(), ConcurrentSkipListMap.class);
    }

    /* We do some defaulting for abstract Map classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Maps will do.
     */
    final static HashMap<String, Class<? extends Collection>> _collectionFallbacks;
    static {
        _collectionFallbacks = new HashMap<String, Class<? extends Collection>>();

        _collectionFallbacks.put(Collection.class.getName(), ArrayList.class);
        _collectionFallbacks.put(List.class.getName(), ArrayList.class);
        _collectionFallbacks.put(Set.class.getName(), HashSet.class);
        _collectionFallbacks.put(SortedSet.class.getName(), TreeSet.class);
        _collectionFallbacks.put(Queue.class.getName(), LinkedList.class);
        _collectionFallbacks.put(Deque.class.getName(), LinkedList.class);
    }

    /**
     * And finally, we have special array deserializers for primitive
     * array types
     */
    final static HashMap<JavaType,JsonDeserializer<Object>> _arrayDeserializers = ArrayDeserializers.getAll();

    /*
    ////////////////////////////////////////////////////////////
    // Life cycle
    ////////////////////////////////////////////////////////////
     */

    public final static StdDeserializerFactory instance = new StdDeserializerFactory();

    /**
     * We will provide default constructor to allow sub-classing,
     * but make it protected so that no non-singleton instances of
     * the class will be instantiated.
     */
    protected StdDeserializerFactory() { }

    /*
    ////////////////////////////////////////////////////////////
    // JsonDeserializerFactory impl
    ////////////////////////////////////////////////////////////
     */

    public JsonDeserializer<Object> createArrayDeserializer(ArrayType type, DeserializerProvider p)
    {
        // Ok; first: do we have a primitive type?
        JavaType elemType = type.getComponentType();

        // First, special type(s):
        JsonDeserializer<Object> deser = _arrayDeserializers.get(elemType);
        /* !!! 08-Jan-2008, tatu: No primitive array deserializers yet,
         *   need to complete
         */
        if (deser != null) {
            return deser;
        }

        // If not, generic one:
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(elemType, this);
        return new ArrayDeserializer(type, valueDes);
    }

    public JsonDeserializer<?> createMapDeserializer(MapType type, DeserializerProvider p)
    {
        JavaType keyType = type.getKeyType();
        // Value handling is identical for all, so:
        JavaType valueType = type.getValueType();
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(valueType, this);

        Class<?> mapClass = type.getRawClass();
        // But EnumMap requires special handling for keys
        if (EnumMap.class.isAssignableFrom(mapClass)) {
            return new EnumMapDeserializer(EnumResolver.constructFor(keyType.getRawClass()), valueDes);
        }

        /* Otherwise, generic handler works ok; need a key deserializer (null 
         * indicates 'default' here)
         */
        KeyDeserializer keyDes = (_typeString.equals(keyType)) ? null : p.findKeyDeserializer(keyType);

        /* But there is one more twist: if we are being asked to instantiate
         * an interface or abstract Map, we need to either find something
         * that implements the thing, or give up.
         *
         * Note that we do NOT try to guess based on secondary interfaces
         * here; that would probably not work correctly since casts would
         * fail later on (as the primary type is not the interface we'd
         * be implementing)
         */
        if (type.isInterface() || type.isAbstract()) {
            Class<? extends Map> fallback = _mapFallbacks.get(mapClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Map type "+type);
            }
            mapClass = fallback;
        }
        return new MapDeserializer(mapClass, keyDes, valueDes);
    }

    public JsonDeserializer<?> createCollectionDeserializer(CollectionType type, DeserializerProvider p)
    {
        JavaType valueType = type.getElementType();

        Class<?> collectionClass = type.getRawClass();

        // One special type: EnumSet:
        if (EnumSet.class.isAssignableFrom(collectionClass)) {
            return new EnumSetDeserializer(EnumResolver.constructFor(valueType.getRawClass()));
        }

        // But otherwise we can just use a generic value deserializer:
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(valueType, this);

        /* One twist: if we are being asked to instantiate an interface or
         * abstract Collection, we need to either find something that implements
         * the thing, or give up.
         *
         * Note that we do NOT try to guess based on secondary interfaces
         * here; that would probably not work correctly since casts would
         * fail later on (as the primary type is not the interface we'd
         * be implementing)
         */
        if (type.isInterface() || type.isAbstract()) {
            Class<? extends Map> fallback = _mapFallbacks.get(collectionClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Collection type "+type);
            }
            collectionClass = fallback;
        }
        return new CollectionDeserializer(collectionClass, valueDes);
    }

    public JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p)
    {
        // First let's figure out scalar value - based construction aspects:

        Class<?> beanClass = type.getRawClass();
        Constructor<?>[] ctors = beanClass.getDeclaredConstructors();
        List<Method> staticMethods = findStaticFactoryMethods(beanClass);

        BeanDeserializer.StringConstructor sctor = constructStringConstructor(beanClass, ctors, staticMethods);
        BeanDeserializer.NumberConstructor nctor = constructNumberConstructor(beanClass, ctors, staticMethods);

        // And then things we need if we get Json Object:

        // !!! TBI

        return new BeanDeserializer(beanClass, null,null, sctor, nctor);
    }

    public JsonDeserializer<Object> createEnumDeserializer(SimpleType type, DeserializerProvider p)
    {
        JsonDeserializer<?> des = new EnumDeserializer(EnumResolver.constructFor(type.getRawClass()));
        JsonDeserializer<Object> result = (JsonDeserializer<Object>) des;
        return result;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for Bean deserializer
    ////////////////////////////////////////////////////////////
     */

    /**
     * Method that will find all single-arg static methods that given
     * class declares, and that construct instance of the class (or
     * one of its subclasses).
     */
    List<Method> findStaticFactoryMethods(Class<?> clz)
    {
        ArrayList<Method> result = new ArrayList<Method>();
        for (Method m : clz.getDeclaredMethods()) {
            // Needs to be static
            if (!Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            /* And return something compatible with the class itself:
             * for now class itself or one of its sub-classes
             */
            Class<?> resultType = m.getReturnType();
            if (!clz.isAssignableFrom(resultType)) {
                continue;
            }
            // And take 1 (and only one) arg:
            if (m.getParameterTypes().length != 1) {
                continue;
            }
            // If so, it might be a candidate
            result.add(m);
        }
        return result;
    }

    BeanDeserializer.StringConstructor constructStringConstructor(Class<?> beanClass, Constructor<?>[] ctors, List<Method> staticMethods)
    {
        Constructor<?> sctor = null;

        // must find 1-string-arg one
        for (Constructor<?> c : ctors) {
            Class<?>[] args = c.getParameterTypes();
            if (args.length == 1) {
                if (args[0] == String.class) {
                    checkAccess(c, beanClass);
                    sctor = c;
                    break;
                }
            }
        }

        // and/or one of "well-known" factory methods
        Method factoryMethod = null;

        for (Method m : staticMethods) {
            /* must be named "valueOf", for now; other candidates?
             */
            if ("valueOf".equals(m.getName())) {
                // must take String arg
                Class<?> arg = m.getParameterTypes()[0];
                if (arg == String.class) {
                    checkAccess(m, beanClass);
                    factoryMethod = m;
                    break;
                }
            }
        }

        return new BeanDeserializer.StringConstructor(beanClass, sctor, factoryMethod);
    }

    BeanDeserializer.NumberConstructor constructNumberConstructor(Class<?> beanClass, Constructor<?>[] ctors, List<Method> staticMethods)
    {
        Constructor<?> intCtor = null;
        Constructor<?> longCtor = null;

        // must find 1-int/long-arg one
        for (Constructor<?> c : ctors) {
            Class<?>[] args = c.getParameterTypes();
            if (args.length != 1) {
                continue;
            }
            Class<?> argType = args[0];
            if (argType == int.class || argType == Integer.class) {
                checkAccess(c, beanClass);
                intCtor = c;
            } else if (argType == long.class || argType == Long.class) {
                checkAccess(c, beanClass);
                longCtor = c;
            }
        }

        // and/or one of "well-known" factory methods
        Method intFactoryMethod = null;
        Method longFactoryMethod = null;

        for (Method m : staticMethods) {
            /* must be named "valueOf", for now; other candidates?
             */
            if ("valueOf".equals(m.getName())) {
                // must take String arg
                Class<?> argType = m.getParameterTypes()[0];
                if (argType == int.class || argType == Integer.class) {
                    checkAccess(m, beanClass);
                    intFactoryMethod = m;
                } else if (argType == long.class || argType == Long.class) {
                    checkAccess(m, beanClass);
                    longFactoryMethod = m;
                }
            }
        }
        return new BeanDeserializer.NumberConstructor(beanClass, intCtor, longCtor, intFactoryMethod, longFactoryMethod);
    }

    /**
     * Method called to check if we can use the passed method or constructor
     * (wrt access restriction -- public methods can be called, others
     * usually not); and if not, if there is a work-around for
     * the problem.
     */
    protected void checkAccess(AccessibleObject obj, Class<?> declClass)
    {
        if (!obj.isAccessible()) {
            try {
                obj.setAccessible(true);
            } catch (SecurityException se) {
                throw new IllegalArgumentException("Can not access "+obj+" (from class "+declClass.getName()+"; failed to set access: "+se.getMessage());
            }
        }
    }
}
