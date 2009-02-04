package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.codehaus.jackson.map.DeserializerFactory;
import org.codehaus.jackson.map.DeserializerProvider;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.map.util.ClassUtil;

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

        /* 11-Jan-2009, tatu: Let's see if we can still add support for
         *    JDK 1.6 interfaces, even if we run on 1.5. Just need to be
         *    more careful with typos, since compiler won't notice any
         *    problems...
         */
        _mapFallbacks.put("java.util.NavigableMap", TreeMap.class);
        try {
            Class<?> key = Class.forName("java.util.ConcurrentNavigableMap");
            Class<?> value = Class.forName("java.util.ConcurrentSkipListMap");
            _mapFallbacks.put(key.getName(), (Class<? extends Map>) value);
        } catch (ClassNotFoundException cnfe) { // occurs on 1.5
        }
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

        /* 11-Jan-2009, tatu: Let's see if we can still add support for
         *    JDK 1.6 interfaces, even if we run on 1.5. Just need to be
         *    more careful with typos, since compiler won't notice any
         *    problems...
         */
        _collectionFallbacks.put("java.util.Deque", LinkedList.class);
        _collectionFallbacks.put("java.util.NavigableSet", TreeSet.class);
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

        // First, special type(s), such as "primitive" arrays (int[] etc)
        JsonDeserializer<Object> deser = _arrayDeserializers.get(elemType);
        if (deser != null) {
            return deser;
        }

        // If not, generic one:
        if (elemType.isPrimitive()) { // sanity check
            throw new IllegalArgumentException("Internal error: primitive type ("+type+") passed, no array deserializer found");
        }
        // 'null' -> arrays have no referring fields
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(elemType, this, type, null);
        return new ArrayDeserializer(type, valueDes);
    }

    public JsonDeserializer<?> createMapDeserializer(MapType type, DeserializerProvider p)
    {
        JavaType keyType = type.getKeyType();
        // Value handling is identical for all, so:
        JavaType valueType = type.getValueType();
        // 'null' -> maps have no referring fields
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(valueType, this, type, null);

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
        // 'null' -> collections have no referring fields
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(valueType, this, type, null);

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
            Class<? extends Collection> fallback = _collectionFallbacks.get(collectionClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Collection type "+type);
            }
            collectionClass = fallback;
        }
        return new CollectionDeserializer(collectionClass, valueDes);
    }

    public JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p)
    {
        // Very first thing: do we even handle this type as a Bean?
        Class<?> beanClass = type.getRawClass();
        if (!isPotentialBeanType(beanClass)) {
            return null;
        }

        /* Ok then: let's figure out scalar value - based construction
         * aspects.
         *
         * !!! 09-Jan-2009, tatu: Should we allow construction from Map
         *   (which would then be assumed to be "untyped")?
         *   Or maybe even from a List (ditto)?
         */

        Constructor<?>[] ctors = beanClass.getDeclaredConstructors();
        List<Method> staticMethods = findStaticFactoryMethods(beanClass);

        BeanDeserializer.StringConstructor sctor = constructStringConstructor(beanClass, ctors, staticMethods);
        BeanDeserializer.NumberConstructor nctor = constructNumberConstructor(beanClass, ctors, staticMethods);
        Constructor<?> defaultCtor = findDefaultConstructor(ctors);

        // sanity check: must have a constructor of one type or another
        if ((sctor == null) && (nctor == null) && (defaultCtor == null)) {
            throw new IllegalArgumentException("Can not create Bean deserializer for ("+type+"): neither default constructor nor factory methods found");
        }
        if (defaultCtor != null) {
            ClassUtil.checkAndFixAccess(defaultCtor, beanClass);
        }

        BeanDeserializer deser = new BeanDeserializer(type, defaultCtor, sctor, nctor);
        // And then things we need if we get Json Object:
        addBeanProps(deser);
        return deser;
    }

    public JsonDeserializer<Object> createEnumDeserializer(SimpleType type, DeserializerProvider p)
    {
        JsonDeserializer<?> des = new EnumDeserializer(EnumResolver.constructFor(type.getRawClass()));
        JsonDeserializer<Object> result = (JsonDeserializer<Object>) des;
        return result;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for Bean deserializer: property handling
    ////////////////////////////////////////////////////////////
     */

    /**
     * Method called to figure out settable properties for the
     * deserializer.
     */
    protected void addBeanProps(BeanDeserializer deser)
    {
        /* Ok, now; we could try Class.getMethods(), but it has couple of
         * problems:
         *
         * (a) Only returns public methods (which is ok for accessor checks,
         *   but should allow annotations to indicate others too)
         * (b) Ordering is arbitrary (may be a problem with other accessors
         *   too?)
         *
         * So: let's instead gather methods ourself. One simplification is
         * that we should always be getting concrete type; hence we need
         * not worry about interfaces or such. Also, we can ignore everything
         * from java.lang.Object, which is neat.
         * 
         * ... too bad that we still won't necessarily get properly
         * ordered (by declaration order or name) List either way
         */
        LinkedHashMap<String,Method> methods = new LinkedHashMap<String,Method>();
        Class<?> beanClass = deser.getBeanClass();
        findCandidateMethods(beanClass, methods);

        // Ok: potential candidates are more than actual mutators...
        for (Method m : methods.values()) {
            String name = okNameForMutator(m);
            if (name == null) {
                continue;
            }
            // need to ensure it is callable now:
            ClassUtil.checkAndFixAccess(m, m.getDeclaringClass());

            Type rawType = m.getGenericParameterTypes()[0];
            JavaType type = TypeFactory.instance.fromType(rawType);

            SettableBeanProperty prop = new SettableBeanProperty(name, type, m);
            SettableBeanProperty oldP = deser.addSetter(prop);
            if (oldP != null) {
                throw new IllegalArgumentException("Duplicate property '"+name+"' for class "+beanClass.getName());
            }
        }
    }

    /**
     * Method for collecting list of all Methods that could conceivably
     * be mutators. At this point we will only do preliminary checks,
     * to weed out things that can not possibly be mutators (i.e. solely
     * based on signature, but not on name or annotations)
     */
    protected void findCandidateMethods(Class<?> type, Map<String,Method> result)
    {
        /* we'll add base class methods first (for ordering purposes), but
         * then override as necessary
         */
        Class<?> parent = type.getSuperclass();
        if (parent != null && parent != Object.class) {
            findCandidateMethods(parent, result);
        }
        for (Method m : type.getDeclaredMethods()) {
            if (okSignatureForMutator(m)) {
                result.put(m.getName(), m);
            }
        }
    }

    protected boolean okSignatureForMutator(Method m)
    {
        // First: we can't use static methods
        if (Modifier.isStatic(m.getModifiers())) {
            return false;
        }
        // Must take a single arg
        Class<?>[] pts = m.getParameterTypes();
        if ((pts == null) || pts.length != 1) {
            return false;
        }
        // No checking for returning type; usually void, don't care

        // Otherwise, potentially ok
        return true;
    }

    protected String okNameForMutator(Method m)
    {
        String name = m.getName();

        /* For mutators, let's not require it to be public. Just need
         * to be able to call it, i.e. do need to 'fix' access if so
         * (which is done at a later point as needed)
         */
        if (name.startsWith("set")) {
            name = mangleName(m, name.substring(3));
            if (name == null) { // plain old "set" is no good...
                return null;
            }
            return name;
        }
        return null;
    }

    /**
     * @return Null to indicate that method is not a valid mutator;
     *   otherwise name of the property it is mutator for
     */
    protected String mangleName(Method method, String basename)
    {
        return ClassUtil.manglePropertyName(basename);
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for Bean deserializer: factory methods
    ////////////////////////////////////////////////////////////
     */

    BeanDeserializer.StringConstructor constructStringConstructor(Class<?> beanClass, Constructor<?>[] ctors, List<Method> staticMethods)
    {
        Constructor<?> sctor = null;

        // must find 1-string-arg one
        for (Constructor<?> c : ctors) {
            Class<?>[] args = c.getParameterTypes();
            if (args.length == 1) {
                if (args[0] == String.class) {
                    ClassUtil.checkAndFixAccess(c, beanClass);
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
                    ClassUtil.checkAndFixAccess(m, beanClass);
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
                ClassUtil.checkAndFixAccess(c, beanClass);
                intCtor = c;
            } else if (argType == long.class || argType == Long.class) {
                ClassUtil.checkAndFixAccess(c, beanClass);
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
                    ClassUtil.checkAndFixAccess(m, beanClass);
                    intFactoryMethod = m;
                } else if (argType == long.class || argType == Long.class) {
                    ClassUtil.checkAndFixAccess(m, beanClass);
                    longFactoryMethod = m;
                }
            }
        }
        return new BeanDeserializer.NumberConstructor(beanClass, intCtor, longCtor, intFactoryMethod, longFactoryMethod);
    }

    protected Constructor<?> findDefaultConstructor(Constructor<?>[] ctors)
    {
        for (Constructor<?> ctor : ctors) {
            // won't use varargs, no point
            if (!ctor.isVarArgs() && ctor.getParameterTypes().length == 0) {
                return ctor;
            }
        }
        return null;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for Bean deserializer, other
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

    /**
     * Helper method used to skip processing for types that we know
     * can not be (i.e. are never consider to be) beans: 
     * things like primitives, Arrays, Enums, and proxy types.
     *<p>
     * Note that usually we shouldn't really be getting these sort of
     * types anyway; but better safe than sorry.
     */
    protected boolean isPotentialBeanType(Class<?> type)
    {
        String typeStr = ClassUtil.canBeABeanType(type);
        if (typeStr != null) {
            throw new IllegalArgumentException("Can not deserialize Class "+type.getName()+" (of type "+typeStr+") as a Bean");
        }
        if (ClassUtil.isProxyType(type)) {
            throw new IllegalArgumentException("Can not deserialize Proxy class "+type.getName()+" as a Bean");
        }
        // also: can't deserialize local (in-method, anonymous, non-static-enclosed) classes
        typeStr = ClassUtil.isLocalType(type);
        if (typeStr != null) {
            throw new IllegalArgumentException("Can not deserialize Class "+type.getName()+" (of type "+typeStr+") as a Bean");
        }
    	return true;
    }
}
