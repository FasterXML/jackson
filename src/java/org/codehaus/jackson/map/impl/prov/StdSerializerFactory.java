package org.codehaus.jackson.map.impl.prov;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerFactory;
import org.codehaus.jackson.map.JsonSerializerProvider;

/**
 * Factory class that can provide serializers for standard JDK classes,
 * as well as custom classes that extend standard classes or implement
 * one of "well-known" interfaces (such as {@link java.util.Collection}).
 */
public class StdSerializerFactory
    extends JsonSerializerFactory
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
        // Boolean type
        _concrete.put(Boolean.class.getName(), new BooleanSerializer());

        // String and string-like types (note: date types explicitly
        // not included -- can use either textual or numeric serialization)
        _concrete.put(String.class.getName(), new StringSerializer());
        _concrete.put(StringBuffer.class.getName(), StringLikeSerializer.instance);
        _concrete.put(StringBuilder.class.getName(), StringLikeSerializer.instance);
        _concrete.put(Character.class.getName(), StringLikeSerializer.instance);
        // including things best serialized as Strings
        _concrete.put(UUID.class.getName(), StringLikeSerializer.instance);
        
        // Numbers, limited length integral
        final IntegerSerializer intS = new IntegerSerializer();
        _concrete.put(Byte.class.getName(), intS);
        _concrete.put(Short.class.getName(), intS);
        _concrete.put(Integer.class.getName(), intS);
        _concrete.put(Long.class.getName(), new LongSerializer());

        // Numbers, limited length floating point
        _concrete.put(Float.class.getName(), new FloatSerializer());
        _concrete.put(Double.class.getName(), new DoubleSerializer());

        // Other numbers, more complicated
        final NumberSerializer ns = new NumberSerializer();
        _concrete.put(BigInteger.class.getName(), ns);
        _concrete.put(BigDecimal.class.getName(), ns);

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
        _concrete.put(EnumMap.class.getName(), mapS);
        _concrete.put(Properties.class.getName(), mapS);

        _concrete.put(HashSet.class.getName(), collectionS);
        _concrete.put(LinkedHashSet.class.getName(), collectionS);
        _concrete.put(TreeSet.class.getName(), collectionS);

        /* Finally, couple of oddball types. Not sure if these are
         * really needed...
         */
        final NullSerializer nullS = NullSerializer.instance;
        _concrete.put(Void.TYPE.getName(), nullS);
    }

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
     * Fast lookup-based accessor method
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
         * - Most collection types
         * - java.lang.Number (but is that integral or not?)
         */
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
        if (Collection.class.isAssignableFrom(type)) {
            return CollectionSerializer.instance;
        }
        if (Number.class.isAssignableFrom(type)) {
            return NumberSerializer.instance;
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
        public void serialize(Boolean value, JsonGenerator jgen, JsonSerializerProvider provider)
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
        public void serialize(String value, JsonGenerator jgen, JsonSerializerProvider provider)
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

        public void serialize(Object value, JsonGenerator jgen, JsonSerializerProvider provider)
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
        public void serialize(Integer value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.intValue());
        }
    }

    public final static class LongSerializer
        extends JsonSerializer<Long>
    {
        public void serialize(Long value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.longValue());
        }
    }

    public final static class FloatSerializer
        extends JsonSerializer<Float>
    {
        public void serialize(Float value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.floatValue());
        }
    }

    public final static class DoubleSerializer
        extends JsonSerializer<Double>
    {
        public void serialize(Double value, JsonGenerator jgen, JsonSerializerProvider provider)
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

        public void serialize(Number value, JsonGenerator jgen, JsonSerializerProvider provider)
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

        @SuppressWarnings("unchecked")
        public void serialize(List<?> value, JsonGenerator jgen, JsonSerializerProvider provider)
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
                            currSerializer = (JsonSerializer<Object>)provider.findValueSerializer(cc);
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

        @SuppressWarnings("unchecked")
        public void serialize(Collection<?> value, JsonGenerator jgen, JsonSerializerProvider provider)
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
                            currSerializer = (JsonSerializer<Object>)provider.findValueSerializer(cc);
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
        @SuppressWarnings("unchecked")
        public void serialize(Iterator<?> value, JsonGenerator jgen, JsonSerializerProvider provider)
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
                            currSerializer = (JsonSerializer<Object>)provider.findValueSerializer(cc);
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
        @SuppressWarnings("unchecked")
        public void serialize(Iterable<?> value, JsonGenerator jgen, JsonSerializerProvider provider)
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
                            currSerializer = (JsonSerializer<Object>)provider.findValueSerializer(cc);
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

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, Maps
    ////////////////////////////////////////////////////////////
     */

    public final static class MapSerializer
        extends JsonSerializer<Map<?,?>>
    {
        public final static MapSerializer instance = new MapSerializer();

        @SuppressWarnings("unchecked")
        public void serialize(Map<?,?> value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();

            final int len = value.size();

            if (len > 0) {
                JsonSerializer<Object> prevKeySerializer = null;
                JsonSerializer<Object> prevValueSerializer = null;
                Class<?> prevKeyClass = null;
                Class<?> prevValueClass = null;

                for (Map.Entry<?,?> entry : value.entrySet()) {
                    // First, serialize key
                    Object keyElem = entry.getKey();
                    if (keyElem == null) {
                        provider.getNullKeySerializer().serialize(null, jgen, provider);
                    } else {
                        Class<?> cc = keyElem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevKeyClass) {
                            currSerializer = prevKeySerializer;
                        } else {
                            currSerializer = (JsonSerializer<Object>)provider.findNonNullKeySerializer(cc);
                            prevKeySerializer = currSerializer;
                            prevKeyClass = cc;
                        }
                        currSerializer.serialize(keyElem, jgen, provider);
                    }

                    // And then value
                    Object valueElem = entry.getKey();
                    if (valueElem == null) {
                        provider.getNullValueSerializer().serialize(null, jgen, provider);
                    } else {
                        Class<?> cc = valueElem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevValueClass) {
                            currSerializer = prevValueSerializer;
                        } else {
                            currSerializer = (JsonSerializer<Object>)provider.findValueSerializer(cc);
                            prevValueSerializer = currSerializer;
                            prevValueClass = cc;
                        }
                        currSerializer.serialize(valueElem, jgen, provider);
                    }
                }
            }
                
            jgen.writeEndArray();
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

        @SuppressWarnings("unchecked")
        public void serialize(Object[] value, JsonGenerator jgen, JsonSerializerProvider provider)
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
                            currSerializer = (JsonSerializer<Object>)provider.findValueSerializer(cc);
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
        public void serialize(String[] value, JsonGenerator jgen, JsonSerializerProvider provider)
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
                //JsonSerializer<String> ser = provider.findValueSerializer(String.class);
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
        public void serialize(boolean[] value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeBoolean(value[i]);
            }
            jgen.writeEndArray();
        }
    }

    public final static class ByteArraySerializer
        extends JsonSerializer<byte[]>
    {
        public void serialize(byte[] value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber((int)value[i]);
            }
            jgen.writeEndArray();
        }
    }

    public final static class ShortArraySerializer
        extends JsonSerializer<short[]>
    {
        public void serialize(short[] value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber((int)value[i]);
            }
            jgen.writeEndArray();
        }
    }

    public final static class CharArraySerializer
        extends JsonSerializer<char[]>
    {
        public void serialize(char[] value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
            jgen.writeEndArray();
        }
    }

    public final static class IntArraySerializer
        extends JsonSerializer<int[]>
    {
        public void serialize(int[] value, JsonGenerator jgen, JsonSerializerProvider provider)
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
        public void serialize(long[] value, JsonGenerator jgen, JsonSerializerProvider provider)
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
        public void serialize(float[] value, JsonGenerator jgen, JsonSerializerProvider provider)
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
        public void serialize(double[] value, JsonGenerator jgen, JsonSerializerProvider provider)
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
    // Other odd-ball special purpose serializers
    ////////////////////////////////////////////////////////////
     */

    /**
     * To allow for special handling for null values (in Objects, Arrays,
     * root-level), handling for nulls is done via serializers too.
     * This is the default serializer for nulls.
     */
    public final static class NullSerializer
        extends JsonSerializer<Object>
    {
        final static NullSerializer instance = new NullSerializer();

        private NullSerializer() { }

        public void serialize(Object value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNull();
        }
    }

    /**
     * This special "serializer" can be used to throw
     * {@link JsonGenerationException} by registering it to handle
     * a type, or as one of special serializers (like one used to handle
     * null Map/Object keys).
     */
    public final static class FailingSerializer
        extends JsonSerializer<Object>
    {
        final String _msg;

        public FailingSerializer(String msg) { _msg = msg; }

        public void serialize(Object value, JsonGenerator jgen, JsonSerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            throw new JsonGenerationException(_msg);
        }
    }
}
