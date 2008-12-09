package org.codehaus.jackson.map.impl.prov;

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

    @Override
    public abstract <T> JsonSerializer<T> findValueSerializer(Class<T> type);

    @Override
    public abstract <T> JsonSerializer<T> findNonNullKeySerializer(Class<T> type);

    @Override
    public abstract JsonSerializer<?> getNullKeySerializer();

    @Override
    public abstract JsonSerializer<?> getNullValueSerializer();
}
