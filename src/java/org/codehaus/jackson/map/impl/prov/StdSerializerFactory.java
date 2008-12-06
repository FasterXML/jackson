package org.codehaus.jackson.map.impl.prov;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerFactory;
import org.codehaus.jackson.map.JsonSerializerProvider;

/**
 * Factory class that can provide serializers for standard JDK classes.
 */
public class StdSerializerFactory
    extends JsonSerializerFactory
{
    @SuppressWarnings("unchecked")
	public <T> JsonSerializer<T> createSerializer(Class<T> type)
    {
        // First, fast lookup for exact type:
        JsonSerializer<T> ser = (JsonSerializer<T>)_concrete.get(type.getName());
        // and should that fail, slower introspection...
        if (ser == null) {
            // !!! TBI
        }
        return null;
    }

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
        _concrete.put(String.class.getName(), StringLikeSerializer.instance);
        _concrete.put(StringBuffer.class.getName(), StringLikeSerializer.instance);
        _concrete.put(StringBuilder.class.getName(), StringLikeSerializer.instance);
        _concrete.put(Character.class.getName(), StringLikeSerializer.instance);
        // including things best serialized as Strings
        _concrete.put(UUID.class.getName(), StringLikeSerializer.instance);
        
        // Numbers, limited length integral
        IntegerSerializer intS = new IntegerSerializer();
        _concrete.put(Byte.class.getName(), intS);
        _concrete.put(Short.class.getName(), intS);
        _concrete.put(Integer.class.getName(), intS);
        _concrete.put(Long.class.getName(), new LongSerializer());

        // Numbers, limited length floating point
        _concrete.put(Float.class.getName(), new FloatSerializer());
        _concrete.put(Double.class.getName(), new DoubleSerializer());

        /*
        // Numbers, more complicated
        _concrete.put(BigInteger.class.getName(), JdkClasses.NUMBER_OTHER);
        _concrete.put(BigDecimal.class.getName(), JdkClasses.NUMBER_OTHER);

        // Arrays of various types (including common object types)

        _concrete.put(new long[0].getClass().getName(), JdkClasses.ARRAY_LONG);
        _concrete.put(new int[0].getClass().getName(), JdkClasses.ARRAY_INT);
        _concrete.put(new short[0].getClass().getName(), JdkClasses.ARRAY_SHORT);
        _concrete.put(new char[0].getClass().getName(), JdkClasses.ARRAY_CHAR);
        _concrete.put(new byte[0].getClass().getName(), JdkClasses.ARRAY_BYTE);
        _concrete.put(new double[0].getClass().getName(), JdkClasses.ARRAY_DOUBLE);
        _concrete.put(new float[0].getClass().getName(), JdkClasses.ARRAY_FLOAT);
        _concrete.put(new boolean[0].getClass().getName(), JdkClasses.ARRAY_BOOLEAN);

        _concrete.put(new Object[0].getClass().getName(), JdkClasses.ARRAY_OBJECT);
        _concrete.put(new String[0].getClass().getName(), JdkClasses.ARRAY_OBJECT);

        // And then Java Collection classes
        _concrete.put(HashMap.class.getName(), JdkClasses.MAP);
        _concrete.put(Hashtable.class.getName(), JdkClasses.MAP);
        _concrete.put(LinkedHashMap.class.getName(), JdkClasses.MAP);
        _concrete.put(TreeMap.class.getName(), JdkClasses.MAP);
        _concrete.put(EnumMap.class.getName(), JdkClasses.MAP);
        _concrete.put(Properties.class.getName(), JdkClasses.MAP);

        _concrete.put(ArrayList.class.getName(), JdkClasses.LIST_INDEXED);
        _concrete.put(Vector.class.getName(), JdkClasses.LIST_INDEXED);
        _concrete.put(LinkedList.class.getName(), JdkClasses.LIST_OTHER);

        _concrete.put(HashSet.class.getName(), JdkClasses.COLLECTION);
        _concrete.put(LinkedHashSet.class.getName(), JdkClasses.COLLECTION);
        _concrete.put(TreeSet.class.getName(), JdkClasses.COLLECTION);
        */
    }

    /**
     * Slower Reflection-based type inspector method.
     */
    /*
    public final static JdkClasses findTypeSlow(Object value)
    {
    */
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

    /*
        if (value instanceof Map) {
            return JdkClasses.MAP;
        }
        if (value instanceof Object[]) {
            return JdkClasses.ARRAY_OBJECT;
        }
        if (value instanceof List) {
            // Could check marker interface now, to know whether
            // to index. But let's not bother... shouldn't make
            // big difference.
            return JdkClasses.LIST_OTHER;
        }
        if (value instanceof Collection) {
            return JdkClasses.LIST_OTHER;
        }
        if (value instanceof CharSequence) {
            return JdkClasses.STRING_LIKE;
        }
        if (value instanceof Number) {
            return JdkClasses.NUMBER_OTHER;
        }
        if (value instanceof Iterable) {
            return JdkClasses.ITERABLE;
        }
        if (value instanceof Iterator) {
            return JdkClasses.ITERATOR;
        }

        return null;
    }
*/

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, non-numeric primitives + Strings
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

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, Lists/Arrays
    ////////////////////////////////////////////////////////////
     */

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, Maps
    ////////////////////////////////////////////////////////////
     */

}
