package org.codehaus.jackson.map.util;

import java.util.*;

/**
 * Helper class that contains set of distinct builders for different
 * arrays of primitive values. It also provides trivially simple
 * reuse scheme, which assumes that caller knows not to use instances
 * concurrently (which works ok with primitive arrays since they can
 * not contain other non-primitive types).
 */
public final class ArrayBuilders
{
    BooleanBuilder _booleanBuilder = null;

    // note: no need for char[] builder, assume they are Strings

    ByteBuilder _byteBuilder = null;
    ShortBuilder _shortBuilder = null;
    IntBuilder _intBuilder = null;
    LongBuilder _longBuilder = null;
    
    FloatBuilder _floatBuilder = null;
    DoubleBuilder _doubleBuilder = null;

    public ArrayBuilders() { }

    public BooleanBuilder getBooleanBuilder()
    {
        if (_booleanBuilder == null) {
            _booleanBuilder = new BooleanBuilder();
        }
        return _booleanBuilder;
    }

    public ByteBuilder getByteBuilder()
    {
        if (_byteBuilder == null) {
            _byteBuilder = new ByteBuilder();
        }
        return _byteBuilder;
    }
    public ShortBuilder getShortBuilder()
    {
        if (_shortBuilder == null) {
            _shortBuilder = new ShortBuilder();
        }
        return _shortBuilder;
    }
    public IntBuilder getIntBuilder()
    {
        if (_intBuilder == null) {
            _intBuilder = new IntBuilder();
        }
        return _intBuilder;
    }
    public LongBuilder getLongBuilder()
    {
        if (_longBuilder == null) {
            _longBuilder = new LongBuilder();
        }
        return _longBuilder;
    }

    public FloatBuilder getFloatBuilder()
    {
        if (_floatBuilder == null) {
            _floatBuilder = new FloatBuilder();
        }
        return _floatBuilder;
    }
    public DoubleBuilder getDoubleBuilder()
    {
        if (_doubleBuilder == null) {
            _doubleBuilder = new DoubleBuilder();
        }
        return _doubleBuilder;
    }

    /*
    /////////////////////////////////////////////////////
    // Impl classes
    /////////////////////////////////////////////////////
     */

    public final static class BooleanBuilder
        extends PrimitiveArrayBuilder<boolean[]>
    {
        public BooleanBuilder() { }
        public final boolean[] _constructArray(int len) { return new boolean[len]; }
    }

    public final static class ByteBuilder
        extends PrimitiveArrayBuilder<byte[]>
    {
        public ByteBuilder() { }
        public final byte[] _constructArray(int len) { return new byte[len]; }
    }
    public final static class ShortBuilder
        extends PrimitiveArrayBuilder<short[]>
    {
        public ShortBuilder() { }
        public final short[] _constructArray(int len) { return new short[len]; }
    }
    public final static class IntBuilder
        extends PrimitiveArrayBuilder<int[]>
    {
        public IntBuilder() { }
        public final int[] _constructArray(int len) { return new int[len]; }
    }
    public final static class LongBuilder
        extends PrimitiveArrayBuilder<long[]>
    {
        public LongBuilder() { }
        public final long[] _constructArray(int len) { return new long[len]; }
    }

    public final static class FloatBuilder
        extends PrimitiveArrayBuilder<float[]>
    {
        public FloatBuilder() { }
        public final float[] _constructArray(int len) { return new float[len]; }
    }
    public final static class DoubleBuilder
        extends PrimitiveArrayBuilder<double[]>
    {
        public DoubleBuilder() { }
        public final double[] _constructArray(int len) { return new double[len]; }
    }
    
    /*
     ***************************************************************
     * Static helper methods
     ***************************************************************
     */

    public static <T> HashSet<T> arrayToSet(T[] elements)
    {
        HashSet<T> result = new HashSet<T>();
        if (elements != null) {
            for (T elem : elements) {
                result.add(elem);
            }
        }
        return result;
    }

    /**
     * Helper method for adding specified element to a List, but also
     * considering case where the List may not have been yet constructed
     * (that is, null is passed instead).
     * 
     * @param list List to add to; may be null to indicate that a new
     *    List is to be constructed
     * @param element Element to add to list
     * 
     * @return List in which element was added; either <code>list</code>
     *   (if it was not null), or a newly constructed List.
     */
    public static <T> List<T> addToList(List<T> list, T element)
    {
        if (list == null) {
            list = new ArrayList<T>();
        }
        list.add(element);
        return list;
    }
}

