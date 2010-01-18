package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.jsontype.JsonTypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.map.util.EnumValues;
import org.codehaus.jackson.map.util.Provider;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSerializableSchema;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.TokenBuffer;

/**
 * Factory class that can provide serializers for standard JDK classes,
 * as well as custom classes that extend standard classes or implement
 * one of "well-known" interfaces (such as {@link java.util.Collection}).
 *<p>
 * Since all the serializers are eagerly instantiated, and there is
 * no additional introspection or customazibility of these types,
 * this factory is stateless. This means that other delegating
 * factories (or {@link SerializerProvider}s) can just use the
 * shared singleton instance via static {@link #instance} field.
 */
public class BasicSerializerFactory
    extends SerializerFactory
{
    /*
    ////////////////////////////////////////////////////////////
    // Configuration, lookup tables/maps
    ////////////////////////////////////////////////////////////
     */

    /**
     * Since these are all JDK classes, we shouldn't have to worry
     * about ClassLoader used to load them. Rather, we can just
     * use the class name, and keep things simple and efficient.
     */
    final static HashMap<String, JsonSerializer<?>> _concrete =
        new HashMap<String, JsonSerializer<?>>();
    /**
     * There are also standard interfaces and abstract classes
     * that we need to support without knowing conrecte implementation
     * classes.
     */
    final static ArrayList<SerializerMapping> _abstractSerializers =
        new ArrayList<SerializerMapping>();

    static {
        /* String and string-like types (note: date types explicitly
         * not included -- can use either textual or numeric serialization)
         */
        _concrete.put(String.class.getName(), new StringSerializer());
        final ToStringSerializer sls = ToStringSerializer.instance;
        _concrete.put(StringBuffer.class.getName(), sls);
        _concrete.put(StringBuilder.class.getName(), sls);
        _concrete.put(Character.class.getName(), sls);
        _concrete.put(Character.TYPE.getName(), sls);

        // Primitives/wrappers for primitives (primitives needed for Beans)
        _concrete.put(Boolean.TYPE.getName(), new BooleanSerializer(true));
        _concrete.put(Boolean.class.getName(), new BooleanSerializer(false));
        final IntegerSerializer intS = new IntegerSerializer();
        _concrete.put(Integer.class.getName(), intS);
        _concrete.put(Integer.TYPE.getName(), intS);
        _concrete.put(Long.class.getName(), LongSerializer.instance);
        _concrete.put(Long.TYPE.getName(), LongSerializer.instance);
        _concrete.put(Byte.class.getName(), IntLikeSerializer.instance);
        _concrete.put(Byte.TYPE.getName(), IntLikeSerializer.instance);
        _concrete.put(Short.class.getName(), IntLikeSerializer.instance);
        _concrete.put(Short.TYPE.getName(), IntLikeSerializer.instance);

        // Numbers, limited length floating point
        _concrete.put(Float.class.getName(), FloatSerializer.instance);
        _concrete.put(Float.TYPE.getName(), FloatSerializer.instance);
        _concrete.put(Double.class.getName(), DoubleSerializer.instance);
        _concrete.put(Double.TYPE.getName(), DoubleSerializer.instance);

        // Other numbers, more complicated
        final NumberSerializer ns = new NumberSerializer();
        _concrete.put(BigInteger.class.getName(), ns);
        _concrete.put(BigDecimal.class.getName(), ns);

        /* Other discrete non-container types:
         * first, Date/Time zoo:
         */
        _concrete.put(Calendar.class.getName(), CalendarSerializer.instance);
        _concrete.put(java.util.Date.class.getName(), UtilDateSerializer.instance);
        _concrete.put(java.sql.Date.class.getName(), new SqlDateSerializer());
        _concrete.put(java.sql.Time.class.getName(), new SqlTimeSerializer());
        // note: timestamps are very similar to java.util.Date, thus serialized as such
        _concrete.put(java.sql.Timestamp.class.getName(), UtilDateSerializer.instance);

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
        for (Map.Entry<Class<?>,JsonSerializer<?>> en : new JdkSerializers().provide()) {
            _concrete.put(en.getKey().getName(), en.getValue());
        }

        // And finally, one specific Jackson type
        // (Q: can this ever be sub-classes?)
        _concrete.put(TokenBuffer.class.getName(), new TokenBufferSerializer());
    }

    static {
        /* 21-Nov-2009, tatu: Also, explicit support for basic Joda DateTime;
         *    and can use same mechanism for javax.xml.datatype types as well.
         */
        for (String provStr : new String[] {
            "org.codehaus.jackson.map.ext.CoreXMLSerializers"
            ,"org.codehaus.jackson.map.ext.JodaSerializers"
        }) {
            Object ob = null;
            try {
                ob = Class.forName(provStr).newInstance();
            }
            catch (LinkageError e) { }
            // too many different kinds to enumerate here:
            catch (Exception e) { }

            if (ob != null) {
                @SuppressWarnings("unchecked")
                    Provider<Map.Entry<Class<?>,JsonSerializer<?>>> prov =
                    (Provider<Map.Entry<Class<?>,JsonSerializer<?>>>) ob;
                for (Map.Entry<Class<?>,JsonSerializer<?>> en : prov.provide()) {
                    /* 22-Nov-2009, tatu: For now this check suffices... may need
                     *   to use other methods in future
                     */
                    Class<?> cls = en.getKey();
                    if (ClassUtil.isConcrete(cls)) {
                        _concrete.put(en.getKey().getName(), en.getValue());
                    } else {
                        _abstractSerializers.add(new SerializerMapping(cls, en.getValue()));
                    }
                }
            }
        }
    }
    
    /*
    ////////////////////////////////////////////////////////////
    // Life cycle
    ////////////////////////////////////////////////////////////
     */

    /**
     * Stateless global singleton instance that should be used
     * for factories that want to use delegation to access
     * standard serializers.
     */
    public final static BasicSerializerFactory instance = new BasicSerializerFactory();

    /*
    ////////////////////////////////////////////////////////////
    // Life cycle
    ////////////////////////////////////////////////////////////
     */

    /**
     * We will provide default constructor to allow sub-classing,
     * but make it protected so that no non-singleton instances of
     * the class will be instantiated.
     */
    protected BasicSerializerFactory() { }

    /*
    ////////////////////////////////////////////////////////////
    // JsonSerializerFactory impl
    ////////////////////////////////////////////////////////////
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
        BasicBeanDescription beanDesc = config.introspect(type.getRawClass());
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

    public TypeSerializer createTypeSerializer(Class<?> baseType, SerializationConfig config)
    {
        BasicBeanDescription bean = config.introspectClassAnnotations(baseType);
        AnnotatedClass ac = bean.getClassInfo();
        JsonTypeResolverBuilder<?> b = config.getAnnotationIntrospector().findTypeResolver(ac, baseType);
        return (b == null) ? null : b.buildTypeSerializer();
    }
    
    /*
    ////////////////////////////////////////////////////////////
    // Other public methods
    ////////////////////////////////////////////////////////////
     */

    public final JsonSerializer<?> getNullSerializer() {
        return NullSerializer.instance;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Overridable secondary serializer accessor methods
    ////////////////////////////////////////////////////////////
     */

    /**
     * Fast lookup-based accessor method, which will only check for
     * type itself, but not consider super-classes or implemented
     * interfaces.
     */
    public final JsonSerializer<?> findSerializerByLookup(JavaType type, SerializationConfig config,
                                                          BasicBeanDescription beanDesc)
    {
        JsonSerializer<?> ser = _concrete.get(type.getRawClass().getName());
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
        }
        return ser;
    }

    /**
     * Reflection-based serialized find method, which checks if
     * given class is a sub-type of one of well-known classes, or implements
     * a "primary" interface. Primary here is defined as the main function
     * of the Object; as opposed to "add-on" functionality.
     */
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
            return SerializableSerializer.instance;
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
            if (RandomAccess.class.isAssignableFrom(cls)) {
                return buildIndexedListSerializer(type, config, beanDesc);
            }
            return buildCollectionSerializer(type, config, beanDesc);
        }
        if (Number.class.isAssignableFrom(cls)) {
            return NumberSerializer.instance;
        }
        if (Enum.class.isAssignableFrom(cls)) {
            @SuppressWarnings("unchecked")
            Class<Enum<?>> enumClass = (Class<Enum<?>>) cls;
            return EnumSerializer.construct(enumClass, config.getAnnotationIntrospector());
        }
        if (Calendar.class.isAssignableFrom(cls)) {
            return CalendarSerializer.instance;
        }
        if (java.util.Date.class.isAssignableFrom(cls)) {
            return UtilDateSerializer.instance;
        }
        for (int i = 0, len = _abstractSerializers.size(); i < len; ++i) {
            SerializerMapping map = _abstractSerializers.get(i);
            if (map.matches(cls)) {
                return map.getSerializer();
            }
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
        boolean staticTyping = usesStaticTyping(config, beanDesc);
        JavaType valueType = type.getContentType();
        TypeSerializer typeSer = createTypeSerializer(valueType.getRawClass(), config);
        if (typeSer != null) {
            return MapSerializer.constructTyped(intr.findPropertiesToIgnore(beanDesc.getClassInfo()),
                type, staticTyping, typeSer);
        }
        return MapSerializer.constructNonTyped(intr.findPropertiesToIgnore(beanDesc.getClassInfo()),
                type, staticTyping);
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
        return ContainerSerializers.enumMapSerializer(valueType,
                usesStaticTyping(config, beanDesc), createTypeSerializer(valueType.getRawClass(), config),
                enums);
    }

    /**
     * Helper method that handles configuration details when constructing serializers for
     * <code>Object[]</code> (and subtypes).
     */
    protected JsonSerializer<?> buildObjectArraySerializer(JavaType type, SerializationConfig config,
                                                   BasicBeanDescription beanDesc)
    {
        JavaType valueType = type.getContentType();
        boolean staticTyping = usesStaticTyping(config, beanDesc);
        TypeSerializer typeSer = createTypeSerializer(valueType.getRawClass(), config);
        return ArraySerializers.objectArraySerializer(valueType, staticTyping, typeSer);
    }

    protected JsonSerializer<?> buildIndexedListSerializer(JavaType type, SerializationConfig config,
            BasicBeanDescription beanDesc)
    {
        JavaType elemType = type.getContentType();
        return ContainerSerializers.indexedListSerializer(elemType,
                usesStaticTyping(config, beanDesc), createTypeSerializer(elemType.getRawClass(), config));
    }

    protected JsonSerializer<?> buildCollectionSerializer(JavaType type, SerializationConfig config,
            BasicBeanDescription beanDesc)
    {
        JavaType elemType = type.getContentType();
        return ContainerSerializers.collectionSerializer(elemType,
                usesStaticTyping(config, beanDesc), createTypeSerializer(elemType.getRawClass(), config));
    }

    protected JsonSerializer<?> buildIteratorSerializer(JavaType type, SerializationConfig config,
            BasicBeanDescription beanDesc)
    {
        // if there's generic type, it'll be the first contained type
        JavaType elemType = type.containedType(0);
        TypeSerializer typeSer = (elemType == null) ? null : createTypeSerializer(elemType.getRawClass(), config);
        return ContainerSerializers.iteratorSerializer(elemType,
                usesStaticTyping(config, beanDesc), typeSer);
    }
    
    protected JsonSerializer<?> buildIterableSerializer(JavaType type, SerializationConfig config,
            BasicBeanDescription beanDesc)
    {
        // if there's generic type, it'll be the first contained type
        JavaType elemType = type.containedType(0);
        TypeSerializer typeSer = (elemType == null) ? null : createTypeSerializer(elemType.getRawClass(), config);
        return ContainerSerializers.iterableSerializer(elemType,
                usesStaticTyping(config, beanDesc), typeSer);
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
                                       BasicBeanDescription beanDesc)
    {
        JsonSerialize.Typing t = config.getAnnotationIntrospector().findSerializationTyping(beanDesc.getClassInfo());
        if (t != null) {
            return (t == JsonSerialize.Typing.STATIC);
        }
        return config.isEnabled(SerializationConfig.Feature.USE_STATIC_TYPING);
    }
    
    /*
    /////////////////////////////////////////////////////////////////
    // Helper classes
    /////////////////////////////////////////////////////////////////
     */

    private final static class SerializerMapping
    {
        final Class<?> _class;
        final JsonSerializer<?> _serializer;

        public SerializerMapping(Class<?> c, JsonSerializer<?> ser) {
            _class = c;
            _serializer = ser;
        }

        public boolean matches(Class<?> c) {
            return _class.isAssignableFrom(c);
        }

        public JsonSerializer<?> getSerializer() { return _serializer; }
    }

    /*
    /////////////////////////////////////////////////////////////////
    // Concrete serializers, non-numeric primitives, Strings, Classes
    /////////////////////////////////////////////////////////////////
     */

    public final static class BooleanSerializer
        extends SerializerBase<Boolean>
    {
        /**
         * Whether type serialized is primitive (boolean) or wrapper
         * (java.lang.Boolean); if true, former, if false, latter.
         */
        final boolean _forPrimitive;

        public BooleanSerializer(boolean forPrimitive)
        {
            super(Boolean.class);
            _forPrimitive = forPrimitive;
        }

        @Override
        public void serialize(Boolean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBoolean(value.booleanValue());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            /*(ryan) it may not, in fact, be optional, but there's no way
             * to tell whether we're referencing a boolean or java.lang.Boolean.
             */
            /* 27-Jun-2009, tatu: Now we can tell, after passing
             *   'forPrimitive' flag...
             */
            return createSchemaNode("boolean", !_forPrimitive);
        }
    }

    /**
     * This is the special serializer for regular {@link java.lang.String}s.
     */
    public final static class StringSerializer
        extends SerializerBase<String>
    {
        public StringSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("string", true);
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, numerics
    ////////////////////////////////////////////////////////////
     */

    public final static class IntegerSerializer
        extends SerializerBase<Integer>
    {
        public IntegerSerializer() { super(Integer.class); }

        @Override
        public void serialize(Integer value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.intValue());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
                throws JsonMappingException
        {
            return createSchemaNode("integer", true);
        }
    }

    /**
     * Similar to {@link IntegerSerializer}, but will not cast to Integer:
     * instead, cast is to {@link java.lang.Number}, and conversion is
     * by calling {@link java.lang.Number#intValue}.
     */
    public final static class IntLikeSerializer
        extends SerializerBase<Number>
    {
        final static IntLikeSerializer instance = new IntLikeSerializer();

        public IntLikeSerializer() { super(Number.class); }
        
        @Override
        public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.intValue());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
                throws JsonMappingException
        {
            return createSchemaNode("integer", true);
        }
    }

    public final static class LongSerializer
        extends SerializerBase<Long>
    {
        final static LongSerializer instance = new LongSerializer();

        public LongSerializer() { super(Long.class); }
        
        @Override
        public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.longValue());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
    }

    public final static class FloatSerializer
        extends SerializerBase<Float>
    {
        final static FloatSerializer instance = new FloatSerializer();

        public FloatSerializer() { super(Float.class); }
        
        @Override
        public void serialize(Float value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.floatValue());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
    }

    public final static class DoubleSerializer
        extends SerializerBase<Double>
    {
        final static DoubleSerializer instance = new DoubleSerializer();

        public DoubleSerializer() { super(Double.class); }

        @Override
        public void serialize(Double value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.doubleValue());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
    }

    /**
     * As a fallback, we may need to use this serializer for other
     * types of {@link Number}s (custom types).
     */
    public final static class NumberSerializer
        extends SerializerBase<Number>
    {
        public final static NumberSerializer instance = new NumberSerializer();

        public NumberSerializer() { super(Number.class); }

        @Override
        public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            /* These shouldn't match (as there are more specific ones),
             * but just to be sure:
             */
            if (value instanceof Double) {
                jgen.writeNumber(((Double) value).doubleValue());
            } else if (value instanceof Float) {
                jgen.writeNumber(((Float) value).floatValue());
            } else {
                // We'll have to use fallback "untyped" number write method
                jgen.writeNumber(value.toString());
            }
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Serializers for JDK date datatypes
    ////////////////////////////////////////////////////////////
     */

    /**
     * For time values we should use timestamp, since that is about the only
     * thing that can be reliably converted between date-based objects
     * and json.
     */
    public final static class CalendarSerializer
        extends SerializerBase<Calendar>
    {
        public final static CalendarSerializer instance = new CalendarSerializer();

        public CalendarSerializer() { super(Calendar.class); }
        
        @Override
        public void serialize(Calendar value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            provider.defaultSerializeDateValue(value.getTimeInMillis(), jgen);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            //TODO: (ryan) add a format for the date in the schema?
            return createSchemaNode(provider.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)
                    ? "number" : "string", true);
        }
    }

    /**
     * For efficiency, we will serialize Dates as longs, instead of
     * potentially more readable Strings.
     */
    public final static class UtilDateSerializer
        extends SerializerBase<java.util.Date>
    {
        public final static UtilDateSerializer instance = new UtilDateSerializer();

        public UtilDateSerializer() { super(java.util.Date.class); }

        @Override
        public void serialize(java.util.Date value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            provider.defaultSerializeDateValue(value, jgen);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
                throws JsonMappingException
        {
            //todo: (ryan) add a format for the date in the schema?
            return createSchemaNode(provider.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)
                    ? "number" : "string", true);
        }
    }

    /**
     * Compared to regular {@link UtilDateSerializer}, we do use String
     * representation here. Why? Basically to truncate of time part, since
     * that should not be used by plain SQL date.
     */
    public final static class SqlDateSerializer
        extends SerializerBase<java.sql.Date>
    {
        public SqlDateSerializer() { super(java.sql.Date.class); }

        @Override
        public void serialize(java.sql.Date value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.toString());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            //todo: (ryan) add a format for the date in the schema?
            return createSchemaNode("string", true);
        }
    }

    public final static class SqlTimeSerializer
        extends SerializerBase<java.sql.Time>
    {
        public SqlTimeSerializer() { super(java.sql.Time.class); }

        @Override
        public void serialize(java.sql.Time value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.toString());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("string", true);
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Other serializers
    ////////////////////////////////////////////////////////////
     */

    /**
     * Generic handler for types that implement {@link JsonSerializable}.
     *<p>
     * Note: given that this is used for anything that implements
     * interface, can not be checked for direct class equivalence.
     */
    public final static class SerializableSerializer
        extends SerializerBase<JsonSerializable>
    {
        final static SerializableSerializer instance = new SerializableSerializer();

        private SerializableSerializer() { super(JsonSerializable.class); }

        @Override
        public void serialize(JsonSerializable value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            value.serialize(jgen, provider);
        }
        
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            ObjectNode objectNode = createObjectNode();
            String schemaType = "any";
            String objectProperties = null;
            String itemDefinition = null;
            if (typeHint != null) {
                Class<?> rawClass = TypeFactory.type(typeHint).getRawClass();
                if (rawClass.isAnnotationPresent(JsonSerializableSchema.class)) {
                    JsonSerializableSchema schemaInfo = rawClass.getAnnotation(JsonSerializableSchema.class);
                    schemaType = schemaInfo.schemaType();
                    if (!"##irrelevant".equals(schemaInfo.schemaObjectPropertiesDefinition())) {
                        objectProperties = schemaInfo.schemaObjectPropertiesDefinition();
                    }
                    if (!"##irrelevant".equals(schemaInfo.schemaItemDefinition())) {
                        itemDefinition = schemaInfo.schemaItemDefinition();
                    }
                }
            }
            objectNode.put("type", schemaType);
            if (objectProperties != null) {
                try {
                    objectNode.put("properties", new ObjectMapper().readValue(objectProperties, JsonNode.class));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            if (itemDefinition != null) {
                try {
                    objectNode.put("items", new ObjectMapper().readValue(itemDefinition, JsonNode.class));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            objectNode.put("optional", true);
            return objectNode;
        }
    }

    /**
     * We also want to directly support serialization of {@link TokenBuffer};
     * and since it is part of core package, it can not implement
     * {@link JsonSerializable} (which is only included in the mapper
     * package)
     *
     * @since 1.5
     */
    public final static class TokenBufferSerializer
        extends SerializerBase<TokenBuffer>
    {
        public TokenBufferSerializer() { super(TokenBuffer.class); }

        @Override
        public void serialize(TokenBuffer value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            value.serialize(jgen);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            /* 01-Jan-2010, tatu: Not 100% sure what we should say here:
             *   type is basically not known. This seems closest
             *   approximation
             */
            return createSchemaNode("any", true);
        }
    }
}
