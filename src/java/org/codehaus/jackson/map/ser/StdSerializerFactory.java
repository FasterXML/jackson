package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializable;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.SerializerProvider;

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
public class StdSerializerFactory
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
        _concrete.put("javax.xml.datatype.XMLGregorianCalendar", StringLikeSerializer.instance);

        /* Reference types, URLs, URIs
         */
        _concrete.put(java.net.URL.class.getName(), StringLikeSerializer.instance);
        _concrete.put(java.net.URI.class.getName(), StringLikeSerializer.instance);

        // Arrays of various types (including common object types)
        _concrete.put(boolean[].class.getName(), new BooleanArraySerializer());
        _concrete.put(byte[].class.getName(), new ByteArraySerializer());
        _concrete.put(char[].class.getName(), new CharArraySerializer());
        _concrete.put(short[].class.getName(), new ShortArraySerializer());
        _concrete.put(int[].class.getName(), new IntArraySerializer());
        _concrete.put(long[].class.getName(), new LongArraySerializer());
        _concrete.put(float[].class.getName(), new FloatArraySerializer());
        _concrete.put(double[].class.getName(), new DoubleArraySerializer());

        _concrete.put(Object[].class.getName(), ObjectArraySerializer.instance);
        _concrete.put(String[].class.getName(), new StringArraySerializer());

        // And then Java Collection classes
        final IndexedListSerializer indListS = IndexedListSerializer.instance;
        final CollectionSerializer collectionS = CollectionSerializer.instance;

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

        // and Enum-variations of set/map
        _concrete.put(EnumMap.class.getName(), new EnumMapSerializer());
        _concrete.put(EnumSet.class.getName(), new EnumSetSerializer());

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
    public final static StdSerializerFactory instance = new StdSerializerFactory();

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
    protected StdSerializerFactory() { }

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
    public <T> JsonSerializer<T> createSerializer(Class<T> type)
    {
        // First, fast lookup for exact type:
        JsonSerializer<?> ser = findSerializerByLookup(type);
        if (ser == null) {
            /* and should that fail, slower introspection methods; first
             * one that deals with "primary" types
             */
            ser = findSerializerByPrimaryType(type);
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
    public final JsonSerializer<?> findSerializerByPrimaryType(Class<?> type)
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
            return MapSerializer.instance;
        }
        if (Object[].class.isAssignableFrom(type)) {
            return ObjectArraySerializer.instance;
        }
        if (List.class.isAssignableFrom(type)) {
            if (RandomAccess.class.isAssignableFrom(type)) {
                return IndexedListSerializer.instance;
            }
            return CollectionSerializer.instance;
        }
        if (Number.class.isAssignableFrom(type)) {
            return NumberSerializer.instance;
        }
        if (Enum.class.isAssignableFrom(type)) {
            return new EnumSerializer();
        }
        if (Calendar.class.isAssignableFrom(type)) {
            return CalendarSerializer.instance;
        }
        if (java.util.Date.class.isAssignableFrom(type)) {
            return UtilDateSerializer.instance;
        }
        if (Collection.class.isAssignableFrom(type)) {
            return CollectionSerializer.instance;
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
            return new IteratorSerializer();
        }
        if (Iterable.class.isAssignableFrom(type)) {
            return new IterableSerializer();
        }
        if (CharSequence.class.isAssignableFrom(type)) {
            return StringLikeSerializer.instance;
        }
        return null;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, non-numeric primitives, Strings
    ////////////////////////////////////////////////////////////
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
     * This is an interesting general purpose serializer, useful for any
     * type for which {@link Object#toString} returns the desired Json
     * value.
     */
    public final static class StringLikeSerializer
        extends JsonSerializer<Object>
    {
        public final static StringLikeSerializer instance = new StringLikeSerializer();

        @Override
		public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.toString());
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
    // Concrete serializers, Lists/collections
    ////////////////////////////////////////////////////////////
     */

    /**
     * This is an optimizied serializer for Lists that can be efficiently
     * traversed by index (as opposed to others, such as {@link LinkedList}
     * that can not}.
     */
    public final static class IndexedListSerializer
        extends JsonSerializer<List<?>>
    {
        public final static IndexedListSerializer instance = new IndexedListSerializer();

        @Override
		public void serialize(List<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();

            final int len = value.size();

            if (len > 0) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                for (int i = 0; i < len; ++i) {
                    Object elem = value.get(i);
                    if (elem == null) {
                        provider.getNullValueSerializer().serialize(null, jgen, provider);
                    } else {
                        // Minor optimization to avoid most lookups:
                        Class<?> cc = elem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevClass) {
                            currSerializer = prevSerializer;
                        } else {
                            currSerializer = provider.findValueSerializer(cc);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                }
            }

            jgen.writeEndArray();
        }
    }

    /**
     * Fallback serializer for cases where Collection is not known to be
     * of type for which more specializer serializer exists (such as
     * index-accessible List).
     * If so, we will just construct an {@link java.util.Iterator}
     * to iterate over elements.
     */
    public final static class CollectionSerializer
        extends JsonSerializer<Collection<?>>
    {
        public final static CollectionSerializer instance = new CollectionSerializer();

        @Override
		public void serialize(Collection<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();

            Iterator<?> it = value.iterator();
            if (it.hasNext()) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;

                do {
                    Object elem = it.next();
                    if (elem == null) {
                        provider.getNullValueSerializer().serialize(null, jgen, provider);
                    } else {
                        // Minor optimization to avoid most lookups:
                        Class<?> cc = elem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevClass) {
                            currSerializer = prevSerializer;
                        } else {
                            currSerializer = provider.findValueSerializer(cc);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                } while (it.hasNext());
            }

            jgen.writeEndArray();
        }
    }

    public final static class IteratorSerializer
        extends JsonSerializer<Iterator<?>>
    {
        @Override
		public void serialize(Iterator<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            if (value.hasNext()) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                do {
                    Object elem = value.next();
                    if (elem == null) {
                        provider.getNullValueSerializer().serialize(null, jgen, provider);
                    } else {
                        // Minor optimization to avoid most lookups:
                        Class<?> cc = elem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevClass) {
                            currSerializer = prevSerializer;
                        } else {
                            currSerializer = provider.findValueSerializer(cc);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                } while (value.hasNext());
            }
            jgen.writeEndArray();
        }
    }

    public final static class IterableSerializer
        extends JsonSerializer<Iterable<?>>
    {
        @Override
		public void serialize(Iterable<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            Iterator<?> it = value.iterator();
            if (it.hasNext()) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                do {
                    Object elem = it.next();
                    if (elem == null) {
                        provider.getNullValueSerializer().serialize(null, jgen, provider);
                    } else {
                        // Minor optimization to avoid most lookups:
                        Class<?> cc = elem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevClass) {
                            currSerializer = prevSerializer;
                        } else {
                            currSerializer = provider.findValueSerializer(cc);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                } while (it.hasNext());
            }
            jgen.writeEndArray();
        }
    }

    public final static class EnumSetSerializer
        extends JsonSerializer<EnumSet<? extends Enum<?>>>
    {
        public final static CollectionSerializer instance = new CollectionSerializer();

        @Override
		public void serialize(EnumSet<? extends Enum<?>> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (Enum<?> en : value) {
                jgen.writeString(en.name());
            }
            jgen.writeEndArray();
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, Maps
    ////////////////////////////////////////////////////////////
     */

    public final static class MapSerializer
        extends JsonSerializer<Map<?,?>>
    {
        public final static MapSerializer instance = new MapSerializer();

        @Override
		public void serialize(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartObject();

            final int len = value.size();

            if (len > 0) {
                final JsonSerializer<Object> keySerializer = provider.getKeySerializer();
                JsonSerializer<Object> prevValueSerializer = null;
                Class<?> prevValueClass = null;

                for (Map.Entry<?,?> entry : value.entrySet()) {
                    // First, serialize key
                    Object keyElem = entry.getKey();
                    if (keyElem == null) {
                        provider.getNullKeySerializer().serialize(null, jgen, provider);
                    } else {
                        keySerializer.serialize(keyElem, jgen, provider);
                    }

                    // And then value
                    Object valueElem = entry.getValue();
                    if (valueElem == null) {
                        provider.getNullValueSerializer().serialize(null, jgen, provider);
                    } else {
                        Class<?> cc = valueElem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevValueClass) {
                            currSerializer = prevValueSerializer;
                        } else {
                            currSerializer = provider.findValueSerializer(cc);
                            prevValueSerializer = currSerializer;
                            prevValueClass = cc;
                        }
                        currSerializer.serialize(valueElem, jgen, provider);
                    }
                }
            }
                
            jgen.writeEndObject();
        }
    }

    public final static class EnumMapSerializer
        extends JsonSerializer<EnumMap<? extends Enum<?>, ?>>
    {
		@Override
		public void serialize(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartObject();
            JsonSerializer<Object> prevSerializer = null;
            Class<?> prevClass = null;

            for (Map.Entry<? extends Enum<?>,?> entry : value.entrySet()) {
                // First, serialize key
                jgen.writeFieldName(entry.getKey().name());
                // And then value
                Object valueElem = entry.getKey();
                if (valueElem == null) {
                    provider.getNullValueSerializer().serialize(null, jgen, provider);
                } else {
                    Class<?> cc = valueElem.getClass();
                    JsonSerializer<Object> currSerializer;
                    if (cc == prevClass) {
                        currSerializer = prevSerializer;
                    } else {
                        currSerializer = provider.findValueSerializer(cc);
                        prevSerializer = currSerializer;
                        prevClass = cc;
                    }
                    currSerializer.serialize(valueElem, jgen, provider);
                }
            }
            jgen.writeEndObject();
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, arrays
    ////////////////////////////////////////////////////////////
     */

    /**
     * Generic serializer for Object arrays (<code>Object[]</code>).
     */
    public final static class ObjectArraySerializer
        extends JsonSerializer<Object[]>
    {
        public final static ObjectArraySerializer instance = new ObjectArraySerializer();

        @Override
		public void serialize(Object[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            final int len = value.length;
            if (len > 0) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                for (int i = 0; i < len; ++i) {
                    Object elem = value[i];
                    if (elem == null) {
                        provider.getNullValueSerializer().serialize(null, jgen, provider);
                    } else {
                        // Minor optimization to avoid most lookups:
                        Class<?> cc = elem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevClass) {
                            currSerializer = prevSerializer;
                        } else {
                            currSerializer = provider.findValueSerializer(cc);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                }
            }
            jgen.writeEndArray();
        }
    }

    public final static class StringArraySerializer
        extends JsonSerializer<String[]>
    {
        @Override
		public void serialize(String[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            final int len = value.length;
            if (len > 0) {
                /* 08-Dec-2008, tatus: If we want this to be fully overridable
                 *  (for example, to support String cleanup during writing
                 *  or something), we should find serializer  by provider.
                 *  But for now, that seems like an overkill: and caller can
                 *  add custom serializer if that is needed as well.
                 */
                //JsonSerializer<String> ser = (JsonSerializer<String>)provider.findValueSerializer(String.class);
                for (int i = 0; i < len; ++i) {
                    //ser.serialize(value[i], jgen, provider);
                    jgen.writeString(value[i]);
                }
            }
            jgen.writeEndArray();
        }
    }

    public final static class BooleanArraySerializer
        extends JsonSerializer<boolean[]>
    {
        @Override
		public void serialize(boolean[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeBoolean(value[i]);
            }
            jgen.writeEndArray();
        }
    }

    /**
     * Unlike other integral number array serializers, we do not just print out byte values
     * as numbers. Instead, we assume that it would make more sense to output content
     * as base64 encoded bytes (using default base64 encoding).
     */
    public final static class ByteArraySerializer
        extends JsonSerializer<byte[]>
    {
        @Override
		public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBinary(value);
        }
    }

    public final static class ShortArraySerializer
        extends JsonSerializer<short[]>
    {
        @SuppressWarnings("cast")
		@Override
		public void serialize(short[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber((int)value[i]);
            }
            jgen.writeEndArray();
        }
    }

    /**
     * Character arrays are different from other integral number arrays in that
     * they are most likely to be textual data, and should be written as
     * Strings, not arrays of entries.
     */
    public final static class CharArraySerializer
        extends JsonSerializer<char[]>
    {
        @Override
		public void serialize(char[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value, 0, value.length);
        }
    }

    public final static class IntArraySerializer
        extends JsonSerializer<int[]>
    {
        @Override
		public void serialize(int[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
            jgen.writeEndArray();
        }
    }

    public final static class LongArraySerializer
        extends JsonSerializer<long[]>
    {
        @Override
		public void serialize(long[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
            jgen.writeEndArray();
        }
    }

    public final static class FloatArraySerializer
        extends JsonSerializer<float[]>
    {
        @Override
		public void serialize(float[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
            jgen.writeEndArray();
        }
    }

    public final static class DoubleArraySerializer
        extends JsonSerializer<double[]>
    {
        @Override
		public void serialize(double[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
            jgen.writeEndArray();
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
            jgen.writeNumber(value.getTimeInMillis());
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
            jgen.writeNumber(value.getTime());
        }
    }

    /**
     * Compared to regular {@link UtilDateSerializer}, we do use String
     * representation here. Why? Basically to truncate of time part, since
     * that should not be used by plain 
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
