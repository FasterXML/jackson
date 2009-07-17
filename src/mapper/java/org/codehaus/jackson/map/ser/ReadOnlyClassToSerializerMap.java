package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.type.ClassKey;

/**
 * Optimized lookup table for accessing {@link JsonSerializer} instances
 * keyed by {@link Class}. Initially this just uses a regular
 * {@link HashMap}, could optimize later on if need be
 */
public final class ReadOnlyClassToSerializerMap
{
    final HashMap<ClassKey, JsonSerializer<Object>> _map;

    /**
     * We'll reuse key class to avoid unnecessary instantiations; since
     * this is not shared between threads, we can just reuse single
     * instance.
     */
    final ClassKey _key;

    private ReadOnlyClassToSerializerMap(HashMap<ClassKey, JsonSerializer<Object>> map, ClassKey key)
    {
        _map = map;
        _key = key;
    }

    public ReadOnlyClassToSerializerMap instance()
    {
        return new ReadOnlyClassToSerializerMap(_map, new ClassKey());
    }

    /**
     * Factory method for creating the "blueprint" lookup map. Such map
     * can not be used as is but just shared: to get an actual usable
     * instance, {@link #instance} has to be called first.
     */
    @SuppressWarnings("unchecked")
    public static ReadOnlyClassToSerializerMap from(HashMap<ClassKey, JsonSerializer<Object>> src)
    {
        return new ReadOnlyClassToSerializerMap((HashMap<ClassKey, JsonSerializer<Object>>)src.clone(), null);
    }

    public JsonSerializer<Object> get(Class<?> clz)
    {
        _key.reset(clz);
        return _map.get(_key);
    }
}

