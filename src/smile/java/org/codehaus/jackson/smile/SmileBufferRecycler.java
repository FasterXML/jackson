package org.codehaus.jackson.smile;

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
        return _seenNamesBuffer;
        /*
        if (buffer == null) {
            buffer = new String[DEFAULT_NAME_BUFFER_LENGTH];
        } else {
            _seenNamesBuffer = null;
        }
        return buffer;
        */
    }

    public T[] allocSeenStringValuesBuffer()
    {
        return _seenStringValuesBuffer;
        /*
        if (buffer == null) {
            buffer = new String[DEFAULT_STRING_VALUE_BUFFER_LENGTH];
        } else {
            _seenStringValuesBuffer = null;
        }
        return buffer;
        */
    }
    
    public void releaseSeenNamesBuffer(T[] buffer) {
        _seenNamesBuffer = buffer;
    }

    public void releaseSeenStringValuesBuffer(T[] buffer) {
        _seenStringValuesBuffer = buffer;
    }
}
