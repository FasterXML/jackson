package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonUseSerializer;
import org.codehaus.jackson.annotate.NoClass;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;

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
    static {
        /* String and string-like types (note: date types explicitly
         * not included -- can use either textual or numeric serialization)
         */
        _concrete.put(String.class.getName(), new StringSerializer());
        ToStringSerializer sls = ToStringSerializer.instance;
        _concrete.put(StringBuffer.class.getName(), sls);
        _concrete.put(StringBuilder.class.getName(), sls);
        _concrete.put(Character.class.getName(), sls);
        _concrete.put(Character.TYPE.getName(), sls);
        // currency units best dealt with as strings too
        _concrete.put(Currency.class.getName(), sls);
        // including things best serialized as Strings
        _concrete.put(UUID.class.getName(), sls);
        
        // Primitives/wrappers for primitives (primitives needed for Beans)
        _concrete.put(Boolean.class.getName(), BooleanSerializer.instance);
        _concrete.put(Boolean.TYPE.getName(), BooleanSerializer.instance);
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
        // not sure if this is exactly right (should use toXMLFormat()?) but:
        /* 19-Jan-2009, tatu: [JACSKON-37]: This is something Android platform doesn't have
         *    so need to hard-code name (it is available on standard JDK 1.5 and above)
         */
        _concrete.put("javax.xml.datatype.XMLGregorianCalendar", ToStringSerializer.instance);

        /* Reference types, URLs, URIs
         */
        _concrete.put(java.net.URL.class.getName(), ToStringSerializer.instance);
        _concrete.put(java.net.URI.class.getName(), ToStringSerializer.instance);

        // Class.class
        _concrete.put(Class.class.getName(), new ClassSerializer());

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

        final ContainerSerializers.MapSerializer mapS = ContainerSerializers.MapSerializer.instance;
        _concrete.put(HashMap.class.getName(), mapS);
        _concrete.put(Hashtable.class.getName(), mapS);
        _concrete.put(LinkedHashMap.class.getName(), mapS);
        _concrete.put(TreeMap.class.getName(), mapS);
        _concrete.put(Properties.class.getName(), mapS);

        _concrete.put(HashSet.class.getName(), collectionS);
        _concrete.put(LinkedHashSet.class.getName(), collectionS);
        _concrete.put(TreeSet.class.getName(), collectionS);

        // and Enum-variations of set/map
        _concrete.put(EnumMap.class.getName(), new ContainerSerializers.EnumMapSerializer());
        _concrete.put(EnumSet.class.getName(), new ContainerSerializers.EnumSetSerializer());

        /* Finally, couple of oddball types. Not sure if these are
         * really needed...
         */
        final NullSerializer nullS = NullSerializer.instance;
        _concrete.put(Void.TYPE.getName(), nullS);
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
        // First, fast lookup for exact type:
        JsonSerializer<?> ser = findSerializerByLookup(type);
        if (ser == null) {
            /* and should that fail, slower introspection methods; first
             * one that deals with "primary" types
             */
            ser = findSerializerByPrimaryType(type, config);
            if (ser == null) {
                // And if that fails, one with "secondary" traits:
                ser = findSerializerByAddonType(type);
            }
        }
        return (JsonSerializer<T>) ser;
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
    public final JsonSerializer<?> findSerializerByLookup(Class<?> type)
    {
        return _concrete.get(type.getName());
    }

    /**
     * Reflection-based serialized find method, which checks if
     * given class is a sub-type of one of well-known classes, or implements
     * a "primary" interface. Primary here is defined as the main function
     * of the Object; as opposed to "add-on" functionality.
     */
    public final JsonSerializer<?> findSerializerByPrimaryType(Class<?> type, SerializationConfig config)
    {
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
        if (JsonSerializable.class.isAssignableFrom(type)) {
            return SerializableSerializer.instance;
        }
        if (Map.class.isAssignableFrom(type)) {
            return ContainerSerializers.MapSerializer.instance;
        }
        if (Object[].class.isAssignableFrom(type)) {
            return ArraySerializers.ObjectArraySerializer.instance;
        }
        if (List.class.isAssignableFrom(type)) {
            if (RandomAccess.class.isAssignableFrom(type)) {
                return ContainerSerializers.IndexedListSerializer.instance;
            }
            return ContainerSerializers.CollectionSerializer.instance;
        }
        if (Number.class.isAssignableFrom(type)) {
            return NumberSerializer.instance;
        }
        if (Enum.class.isAssignableFrom(type)) {
            /* 18-Feb-2009, tatu: Sort of related to [JACKSON-58], it
             *   was found out that annotations do not work with
             *   Enum classes.
             */
            BasicBeanDescription desc = config.introspectClassAnnotations(type);
            JsonSerializer<Object> ser = findSerializerFromAnnotation(config, desc.getClassInfo());
            if (ser != null) {
                return ser;
            }
            return new EnumSerializer();
        }
        if (Calendar.class.isAssignableFrom(type)) {
            return CalendarSerializer.instance;
        }
        if (java.util.Date.class.isAssignableFrom(type)) {
            return UtilDateSerializer.instance;
        }
        if (Collection.class.isAssignableFrom(type)) {
            return ContainerSerializers.CollectionSerializer.instance;
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
    public final JsonSerializer<?> findSerializerByAddonType(Class<?> type)
    {
        // These need to be in decreasing order of specificity...
        if (Iterator.class.isAssignableFrom(type)) {
            return ContainerSerializers.IteratorSerializer.instance;
        }
        if (Iterable.class.isAssignableFrom(type)) {
            return ContainerSerializers.IterableSerializer.instance;
        }
        if (CharSequence.class.isAssignableFrom(type)) {
            return ToStringSerializer.instance;
        }
        return null;
    }

    /**
     * Helper method called to check if a class or method
     * has {@link JsonUseSerializer} annotation which tells the
     * class to use for serialization.
     * Returns null if no such annotation found.
     */
    protected JsonSerializer<Object> findSerializerFromAnnotation(SerializationConfig config, Annotated a)
    {
        // Must be of proper type, of course
        if (!JsonSerializer.class.isAssignableFrom(serClass)) {
            throw new IllegalArgumentException("Invalid @JsonSerializer annotation for "+a.getName()+": value ("+serClass.getName()+") does not implement JsonSerializer interface");
        }
        try {
            Object ob = serClass.newInstance();
            @SuppressWarnings("unchecked")
                JsonSerializer<Object> ser = (JsonSerializer<Object>) ob;
            return ser;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate "+serClass.getName()+" to use as serializer for "+a.getName()+", problem: "+e.getMessage(), e);
        }
    }

    /*
    /////////////////////////////////////////////////////////////////
    // Concrete serializers, non-numeric primitives, Strings, Classes
    /////////////////////////////////////////////////////////////////
     */

    public final static class BooleanSerializer
        extends JsonSerializer<Boolean>
    {
        final static BooleanSerializer instance = new BooleanSerializer();

        @Override
		public void serialize(Boolean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBoolean(value.booleanValue());
        }
    }

    /**
     * This is the special serializer for regular {@link java.lang.String}s.
     */
    public final static class StringSerializer
        extends JsonSerializer<String>
    {
        @Override
            public void serialize(String value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value);
        }
    }

    /**
     * Deprecated serializer, identical to {@link ToStringSerializer}.
     *
     * @deprecated Use {@link ToStringSerializer} instead (stand-along class,
     *   more accurate name)
     */
    @Deprecated
    public final static class StringLikeSerializer<T>
        extends JsonSerializer<T>
    {
        public final static StringLikeSerializer<Object> instance = new StringLikeSerializer<Object>();

        /* 17-Feb-2009, tatus: better ensure there is the no-arg constructor,
         *   so it can be used via annotations
         */
        public StringLikeSerializer() { }

        @Override
        public void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.toString());
        }
    }

    /**
     * Also: default bean access will not do much good with Class.class. But
     * we can just store the name.
     */
    public final static class ClassSerializer
        extends JsonSerializer<Class<?>>
    {
        @Override
        public void serialize(Class<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.getName());
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, numerics
    ////////////////////////////////////////////////////////////
     */

    public final static class IntegerSerializer
        extends JsonSerializer<Integer>
    {
        @Override
            public void serialize(Integer value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.intValue());
        }
    }

    /**
     * Similar to {@link IntegerSerializer}, but will not cast to Integer:
     * instead, cast is to {@link java.lang.Number}, and conversion is
     * by calling {@link java.lang.Number#intValue}.
     */
    public final static class IntLikeSerializer
        extends JsonSerializer<Number>
    {
        final static IntLikeSerializer instance = new IntLikeSerializer();

        @Override
            public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.intValue());
        }
    }

    public final static class LongSerializer
        extends JsonSerializer<Long>
    {
        final static LongSerializer instance = new LongSerializer();

        @Override
		public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.longValue());
        }
    }

    public final static class FloatSerializer
        extends JsonSerializer<Float>
    {
        final static FloatSerializer instance = new FloatSerializer();

        @Override
		public void serialize(Float value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.floatValue());
        }
    }

    public final static class DoubleSerializer
        extends JsonSerializer<Double>
    {
        final static DoubleSerializer instance = new DoubleSerializer();

        @Override
		public void serialize(Double value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.doubleValue());
        }
    }

    /**
     * As a fallback, we may need to use this serializer for other
     * types of {@link Number}s (custom types).
     */
    public final static class NumberSerializer
        extends JsonSerializer<Number>
    {
        public final static NumberSerializer instance = new NumberSerializer();

        @Override
		public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            // We'll have to use fallback "untyped" number write method
            jgen.writeNumber(value.toString());
        }
    }


    /*
    ////////////////////////////////////////////////////////////
    // Other odd-ball special-purpose serializers
    ////////////////////////////////////////////////////////////
     */

    public final static class EnumSerializer
        extends JsonSerializer<Enum<?>>
    {
        @Override
            public void serialize(Enum<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.name());
        }
    }

    /**
     * For time values we should use timestamp, since that is about the only
     * thing that can be reliably converted between date-based objects
     * and json.
     */
    public final static class CalendarSerializer
        extends JsonSerializer<Calendar>
    {
        public final static CalendarSerializer instance = new CalendarSerializer();
        @Override
		public void serialize(Calendar value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            provider.defaultSerializeDateValue(value.getTimeInMillis(), jgen);
        }
    }

    /**
     * For efficiency, we will serialize Dates as longs, instead of
     * potentially more readable Strings.
     */
    public final static class UtilDateSerializer
        extends JsonSerializer<java.util.Date>
    {
        public final static UtilDateSerializer instance = new UtilDateSerializer();
        @Override
        public void serialize(java.util.Date value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            provider.defaultSerializeDateValue(value, jgen);
        }
    }

    /**
     * Compared to regular {@link UtilDateSerializer}, we do use String
     * representation here. Why? Basically to truncate of time part, since
     * that should not be used by plain SQL date.
     */
    public final static class SqlDateSerializer
        extends JsonSerializer<java.sql.Date>
    {
        @Override
		public void serialize(java.sql.Date value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.toString());
        }
    }

    public final static class SqlTimeSerializer
        extends JsonSerializer<java.sql.Time>
    {
        @Override
            public void serialize(java.sql.Time value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.toString());
        }
    }

    /**
     * To allow for special handling for null values (in Objects, Arrays,
     * root-level), handling for nulls is done via serializers too.
     * This is the default serializer for nulls.
     */
    public final static class NullSerializer
        extends JsonSerializer<Object>
    {
        public final static NullSerializer instance = new NullSerializer();

        private NullSerializer() { }

        @Override
            public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNull();
        }
    }

    public final static class SerializableSerializer
        extends JsonSerializer<JsonSerializable>
    {
        final static SerializableSerializer instance = new SerializableSerializer();

        private SerializableSerializer() { }

        @Override
		public void serialize(JsonSerializable value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            value.serialize(jgen, provider);
        }
    }
}
