package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ser.SerializerCache.*;

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
    final TypedKeyRaw _typedKeyRaw = new TypedKeyRaw(getClass());

    final TypedKeyFull _typedKeyFull = new TypedKeyFull(null);

    final UntypedKeyRaw _untypedKeyRaw = new UntypedKeyRaw(getClass());
    
    private ReadOnlyClassToSerializerMap(HashMap<Object, JsonSerializer<Object>> map)
    {
        _map = map;
    }

    public ReadOnlyClassToSerializerMap instance()
    {
        return new ReadOnlyClassToSerializerMap(_map);
    }

    /**
     * Factory method for creating the "blueprint" lookup map. Such map
     * can not be used as is but just shared: to get an actual usable
     * instance, {@link #instance} has to be called first.
     */
    @SuppressWarnings("unchecked")
    public static ReadOnlyClassToSerializerMap from(HashMap<Object, JsonSerializer<Object>> src)
    {
        return new ReadOnlyClassToSerializerMap((HashMap<Object, JsonSerializer<Object>>)src.clone());
    }

    public JsonSerializer<Object> typedValueSerializer(JavaType type)
    { 
        _typedKeyFull.reset(type);
        return _map.get(_typedKeyFull);
    }

    public JsonSerializer<Object> typedValueSerializer(Class<?> cls)
    { 
        _typedKeyRaw.reset(cls);
        return _map.get(_typedKeyRaw);
    }
    
    public JsonSerializer<Object> untypedValueSerializer(Class<?> cls)
    { 
        _untypedKeyRaw.reset(cls);
        return _map.get(_untypedKeyRaw);
    }

    /**
     * @since 1.5
     */
    public JsonSerializer<Object> untypedValueSerializer(JavaType type)
    { 
        return _map.get(type);
    }
}
