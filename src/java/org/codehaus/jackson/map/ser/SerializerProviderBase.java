package org.codehaus.jackson.map.ser;

import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerProvider;

/**
 * Abstract base class that defines lowest-level common feature of most
 * implementations of {@link JsonSerializerProvider}: that of locally
 * storing references to serializers once they have been succesfully
 * constructed.
 */
public abstract class SerializerProviderBase
    extends JsonSerializerProvider
{
    final ConcurrentHashMap<ClassKey, JsonSerializer<Object>> _resolvedSerializers = new ConcurrentHashMap<ClassKey, JsonSerializer<Object>>(4);

    @SuppressWarnings("unchecked")
    public final JsonSerializer<Object> findValueSerializer(Class<?> type)
    {
        ClassKey key = new ClassKey(type);
        JsonSerializer<Object> ser = _resolvedSerializers.get(key);
        if (ser != null) {
            return ser;
        }
        // Not yet constructed? Construct it if possible
        ser = (JsonSerializer<Object>)constructValueSerializer(type);
        if (ser == null) {
            ser = getUnknownTypeSerializer();
            /* But should we add this to resolved ones?
             * May need to allow configuring behavior: but
             * for now, let's not 'cache' it.
             */
            return ser;
        }
        // otherwise, let's add it to known serializers
        _resolvedSerializers.put(key, ser);
        return ser;
    }

    @Override
    public abstract JsonSerializer<Object> getKeySerializer();

    @Override
    public abstract JsonSerializer<Object> getNullKeySerializer();

    @Override
    public abstract JsonSerializer<Object> getUnknownTypeSerializer();

    /*
    //////////////////////////////////////////////////
    // Abstract methods sub-classes need to implement
    //////////////////////////////////////////////////
     */

    protected abstract JsonSerializer<?> constructValueSerializer(Class<?> type);
}
