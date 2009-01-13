package org.codehaus.jackson.map.deser;

/**
 * Helper class that contains set of distinct builders for different
 * arrays of primitive values.
 */
public final class ArrayBuilders
{
    PrimitiveArrayBuilder<boolean[]> _booleanBuilder = null;

    // note: no need for char[] builder, assume they are Strings

    PrimitiveArrayBuilder<byte[]> _byteBuilder = null;
    PrimitiveArrayBuilder<short[]> _shortBuilder = null;
    PrimitiveArrayBuilder<int[]> _intBuilder = null;
    PrimitiveArrayBuilder<long[]> _longBuilder = null;

    PrimitiveArrayBuilder<float[]> _floatBuilder = null;
    PrimitiveArrayBuilder<double[]> _doubleBuilder = null;

    public ArrayBuilders() { }
}

