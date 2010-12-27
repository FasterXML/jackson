package org.codehaus.jackson.smile;

import org.codehaus.jackson.smile.SmileGenerator.SharedStringNode;

/**
 * Smile-specific object similar to {@link org.codehaus.jackson.util.BufferRecycler}
 * used for recycling additional buffer types.
 * Added based on profiling that showed up non-trivial
 * effects of having to allocate buffers for checking shared names and
 * String values
 *
 * @since 1.7
 */
public class SmileBufferRecycler
{
    protected final static int DEFAULT_NAME_BUFFER_LENGTH = 64;

    protected final static int DEFAULT_STRING_VALUE_BUFFER_LENGTH = 64;
    
    protected SharedStringNode[] _seenNamesBuffer;

    protected SharedStringNode[] _seenStringValuesBuffer;

    public SmileBufferRecycler() { }

    public SharedStringNode[] allocSeenNamesBuffer()
    {
        SharedStringNode[] buffer = _seenNamesBuffer;
        if (buffer == null) {
            buffer = new SharedStringNode[DEFAULT_NAME_BUFFER_LENGTH];
        } else {
            _seenNamesBuffer = null;
        }
        return buffer;
    }

    public SharedStringNode[] allocSeenStringValuesBuffer()
    {
        SharedStringNode[] buffer = _seenStringValuesBuffer;
        if (buffer == null) {
            buffer = new SharedStringNode[DEFAULT_STRING_VALUE_BUFFER_LENGTH];
        } else {
            _seenStringValuesBuffer = null;
        }
        return buffer;
    }
    
    public void releaseSeenNamesBuffer(SharedStringNode[] buffer) {
        _seenNamesBuffer = buffer;
    }

    public void releaseSeenStringValuesBuffer(SharedStringNode[] buffer) {
        _seenStringValuesBuffer = buffer;
    }
}
