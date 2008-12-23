package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.type.JavaType;

/**
 * Simple cache object that allows for doing 2-level lookups: first level is
 * by "local" read-only lookup Map (used without locking)
 * and second backup level is by a shared modifiable HashMap.
 * The idea is that after a while, most deserializers are found from the
 * local Map (to optimize performance, reduce lock contention),
 * but that during buildup we can use a shared map to reduce both
 * number of distinct read-only maps constructed, and number of
 * deserializers constructed.
 */
public final class DeserializerCache
{
    /**
     * Shared, modifiable map; all access needs to be through synchronized blocks.
     */
    private HashMap<JavaType, JsonDeserializer<Object>> _sharedMap = new HashMap<JavaType, JsonDeserializer<Object>>(64);

    /**
     * Most recent read-only instance, created from _sharedMap, if any.
     */
    private HashMap<JavaType, JsonDeserializer<Object>> _readOnlyMap = null;

    public DeserializerCache() {
    }

    /**
     * Method that can be called to get a read-only instance populated from the
     * most recent version of the shared lookup Map.
     */
    @SuppressWarnings("unchecked")
    public HashMap<JavaType, JsonDeserializer<Object>> getReadOnlyLookupMap()
    {
        synchronized (this) {
            if (_readOnlyMap == null) {
                _readOnlyMap = (HashMap<JavaType, JsonDeserializer<Object>>) _sharedMap.clone();
            }
            return _readOnlyMap;
        }
    }

    /**
     * Method that checks if the shared (and hence, synchronized) lookup Map might have
     * the deserializer already.
     */
    public JsonDeserializer<Object> findDeserializer(JavaType type)
    {
        synchronized (this) {
            return _sharedMap.get(type);
        }
    }

    /**
     * Method called if none of lookups succeeded, and caller had to construct
     * a deserializer. If so, we will update the shared lookup map so that it
     * can be resolved via it next time.
     */
    public void addDeserializer(JavaType type, JsonDeserializer<Object> ser)
    {
        synchronized (this) {
            if (_sharedMap.put(type, ser) == null) {
                // let's invalidate the read-only copy, too, to get it updated
                _readOnlyMap = null;
            }
        }
    }
}
