package org.codehaus.jackson.map.deser;

/**
 * Helper class that contains set of distinct builders for different
 * arrays of primitive values.
 */
public final class ArrayBuilders
{
    PrimitiveArrayBuilder<boolean[]> _booleanBuilder = null;

    // note: no need for char[] builder, assume they are Strings

    PrimitiveArrayBuilder<byte[]> _booleanBuilder = null;
    PrimitiveArrayBuilder<short[]> _booleanBuilder = null;
    PrimitiveArrayBuilder<int[]> _booleanBuilder = null;
    PrimitiveArrayBuilder<long[]> _booleanBuilder = null;

    PrimitiveArrayBuilder<float[]> _booleanBuilder = null;
    PrimitiveArrayBuilder<double[]> _booleanBuilder = null;

    public ArrayBuilders();
}

