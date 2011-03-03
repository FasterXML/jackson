package org.codehaus.jackson.smile;

import java.util.Arrays;

/**
 * Simple helper class used for implementing simple reuse system for Smile-specific
 * buffers that are used.
 *
 * @param <T> Type of name entries stored in arrays to recycle
 * 
 * @since 1.7
 */
public class SmileBufferRecycler<T>
{
    public final static int DEFAULT_NAME_BUFFER_LENGTH = 64;

    public final static int DEFAULT_STRING_VALUE_BUFFER_LENGTH = 64;
    
    protected T[] _seenNamesBuffer;

    protected T[] _seenStringValuesBuffer;

    public SmileBufferRecycler() { }

    public T[] allocSeenNamesBuffer()
    {
        // 11-Feb-2011, tatu: Used to alloc here; but due to generics, can't easily any more
        T[] result = _seenNamesBuffer;
        if (result != null) {
            // let's ensure we don't retain it here, unless returned
            _seenNamesBuffer = null;
            // and also clean up recycled buffer
            Arrays.fill(result, null);
        }
        return result;
    }

    public T[] allocSeenStringValuesBuffer()
    {
        // 11-Feb-2011, tatu: Used to alloc here; but due to generics, can't easily any more
        T[] result = _seenStringValuesBuffer;
        if (result != null) {
            _seenStringValuesBuffer = null;
            // and also clean up recycled buffer
            Arrays.fill(result, null);
        }
        return result;
    }
    
    public void releaseSeenNamesBuffer(T[] buffer) {
        _seenNamesBuffer = buffer;
    }

    public void releaseSeenStringValuesBuffer(T[] buffer) {
        _seenStringValuesBuffer = buffer;
    }
}
