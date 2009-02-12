package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
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

    @Override
	public JsonDeserializer<Object> createArrayDeserializer(ArrayType type, DeserializerProvider p)
        throws JsonMappingException
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
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(elemType, type, null);
        return new ArrayDeserializer(type, valueDes);
    }

    @Override
	public JsonDeserializer<?> createMapDeserializer(MapType type, DeserializerProvider p)
        throws JsonMappingException
    {
        JavaType keyType = type.getKeyType();
        // Value handling is identical for all, so:
        JavaType valueType = type.getValueType();
        // 'null' -> maps have no referring fields
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(valueType, type, null);

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

    @Override
	public JsonDeserializer<?> createCollectionDeserializer(CollectionType type, DeserializerProvider p)
        throws JsonMappingException
    {
        JavaType valueType = type.getElementType();

        Class<?> collectionClass = type.getRawClass();

        // One special type: EnumSet:
        if (EnumSet.class.isAssignableFrom(collectionClass)) {
            return new EnumSetDeserializer(EnumResolver.constructFor(valueType.getRawClass()));
        }

        // But otherwise we can just use a generic value deserializer:
        // 'null' -> collections have no referring fields
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(valueType, type, null);

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

    @Override
	public JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p)
        throws JsonMappingException
    {
        // Very first thing: do we even handle this type as a Bean?
        Class<?> beanClass = type.getRawClass();
        if (!isPotentialBeanType(beanClass)) {
            return null;
        }

        // And then: maybe it's explicitly defined by annotations?
        JsonDeserializer<Object> ad = findDeserializerByAnnotation(beanClass);
        if (ad != null) {
            return ad;
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

    @Override
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
        Class<?> beanClass = deser.getBeanClass();
        ClassIntrospector intr = new ClassIntrospector(beanClass);

        LinkedHashMap<String,Method> methodsByProp = intr.findSetters();

        /* No setters? Should we proceed here? It may well be ok, if
         * there are factory methods or such.
         */
        //if (methodsByProp.isEmpty()) ...

        ArrayList<SettableBeanProperty> props = new ArrayList<SettableBeanProperty>(methodsByProp.size());

        // These are all valid setters, but we do need to introspect bit more
        for (Map.Entry<String,Method> en : methodsByProp.entrySet()) {
            String name = en.getKey();
            Method m = en.getValue();
            // need to ensure it is callable now:
            ClassUtil.checkAndFixAccess(m, m.getDeclaringClass());

            // note: this works since we know there's exactly one arg for methods
            Type rawType = m.getGenericParameterTypes()[0];
            JavaType type = TypeFactory.instance.fromType(rawType);

            SettableBeanProperty prop = new SettableBeanProperty(name, type, m);
            SettableBeanProperty oldP = deser.addSetter(prop);
            if (oldP != null) { // can this ever occur?
                throw new IllegalArgumentException("Duplicate property '"+name+"' for class "+beanClass.getName());
            }

            /* One more thing: does Method specify a serializer?
             * If so, let's use it.
             */
            JsonDeserializer<Object> propDeser = findDeserializerByAnnotation(m);
            if (propDeser != null) {
                prop.setValueDeserializer(propDeser);
            }
        }
    }

    /**
     * Helper method called to check if the class in question
     * has {@link JsonUseDeserializer} annotation which tells the
     * class to use for deserialization.
     * Returns null if no such annotation found.
     */
    protected JsonDeserializer<Object> findDeserializerByAnnotation(AnnotatedElement elem)
    {
        JsonUseDeserializer ann = elem.getAnnotation(JsonUseDeserializer.class);
        if (ann != null) {
            Class<?> deserClass = ann.value();
            // Must be of proper type, of course
            if (!JsonDeserializer.class.isAssignableFrom(deserClass)) {
                throw new IllegalArgumentException("Invalid @JsonDeserializer annotation for "+ClassUtil.descFor(elem)+": value ("+deserClass.getName()+") does not implement JsonDeserializer interface");
            }
            try {
                Object ob = deserClass.newInstance();
                @SuppressWarnings("unchecked")
                    JsonDeserializer<Object> ser = (JsonDeserializer<Object>) ob;
                return ser;
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to instantiate "+deserClass.getName()+" to use as deserializer for "+ClassUtil.descFor(elem)+", problem: "+e.getMessage(), e);
            }
        }
        return null;
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
            if (c.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
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

        // note: @JsonIgnore handled earlier
        for (Method m : staticMethods) {
            // must be named "valueOf", for now; other candidates?
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
            if (c.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
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

        // note: @JsonIgnore handled earlier
        for (Method m : staticMethods) {
            // must be named "valueOf", for now; other candidates?
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
                // 11-Feb-2009, tatu: Also, must ignore if instructed to:
                if (!ctor.isAnnotationPresent(JsonIgnore.class)) {
                    return ctor;
                }
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
            // and can't include things that must be ignored
            if (m.isAnnotationPresent(JsonIgnore.class)) {
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
