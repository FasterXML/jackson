package org.codehaus.jackson.map.ser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ext.OptionalHandlerFactory;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.map.util.EnumValues;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.TokenBuffer;

/**
 * Factory class that can provide serializers for standard JDK classes,
 * as well as custom classes that extend standard classes or implement
 * one of "well-known" interfaces (such as {@link java.util.Collection}).
 *<p>
 * Since all the serializers are eagerly instantiated, and there is
 * no additional introspection or customizability of these types,
 * this factory is stateless. This means that other delegating
 * factories (or {@link SerializerProvider}s) can just use the
 * shared singleton instance via static {@link #instance} field.
 */
public class BasicSerializerFactory
    extends SerializerFactory
{
    
    /*
    /**********************************************************
    /* Configuration, lookup tables/maps
    /**********************************************************
     */

    /**
     * Since these are all JDK classes, we shouldn't have to worry
     * about ClassLoader used to load them. Rather, we can just
     * use the class name, and keep things simple and efficient.
     */
    protected final static HashMap<String, JsonSerializer<?>> _concrete =
        new HashMap<String, JsonSerializer<?>>();

    /**
     * Actually it may not make much sense to eagerly instantiate all
     * kinds of serializers: so this Map actually contains class references,
     * not instances
     *
     * @since 1.6
     */
    protected final static HashMap<String, Class<? extends JsonSerializer<?>>> _concreteLazy =
        new HashMap<String, Class<? extends JsonSerializer<?>>>();

    static {
        /* String and string-like types (note: date types explicitly
         * not included -- can use either textual or numeric serialization)
         */
        _concrete.put(String.class.getName(), new StdSerializers.StringSerializer());
        final ToStringSerializer sls = ToStringSerializer.instance;
        _concrete.put(StringBuffer.class.getName(), sls);
        _concrete.put(StringBuilder.class.getName(), sls);
        _concrete.put(Character.class.getName(), sls);
        _concrete.put(Character.TYPE.getName(), sls);

        // Primitives/wrappers for primitives (primitives needed for Beans)
        _concrete.put(Boolean.TYPE.getName(), new StdSerializers.BooleanSerializer(true));
        _concrete.put(Boolean.class.getName(), new StdSerializers.BooleanSerializer(false));
        final JsonSerializer<?> intS = new StdSerializers.IntegerSerializer();
        _concrete.put(Integer.class.getName(), intS);
        _concrete.put(Integer.TYPE.getName(), intS);
        _concrete.put(Long.class.getName(), StdSerializers.LongSerializer.instance);
        _concrete.put(Long.TYPE.getName(), StdSerializers.LongSerializer.instance);
        _concrete.put(Byte.class.getName(), StdSerializers.IntLikeSerializer.instance);
        _concrete.put(Byte.TYPE.getName(), StdSerializers.IntLikeSerializer.instance);
        _concrete.put(Short.class.getName(), StdSerializers.IntLikeSerializer.instance);
        _concrete.put(Short.TYPE.getName(), StdSerializers.IntLikeSerializer.instance);

        // Numbers, limited length floating point
        _concrete.put(Float.class.getName(), StdSerializers.FloatSerializer.instance);
        _concrete.put(Float.TYPE.getName(), StdSerializers.FloatSerializer.instance);
        _concrete.put(Double.class.getName(), StdSerializers.DoubleSerializer.instance);
        _concrete.put(Double.TYPE.getName(), StdSerializers.DoubleSerializer.instance);

        // Other numbers, more complicated
        final JsonSerializer<?> ns = new StdSerializers.NumberSerializer();
        _concrete.put(BigInteger.class.getName(), ns);
        _concrete.put(BigDecimal.class.getName(), ns);

        /* Other discrete non-container types:
         * first, Date/Time zoo:
         */
        _concrete.put(Calendar.class.getName(), StdSerializers.CalendarSerializer.instance);
        _concrete.put(java.util.Date.class.getName(), StdSerializers.UtilDateSerializer.instance);
        _concrete.put(java.sql.Date.class.getName(), new StdSerializers.SqlDateSerializer());
        _concrete.put(java.sql.Time.class.getName(), new StdSerializers.SqlTimeSerializer());
        // note: timestamps are very similar to java.util.Date, thus serialized as such
        _concrete.put(java.sql.Timestamp.class.getName(), StdSerializers.UtilDateSerializer.instance);

        // Arrays of various types (including common object types)
        _concrete.put(boolean[].class.getName(), new ArraySerializers.BooleanArraySerializer());
        _concrete.put(byte[].class.getName(), new ArraySerializers.ByteArraySerializer());
        _concrete.put(char[].class.getName(), new ArraySerializers.CharArraySerializer());
        _concrete.put(short[].class.getName(), new ArraySerializers.ShortArraySerializer());
        _concrete.put(int[].class.getName(), new ArraySerializers.IntArraySerializer());
        _concrete.put(long[].class.getName(), new ArraySerializers.LongArraySerializer());
        _concrete.put(float[].class.getName(), new ArraySerializers.FloatArraySerializer());
        _concrete.put(double[].class.getName(), new ArraySerializers.DoubleArraySerializer());

        _concrete.put(Object[].class.getName(), ArraySerializers.ObjectArraySerializer.instance);
        _concrete.put(String[].class.getName(), new ArraySerializers.StringArraySerializer());

        // And then Java Collection classes
        final ContainerSerializers.IndexedListSerializer indListS = ContainerSerializers.IndexedListSerializer.instance;
        final ContainerSerializers.CollectionSerializer collectionS = ContainerSerializers.CollectionSerializer.instance;

        _concrete.put(ArrayList.class.getName(), indListS);
        _concrete.put(Vector.class.getName(), indListS);
        _concrete.put(LinkedList.class.getName(), collectionS);
        // (java.util.concurrent has others, but let's allow those to be
        // found via slower introspection; too many to enumerate here)

        final MapSerializer mapS = MapSerializer.instance;
        _concrete.put(HashMap.class.getName(), mapS);
        _concrete.put(Hashtable.class.getName(), mapS);
        _concrete.put(LinkedHashMap.class.getName(), mapS);
        _concrete.put(TreeMap.class.getName(), mapS);
        _concrete.put(Properties.class.getName(), mapS);

        _concrete.put(HashSet.class.getName(), collectionS);
        _concrete.put(LinkedHashSet.class.getName(), collectionS);
        _concrete.put(TreeSet.class.getName(), collectionS);

        // And then other standard non-structured JDK types
        for (Map.Entry<Class<?>,Object> en : new JdkSerializers().provide()) {
            Object value = en.getValue();
            if (value instanceof JsonSerializer<?>) {
                _concrete.put(en.getKey().getName(), (JsonSerializer<?>) value);
            } else if (value instanceof Class<?>) {
                @SuppressWarnings("unchecked")
                Class<? extends JsonSerializer<?>> cls = (Class<? extends JsonSerializer<?>>) value;
                _concreteLazy.put(en.getKey().getName(), cls);
            } else { // should never happen, but:
                throw new IllegalStateException("Internal error: unrecognized value of type "+en.getClass().getName());
            }
        }

        // Jackson-specific type(s)
        // (Q: can this ever be sub-classed?)
        _concreteLazy.put(TokenBuffer.class.getName(), StdSerializers.TokenBufferSerializer.class);
    }

    protected OptionalHandlerFactory optionalHandlers = OptionalHandlerFactory.instance;

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */
    
    /**
     * Stateless global singleton instance that should be used
     * for factories that want to use delegation to access
     * standard serializers.
     */
    public final static BasicSerializerFactory instance = new BasicSerializerFactory();

    /**
     * We will provide default constructor to allow sub-classing,
     * but make it protected so that no non-singleton instances of
     * the class will be instantiated.
     */
    protected BasicSerializerFactory() { }

    /*
    /**********************************************************
    /* SerializerFactory impl
    /**********************************************************
     */

    /**
     * Main serializer constructor method. The base implementation within
     * this class first calls a fast lookup method that can find serializers
     * for well-known JDK classes; and if that fails, a slower one that
     * tries to check out which interfaces given Class implements.
     * Sub-classes can (and do) change this behavior to alter behavior.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> JsonSerializer<T> createSerializer(Class<T> type, SerializationConfig config)
    {
        return (JsonSerializer<T>) createSerializer(TypeFactory.type(type), config);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> createSerializer(JavaType type, SerializationConfig config)
    {
        /* [JACKSON-220]: Very first thing, let's check annotations to
         * see if we have explicit definition
         */
        BasicBeanDescription beanDesc = config.introspect(type);
        JsonSerializer<?> ser = findSerializerFromAnnotation(config, beanDesc.getClassInfo());
        if (ser == null) {
            // First, fast lookup for exact type:
            ser = findSerializerByLookup(type, config, beanDesc);
            if (ser == null) {
                /* and should that fail, slower introspection methods; first
                 * one that deals with "primary" types
                 */
                ser = findSerializerByPrimaryType(type, config, beanDesc);
                if (ser == null) {
                    // And if that fails, one with "secondary" traits:
                    ser = findSerializerByAddonType(type, config, beanDesc);
                }
            }
        }
        return (JsonSerializer<Object>)ser;
    }

    /**
     * Method called to construct a type serializer for values with given declared
     * base type. This is called for values other than those of bean property
     * types.
     */
    @Override
    public TypeSerializer createTypeSerializer(JavaType baseType, SerializationConfig config)
    {
        BasicBeanDescription bean = config.introspectClassAnnotations(baseType.getRawClass());
        AnnotatedClass ac = bean.getClassInfo();
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findTypeResolver(ac, baseType);
        /* Ok: if there is no explicit type info handler, we may want to
         * use a default. If so, config object knows what to use.
         */
        Collection<NamedType> subtypes = null;
        if (b == null) {
            b = config.getDefaultTyper(baseType);
        } else {
            subtypes = config.getSubtypeResolver().collectAndResolveSubtypes(ac, config, ai);
        }
        return (b == null) ? null : b.buildTypeSerializer(baseType, subtypes);
    }

    
    /*
    /**********************************************************
    /* Additional API for other core classes
    /**********************************************************
     */

    public final JsonSerializer<?> getNullSerializer() {
        return NullSerializer.instance;
    }    

    /*
    /**********************************************************
    /* Overridable secondary serializer accessor methods
    /**********************************************************
     */

    /**
     * Fast lookup-based accessor method, which will only check for
     * type itself, but not consider super-classes or implemented
     * interfaces.
     */
    public final JsonSerializer<?> findSerializerByLookup(JavaType type, SerializationConfig config,
                                                          BasicBeanDescription beanDesc)
    {
        String clsName = type.getRawClass().getName();
        JsonSerializer<?> ser = _concrete.get(clsName);
        if (ser == null) {
            Class<? extends JsonSerializer<?>> serClass = _concreteLazy.get(clsName);
            if (serClass != null) {
                try {
                    ser = serClass.newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to instantiate standard serializer (of type "+serClass.getName()+"): "
                            +e.getMessage(), e);
                }
            }
        }
        
        /* 08-Nov-2009, tatus: Some standard types may need customization;
         *    for now that just means Maps, but in future probably other
         *    collections as well. For strictly standard types this is
         *    currently only needed due to mix-in annotations.
         */
        if (ser != null ) {
            if (ser == MapSerializer.instance) {
                return buildMapSerializer(type, config, beanDesc);
            }
            if (ser == ArraySerializers.ObjectArraySerializer.instance) {
                return buildObjectArraySerializer(type, config, beanDesc);
            }
            if (ser == ContainerSerializers.IndexedListSerializer.instance) {
                return buildIndexedListSerializer(type, config, beanDesc);
            }
            if (ser == ContainerSerializers.CollectionSerializer.instance) {
                return buildCollectionSerializer(type, config, beanDesc);
            }
        } else {
            // Then check for optional/external serializers [JACKSON-386]
            ser = optionalHandlers.findSerializer(type, config, beanDesc);
        }
        return ser;
    }

    /**
     * Reflection-based serialized find method, which checks if
     * given class is a sub-type of one of well-known classes, or implements
     * a "primary" interface. Primary here is defined as the main function
     * of the Object; as opposed to "add-on" functionality.
     */
    @SuppressWarnings("deprecation")
    public final JsonSerializer<?> findSerializerByPrimaryType(JavaType type, SerializationConfig config,
                                                          BasicBeanDescription beanDesc)
    {
        Class<?> cls = type.getRawClass();

        /* Some types are final, and hence not checked here (will
         * have been handled by fast method above):
         *
         * - Boolean
         * - String (StringBuffer, StringBuilder)
         * - Arrays for primitive types
         *
         * But we do need to check for
         *
         * - "primary" interfaces: Enum, Number, JsonSerializable
         * - Most collection types
         * - java.lang.Number (but is that integral or not?)
         */
        if (JsonSerializable.class.isAssignableFrom(cls)) {
            if (JsonSerializableWithType.class.isAssignableFrom(cls)) {
                return StdSerializers.SerializableWithTypeSerializer.instance;
            }
            return StdSerializers.SerializableSerializer.instance;
        }
        if (Map.class.isAssignableFrom(cls)) {
            if (EnumMap.class.isAssignableFrom(cls)) {
                return buildEnumMapSerializer(type, config, beanDesc);
            }
            return buildMapSerializer(type, config, beanDesc);
        }
        if (Object[].class.isAssignableFrom(cls)) {
            return buildObjectArraySerializer(type, config, beanDesc);
        }
        if (List.class.isAssignableFrom(cls)) {
            if (cls == List.class || cls == AbstractList.class || RandomAccess.class.isAssignableFrom(cls)) {
                return buildIndexedListSerializer(type, config, beanDesc);
            }
            return buildCollectionSerializer(type, config, beanDesc);
        }
        // [JACKSON-193]: consider @JsonValue for enum types (and basically any type), so:
        AnnotatedMethod valueMethod = beanDesc.findJsonValueMethod();
        if (valueMethod != null) {
            JsonSerializer<Object> ser = findSerializerFromAnnotation(config, valueMethod);
            return new JsonValueSerializer(valueMethod.getAnnotated(), ser);
            
        }
        
        if (Number.class.isAssignableFrom(cls)) {
            return StdSerializers.NumberSerializer.instance;
        }
        if (Enum.class.isAssignableFrom(cls)) {
            @SuppressWarnings("unchecked")
            Class<Enum<?>> enumClass = (Class<Enum<?>>) cls;
            return EnumSerializer.construct(enumClass, config, beanDesc);
        }
        if (Calendar.class.isAssignableFrom(cls)) {
            return StdSerializers.CalendarSerializer.instance;
        }
        if (java.util.Date.class.isAssignableFrom(cls)) {
            return StdSerializers.UtilDateSerializer.instance;
        }
        if (Collection.class.isAssignableFrom(cls)) {
            if (EnumSet.class.isAssignableFrom(cls)) {
                return buildEnumSetSerializer(type, config, beanDesc);
            }
            return buildCollectionSerializer(type, config, beanDesc);
        }
        return null;
    }
        
    /**
     * Reflection-based serialized find method, which checks if
     * given class implements one of recognized "add-on" interfaces.
     * Add-on here means a role that is usually or can be a secondary
     * trait: for example,
     * bean classes may implement {@link Iterable}, but their main
     * function is usually something else. The reason for
     */
    public final JsonSerializer<?> findSerializerByAddonType(JavaType javaType, SerializationConfig config,
                                                             BasicBeanDescription beanDesc)
    {
        Class<?> type = javaType.getRawClass();

        // These need to be in decreasing order of specificity...
        if (Iterator.class.isAssignableFrom(type)) {
            return buildIteratorSerializer(javaType, config, beanDesc);
        }
        if (Iterable.class.isAssignableFrom(type)) {
            return buildIterableSerializer(javaType, config, beanDesc);
        }
        if (CharSequence.class.isAssignableFrom(type)) {
            return ToStringSerializer.instance;
        }
        return null;
    }

    /**
     * Helper method called to check if a class or method
     * has an annotation
     * (@link org.codehaus.jackson.map.ser.JsonSerialize#using)
     * that tells the class to use for serialization.
     * Returns null if no such annotation found.
     */
    @SuppressWarnings("unchecked")
    protected JsonSerializer<Object> findSerializerFromAnnotation(SerializationConfig config, Annotated a)
    {
        Object serDef = config.getAnnotationIntrospector().findSerializer(a);
        if (serDef != null) {
            if (serDef instanceof JsonSerializer) {
                return (JsonSerializer<Object>) serDef;
            }
            /* Alas, there's no way to force return type of "either class
             * X or Y" -- need to throw an exception after the fact
             */
            if (!(serDef instanceof Class)) {
                throw new IllegalStateException("AnnotationIntrospector returned value of type "+serDef.getClass().getName()+"; expected type JsonSerializer or Class<JsonSerializer> instead");
            }
            Class<?> cls = (Class<?>) serDef;
            if (!JsonSerializer.class.isAssignableFrom(cls)) {
                throw new IllegalStateException("AnnotationIntrospector returned Class "+cls.getName()+"; expected Class<JsonSerializer>");
            }
            return (JsonSerializer<Object>) ClassUtil.createInstance(cls, config.isEnabled(SerializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS));
        }
        return null;
    }

    /**
     * Helper method that handles configuration details when constructing serializers for
     * {@link java.util.Map} types.
     */
    protected JsonSerializer<?> buildMapSerializer(JavaType type, SerializationConfig config,
                                                   BasicBeanDescription beanDesc)
    {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        JavaType valueType = type.getContentType();
        TypeSerializer vts = createTypeSerializer(valueType, config);
        boolean staticTyping = usesStaticTyping(config, beanDesc, vts);
        return MapSerializer.construct(intr.findPropertiesToIgnore(beanDesc.getClassInfo()),
                type, staticTyping, vts);
    }

    protected JsonSerializer<?> buildEnumMapSerializer(JavaType type, SerializationConfig config,
                                                   BasicBeanDescription beanDesc)
    {
        JavaType keyType = type.getKeyType();
        JavaType valueType = type.getContentType();
        // Need to find key enum values...
        EnumValues enums = null;
        if (keyType.isEnumType()) { // non-enum if we got it as type erased class (from instance)
            @SuppressWarnings("unchecked")
            Class<Enum<?>> enumClass = (Class<Enum<?>>) keyType.getRawClass();
            enums = EnumValues.construct(enumClass, config.getAnnotationIntrospector());
        }
        TypeSerializer vts = createTypeSerializer(valueType, config);
        return new EnumMapSerializer(valueType, usesStaticTyping(config, beanDesc, vts),
                enums, vts);
    }

    /**
     * Helper method that handles configuration details when constructing serializers for
     * <code>Object[]</code> (and subtypes).
     */
    protected JsonSerializer<?> buildObjectArraySerializer(JavaType type, SerializationConfig config,
                                                   BasicBeanDescription beanDesc)
    {
        JavaType valueType = type.getContentType();
        TypeSerializer vts = createTypeSerializer(valueType, config);
        return ArraySerializers.objectArraySerializer(valueType,
                usesStaticTyping(config, beanDesc, vts), vts);
    }

    protected JsonSerializer<?> buildIndexedListSerializer(JavaType type, SerializationConfig config,
            BasicBeanDescription beanDesc)
    {
        JavaType valueType = type.getContentType();
        TypeSerializer vts = createTypeSerializer(valueType, config);
        return ContainerSerializers.indexedListSerializer(valueType,
                usesStaticTyping(config, beanDesc, vts), vts);
    }

    protected JsonSerializer<?> buildCollectionSerializer(JavaType type, SerializationConfig config,
            BasicBeanDescription beanDesc)
    {
        JavaType valueType = type.getContentType();
        TypeSerializer vts = createTypeSerializer(valueType, config);
        return ContainerSerializers.collectionSerializer(valueType,
                usesStaticTyping(config, beanDesc, vts), vts);
    }

    protected JsonSerializer<?> buildIteratorSerializer(JavaType type, SerializationConfig config,
            BasicBeanDescription beanDesc)
    {
        // if there's generic type, it'll be the first contained type
        JavaType valueType = type.containedType(0);
        if (valueType == null) {
            valueType = TypeFactory.type(Object.class);
        }
        TypeSerializer vts = createTypeSerializer(valueType, config);
        return ContainerSerializers.iteratorSerializer(valueType,
                usesStaticTyping(config, beanDesc, vts), vts);
    }
    
    protected JsonSerializer<?> buildIterableSerializer(JavaType type, SerializationConfig config,
            BasicBeanDescription beanDesc)
    {
        // if there's generic type, it'll be the first contained type
        JavaType valueType = type.containedType(0);
        if (valueType == null) {
            valueType = TypeFactory.type(Object.class);
        }
        TypeSerializer vts = createTypeSerializer(valueType, config);
        return ContainerSerializers.iterableSerializer(valueType,
                usesStaticTyping(config, beanDesc, vts), vts);
    }

    protected JsonSerializer<?> buildEnumSetSerializer(JavaType type, SerializationConfig config,
                                                   BasicBeanDescription beanDesc)
    {
        // this may or may not be available (Class doesn't; type of field/method does)
        JavaType enumType = type.getContentType();
        // and even if nominally there is something, only use if it really is enum
        if (!enumType.isEnumType()) {
            enumType = null;
        }
        return ContainerSerializers.enumSetSerializer(enumType);
    }
    
    /**
     * Helper method to check whether global settings and/or class
     * annotations for the bean class indicate that static typing
     * (declared types)  should be used for properties.
     * (instead of dynamic runtime types).
     */
    protected boolean usesStaticTyping(SerializationConfig config,
                                       BasicBeanDescription beanDesc,
                                       TypeSerializer typeSer)
    {
        /* 16-Aug-2010, tatu: If there is a (value) type serializer, we can not force
         *    static typing; that would make it impossible to handle expected subtypes
         * 
         */
        if (typeSer != null) {
            return false;
        }
        JsonSerialize.Typing t = config.getAnnotationIntrospector().findSerializationTyping(beanDesc.getClassInfo());
        if (t != null) {
            return (t == JsonSerialize.Typing.STATIC);
        }
        return config.isEnabled(SerializationConfig.Feature.USE_STATIC_TYPING);
    }
}
