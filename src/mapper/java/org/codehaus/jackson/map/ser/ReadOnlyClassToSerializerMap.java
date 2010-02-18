package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.type.ClassPairKey;

/**
 * Optimized lookup table for accessing two types of serializers; typed
 * and non-typed.
 */
public final class ReadOnlyClassToSerializerMap
{
    /**
     *<p>
     * NOTE: keys are {@link ClassPairKey}s (for "typed" serializers) and
     *    {@link JavaType}s (untyped)
     */
    final HashMap<Object, JsonSerializer<Object>> _map;

    /**
     * We'll reuse key class to avoid unnecessary instantiations; since
     * this is not shared between threads, we can just reuse single
     * instance.
     */
    final ClassPairKey _key;

    private ReadOnlyClassToSerializerMap(HashMap<Object, JsonSerializer<Object>> map, ClassPairKey key)
    {
        _map = map;
        _key = key;
    }

    public ReadOnlyClassToSerializerMap instance()
    {
        return new ReadOnlyClassToSerializerMap(_map, new ClassPairKey(null, null));
    }

    /**
     * Factory method for creating the "blueprint" lookup map. Such map
     * can not be used as is but just shared: to get an actual usable
     * instance, {@link #instance} has to be called first.
     */
    @SuppressWarnings("unchecked")
    public static ReadOnlyClassToSerializerMap from(HashMap<Object, JsonSerializer<Object>> src)
    {
        return new ReadOnlyClassToSerializerMap((HashMap<Object, JsonSerializer<Object>>)src.clone(), null);
    }

    public JsonSerializer<Object> typedValueSerializer(Class<?> runtimeType, Class<?> declaredType)
    { 
        _key.reset(runtimeType, declaredType);
        return _map.get(_key);
    }

    public JsonSerializer<Object> untypedValueSerializer(Class<?> runtimeType)
    { 
        _key.reset(runtimeType, null);
        return _map.get(_key);
    }

    /**
     * @since 1.5
     */
    public JsonSerializer<Object> untypedValueSerializer(JavaType type)
    { 
        return _map.get(type);
    }
}
