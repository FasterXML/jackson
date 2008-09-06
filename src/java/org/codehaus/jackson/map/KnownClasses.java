package org.codehaus.jackson.map;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Helper class used for fast handling of "well-known" JDK primitive
 * wrapper and data struct classes.
 */
final class KnownClasses
{
    public enum JdkClasses {
        BOOLEAN,
            STRING, STRING_LIKE,

            NUMBER_INTEGER, NUMBER_LONG, NUMBER_DOUBLE,
            NUMBER_OTHER, // to be converted via 'raw'...

            ARRAY_LONG, ARRAY_INT, ARRAY_SHORT, ARRAY_CHAR, ARRAY_BYTE,
            ARRAY_DOUBLE, ARRAY_FLOAT,
            ARRAY_BOOLEAN,
            ARRAY_OBJECT,

            MAP, LIST_INDEXED, LIST_OTHER, COLLECTION,
            ITERABLE, ITERATOR
    };

    final static HashMap<String, JdkClasses> mConcrete = 
        new HashMap<String, JdkClasses>();
    static {
        // Boolean type
        mConcrete.put(Boolean.class.getName(), JdkClasses.BOOLEAN);

        /* String and string-like types (note: date types explicitly
         * not included -- can use either textual or numeric serialization)
         */
        mConcrete.put(String.class.getName(), JdkClasses.STRING);
        mConcrete.put(StringBuffer.class.getName(), JdkClasses.STRING_LIKE);
        mConcrete.put(StringBuilder.class.getName(), JdkClasses.STRING_LIKE);
        mConcrete.put(Character.class.getName(), JdkClasses.STRING_LIKE);
        // including things best serialized as Strings
        mConcrete.put(UUID.class.getName(), JdkClasses.STRING_LIKE);
        
        // Arrays of various types (including common object types)

        mConcrete.put(new long[0].getClass().getName(), JdkClasses.ARRAY_LONG);
        mConcrete.put(new int[0].getClass().getName(), JdkClasses.ARRAY_INT);
        mConcrete.put(new short[0].getClass().getName(), JdkClasses.ARRAY_SHORT);
        mConcrete.put(new char[0].getClass().getName(), JdkClasses.ARRAY_CHAR);
        mConcrete.put(new byte[0].getClass().getName(), JdkClasses.ARRAY_BYTE);
        mConcrete.put(new double[0].getClass().getName(), JdkClasses.ARRAY_DOUBLE);
        mConcrete.put(new float[0].getClass().getName(), JdkClasses.ARRAY_FLOAT);
        mConcrete.put(new boolean[0].getClass().getName(), JdkClasses.ARRAY_BOOLEAN);

        mConcrete.put(new Object[0].getClass().getName(), JdkClasses.ARRAY_OBJECT);
        mConcrete.put(new String[0].getClass().getName(), JdkClasses.ARRAY_OBJECT);

        // Numbers, limited length integral
        mConcrete.put(Byte.class.getName(), JdkClasses.NUMBER_INTEGER);
        mConcrete.put(Short.class.getName(), JdkClasses.NUMBER_INTEGER);
        mConcrete.put(Integer.class.getName(), JdkClasses.NUMBER_INTEGER);
        mConcrete.put(Long.class.getName(), JdkClasses.NUMBER_LONG);

        // Numbers, limited length floating point
        mConcrete.put(Float.class.getName(), JdkClasses.NUMBER_DOUBLE);
        mConcrete.put(Double.class.getName(), JdkClasses.NUMBER_DOUBLE);

        // Numbers, more complicated
        mConcrete.put(BigInteger.class.getName(), JdkClasses.NUMBER_OTHER);
        mConcrete.put(BigDecimal.class.getName(), JdkClasses.NUMBER_OTHER);

        // And then Java Collection classes
        mConcrete.put(HashMap.class.getName(), JdkClasses.MAP);
        mConcrete.put(Hashtable.class.getName(), JdkClasses.MAP);
        mConcrete.put(LinkedHashMap.class.getName(), JdkClasses.MAP);
        mConcrete.put(TreeMap.class.getName(), JdkClasses.MAP);
        mConcrete.put(EnumMap.class.getName(), JdkClasses.MAP);
        mConcrete.put(Properties.class.getName(), JdkClasses.MAP);

        mConcrete.put(ArrayList.class.getName(), JdkClasses.LIST_INDEXED);
        mConcrete.put(Vector.class.getName(), JdkClasses.LIST_INDEXED);
        mConcrete.put(LinkedList.class.getName(), JdkClasses.LIST_OTHER);

        mConcrete.put(HashSet.class.getName(), JdkClasses.COLLECTION);
        mConcrete.put(LinkedHashSet.class.getName(), JdkClasses.COLLECTION);
        mConcrete.put(TreeSet.class.getName(), JdkClasses.COLLECTION);
    }

    /**
     * Quick lookup method that tries to if the concrete class
     * happens to be one of well-known classes. This works for
     * leaf class types, but not for sub-classes of those
     * types, and possibly not for generics versions of
     * types.
     */
    public final static JdkClasses findTypeFast(Object value)
    {
        return mConcrete.get(value.getClass().getName());
    }

    /**
     * Slower Reflection-based type inspector method.
     */
    public final static JdkClasses findTypeSlow(Object value)
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

        if (value instanceof Map) {
            return JdkClasses.MAP;
        }
        if (value instanceof Object[]) {
            return JdkClasses.ARRAY_OBJECT;
        }
        if (value instanceof List) {
            /* Could check marker interface now, to know whether
             * to index. But let's not bother... shouldn't make
             * big difference.
             */
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
}

