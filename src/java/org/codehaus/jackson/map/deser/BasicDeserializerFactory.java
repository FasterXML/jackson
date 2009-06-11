package org.codehaus.jackson.map.deser;

import java.util.*;
import java.util.concurrent.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.type.JavaType;

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

    final static JavaType TYPE_STRING = TypeFactory.fromClass(String.class);

    /**
     * We will pre-create serializers for common non-structured
     * (that is things other than Collection, Map or array)
     * types. These need not go through factory.
     */
    final static HashMap<JavaType, JsonDeserializer<Object>> _simpleDeserializers = StdDeserializers.constructAll();


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
	public JsonDeserializer<?> createArrayDeserializer(DeserializationConfig config, ArrayType type, DeserializerProvider p)
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
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(config, elemType, type, null);
        return new ArrayDeserializer(type, valueDes);
    }

    @Override
    public JsonDeserializer<?> createCollectionDeserializer(DeserializationConfig config, CollectionType type, DeserializerProvider p)
        throws JsonMappingException
    {
        JavaType valueType = type.getElementType();

        Class<?> collectionClass = type.getRawClass();

        // One special type: EnumSet:
        if (EnumSet.class.isAssignableFrom(collectionClass)) {
            return new EnumSetDeserializer(EnumResolver.constructFor(valueType.getRawClass(), config.getAnnotationIntrospector()));
        }

        // But otherwise we can just use a generic value deserializer:
        // 'null' -> collections have no referring fields
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(config, valueType, type, null);

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
   public JsonDeserializer<?> createMapDeserializer(DeserializationConfig config, MapType type, DeserializerProvider p)
        throws JsonMappingException
    {
        JavaType keyType = type.getKeyType();
        // Value handling is identical for all, so:
        JavaType valueType = type.getValueType();
        // 'null' -> maps have no referring fields
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(config, valueType, type, null);

        Class<?> mapClass = type.getRawClass();
        // But EnumMap requires special handling for keys
        if (EnumMap.class.isAssignableFrom(mapClass)) {
            return new EnumMapDeserializer(EnumResolver.constructFor(keyType.getRawClass(), config.getAnnotationIntrospector()), valueDes);
        }

        /* Otherwise, generic handler works ok; need a key deserializer (null
         * indicates 'default' here)
         */
        KeyDeserializer keyDes = (TYPE_STRING.equals(keyType)) ? null : p.findKeyDeserializer(config, keyType);

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
    public JsonDeserializer<?> createEnumDeserializer(DeserializationConfig config, Class<?> enumClass, DeserializerProvider p)
        throws JsonMappingException
    {
        /* 18-Feb-2009, tatu: Must first check if we have a class annotation
         *    that should override default deserializer
         */
        BasicBeanDescription beanDesc = config.introspectClassAnnotations(enumClass);
        JsonDeserializer<Object> des = findDeserializerFromAnnotation(config, beanDesc.getClassInfo());
        if (des != null) {
            return des;
        }
        JsonDeserializer<?> d2 = new EnumDeserializer(EnumResolver.constructFor(enumClass, config.getAnnotationIntrospector()));
        return (JsonDeserializer<Object>) d2;
    }

    @Override
    public JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config, Class<? extends JsonNode> nodeClass, DeserializerProvider p)
        throws JsonMappingException
    {
        /* !!! 02-Mar-2009, tatu: Should probably allow specifying more
         *   accurate nodes too...
         */
        /*
        if (ArrayNode.class.isAssignableFrom(nodeClass)) {
            // !!! TBI
        }
        if (ObjectNode.class.isAssignableFrom(nodeClass)) {
            // !!! TBI
        }
        */
        // For plain old JsonNode, we'll return basic deserializer:
        return JsonNodeDeserializer.instance;
    }

    @Override
    public JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config, JavaType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return _simpleDeserializers.get(type);
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods, value/content/key type introspection
    ////////////////////////////////////////////////////////////
     */

    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization.
     * Returns null if no such annotation found.
     */
    @SuppressWarnings("unchecked")
    protected JsonDeserializer<Object> findDeserializerFromAnnotation(DeserializationConfig config, Annotated a)
    {
        return (JsonDeserializer<Object>) config.getAnnotationIntrospector().getDeserializerInstance(a);
    }

    /**
     * Method called to see if given method has annotations that indicate
     * a more specific type than what the argument specifies.
     * If annotations are present, they must specify compatible Class;
     * instance of which can be assigned using the method. This means
     * that the Class has to be raw class of type, or its sub-class
     * (or, implementing class if original Class instance is an interface).
     *
     * @param a Method or field that the type is associated with
     * @param type Type derived from the setter argument
     *
     * @return Original type if no annotations are present; or a more
     *   specific type derived from it if type annotation(s) was found
     *
     * @throws JsonMappingException if invalid annotation is found
     */
    protected JavaType modifyTypeByAnnotation(DeserializationConfig config,
                                              Annotated a, JavaType type)
        throws JsonMappingException
    {
        // first: let's check class for the instance itself:
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        Class<?> subclass = intr.findDeserializationType(a);
        if (subclass != null) {
            try {
                type = type.narrowBy(subclass);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow type "+type+" with concrete-type annotation (value "+subclass.getName()+"), method '"+a.getName()+"': "+iae.getMessage(), null, iae);
            }
        }

        // then key class
        Class<?> keyClass = intr.findDeserializationKeyType(a);
        if (keyClass != null) {
            // illegal to use on non-Maps
            if (!(type instanceof MapType)) {
                throw new JsonMappingException("Illegal key-type annotation: type "+type+" is not a Map type");
            }
            try {
                type = ((MapType) type).narrowKey(keyClass);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow key type "+type+" with key-type annotation ("+keyClass.getName()+"): "+iae.getMessage(), null, iae);
            }
        }

        // and finally content class; only applicable to structured types
        Class<?> cc = intr.findDeserializationContentType(a);
        if (cc != null) {
            if (!type.isContainerType()) {
                throw new JsonMappingException("Illegal content-type annotation on "+a.getName()+"; can only be used for container types (Collections, Maps, arrays");
            }
            try {
                type = type.narrowContentsBy(cc);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow content type "+type+" with content-type annotation ("+cc.getName()+"): "+iae.getMessage(), null, iae);
            }
        }
        return type;
    }
}
