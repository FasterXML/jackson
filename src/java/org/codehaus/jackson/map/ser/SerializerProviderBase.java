package org.codehaus.jackson.map.ser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerProvider;

/**
 * Abstract base class that defines lowest-level common feature of most
 * implementations of {@JsonSerializerProvider}: that of locally
 * storing references to serializers once they have been succesfully
 * constructed.
 */
public abstract class SerializerProviderBase
    extends JsonSerializerProvider
{
    // ConcurrentHashMap _resolvedSerializers;

    public final JsonSerializer<?> findValueSerializer(Class<?> type)
    {
        // !!! TBI
        return null;
    }

    @Override
    public abstract JsonSerializer<?> findNonNullKeySerializer(Class<?> type);

    @Override
    public abstract JsonSerializer<?> getNullKeySerializer();

    @Override
    public abstract JsonSerializer<?> getNullValueSerializer();

    /*
    //////////////////////////////////////////////////
    // Abstract methods sub-classes need to implement
    //////////////////////////////////////////////////
     */

    protected abstract JsonSerializer<?> constructValueSerializer(Class<?> type);
}
