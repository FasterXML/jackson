package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.map.JsonSerializer;

/**
 * Simple cache object that allows for doing 2-level lookups: first level is
 * by "local" read-only lookup Map (used without locking)
 * and second backup level is by a shared modifiable HashMap.
 * The idea is that after a while, most serializers are found from the
 * local Map (to optimize performance, reduce lock contention),
 * but that during buildup we can use a shared map to reduce both
 * number of distinct read-only maps constructed, and number of
 * serializers constructed.
 */
public final class SerializerCache
{
    /**
     * Shared, modifiable map; all access needs to be through synchronized blocks.
     */
    private HashMap<ClassKey, JsonSerializer<Object>> _sharedMap = new HashMap<ClassKey, JsonSerializer<Object>>(64);

    /**
     * Most recent read-only instance, created from _sharedMap, if any.
     */
    private ReadOnlyClassToSerializerMap _readOnlyMap = null;

    public SerializerCache() {
    }

    /**
     * Method that can be called to get a read-only instance populated from the
     * most recent version of the shared lookup Map.
     */
    public ReadOnlyClassToSerializerMap getReadOnlyLookupMap()
    {
        synchronized (this) {
            if (_readOnlyMap == null) {
                _readOnlyMap = ReadOnlyClassToSerializerMap.from(_sharedMap);
            }
            return _readOnlyMap.instance();
        }
    }

    /**
     * Method that checks if the shared (and hence, synchronized) lookup Map might have
     * the serializer already.
     */
    public JsonSerializer<Object> findSerializer(Class type)
    {
        synchronized (this) {
            return _sharedMap.get(new ClassKey(type));
        }
    }

    /**
     * Method called if none of lookups succeeded, and caller had to construct
     * a serializer. If so, we will update the shared lookup map so that it
     * can be resolved via it next time.
     */
    public void addSerializer(Class type, JsonSerializer<Object> ser)
    {
        synchronized (this) {
            ClassKey key = new ClassKey(type);
            if (_sharedMap.put(key, ser) == null) {
                // let's invalidate the read-only copy, too, to get it updated
                _readOnlyMap = null;
            }
        }
    }
}

