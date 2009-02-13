package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Abstract factory base class that can provide deserializers for standard
 * JDK classes, including collection classes and simple heuristics for
 * "upcasting" commmon collection interface types
 * (such as {@link java.util.Collection}).
 *<p>
 * Since all simple deserializers are eagerly instantiated, and there is
 * no additional introspection or customazibility of these types,
 * this factory is stateless.
 */
public abstract class BasicDeserializerFactory
    extends DeserializerFactory
{
    // // Can cache some types

    final static JavaType _typeString = TypeFactory.instance.fromClass(String.class);

    /* We do some defaulting for abstract Map classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Maps will do.
     */
    @SuppressWarnings("unchecked")
	final static HashMap<String, Class<? extends Map>> _mapFallbacks =
        new HashMap<String, Class<? extends Map>>();
    static {

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
            @SuppressWarnings("unchecked")
                Class<? extends Map> mapValue = (Class<? extends Map>) value;
            _mapFallbacks.put(key.getName(), mapValue);
        } catch (ClassNotFoundException cnfe) { // occurs on 1.5
        }
    }

    /* We do some defaulting for abstract Map classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Maps will do.
     */
    @SuppressWarnings("unchecked")
	final static HashMap<String, Class<? extends Collection>> _collectionFallbacks =
        new HashMap<String, Class<? extends Collection>>();
    static {
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

    protected BasicDeserializerFactory() { }

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
            @SuppressWarnings("unchecked")
            Class<? extends Collection> fallback = _collectionFallbacks.get(collectionClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Collection type "+type);
            }
            collectionClass = fallback;
        }
        return new CollectionDeserializer(collectionClass, valueDes);
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
            @SuppressWarnings("unchecked")
            Class<? extends Map> fallback = _mapFallbacks.get(mapClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Map type "+type);
            }
            mapClass = fallback;
        }
        return new MapDeserializer(mapClass, keyDes, valueDes);
    }

    /**
     * Factory method for constructing serializers of {@link Enum} types.
     */
    @Override
    @SuppressWarnings("unchecked")
	public JsonDeserializer<Object> createEnumDeserializer(SimpleType type, DeserializerProvider p)
    {
        JsonDeserializer<?> des = new EnumDeserializer(EnumResolver.constructFor(type.getRawClass()));
        JsonDeserializer<Object> result = (JsonDeserializer<Object>) des;
        return result;
    }

    @Override
    public abstract JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p)
        throws JsonMappingException;

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods, value/content/key type introspection
    ////////////////////////////////////////////////////////////
     */

    /**
     * Method called to see if given method has annotations that indicate
     * a more specific type than what the argument specifies.
     * If annotations are present, they must specify compatible Class;
     * instance of which can be assigned using the method. This means
     * that the Class has to be raw class of type, or its sub-class
     * (or, implementing class if original Class instance is an interface).
     *
     * @param m Setter method that the type is associated with
     * @param type Type derived from the setter argument
     *
     * @return Original type if no annotations are present; or a more
     *   specific type derived from it if type annotation(s) was found
     *
     * @throws JsonMappingException if invalid annotation is found
     */
    protected JavaType modifyTypeByAnnotation(Method m, JavaType type)
        throws JsonMappingException
    {
        // first: let's check class for the instance itself:
        JsonClass mainAnn = m.getAnnotation(JsonClass.class);
        if (mainAnn != null) {
            Class<?> subclass = mainAnn.value();
            try {
                type = type.narrowBy(subclass);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow type "+type+" with @JsonClass("+subclass.getName()+"): "+iae.getMessage(), null, iae);
            }
        }

        // then key class
        JsonKeyClass keyAnn = m.getAnnotation(JsonKeyClass.class);
        if (keyAnn != null) {
            // illegal to use on non-Maps
            if (!(type instanceof MapType)) {
                throw new JsonMappingException("Illegal @JsonKey annotation: type "+type+" is not a Map type");
            }
            Class<?> keyClass = keyAnn.value();
            try {
                type = ((MapType) type).narrowKey(keyClass);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow key of "+type+" with @JsonKeyClass("+keyClass.getName()+"): "+iae.getMessage(), null, iae);
            }
        }

        // and finally content class; only applicable to structured types
        JsonContentClass contentAnn = m.getAnnotation(JsonContentClass.class);
        if (contentAnn != null) {
            if (!type.isContainerType()) {
                throw new JsonMappingException("Illegal @JsonContentClass annotation on "+ClassUtil.descFor(m)+"; can only be used for container types (Collections, Maps, arrays");
            }
            Class<?> cc = contentAnn.value();
            try {
                type = type.narrowContentsBy(cc);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow content type "+type+" with @JsonContentClass("+cc.getName()+"): "+iae.getMessage(), null, iae);
            }
        }
        return type;
    }
}
