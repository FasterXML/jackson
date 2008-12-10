package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerProvider;

/**
 * Simple cache object that allows for doing 2-level lookups: first level is by "local"
 * read-only lookup Map (used without locking); and second backup level is by shared
 * modifiable Map. The idea is that after a while, most serializers are found from the
 * local Map (to optimize performance, reduce lock contention), while trying to reduce
 * unnecessary construction of serializers during rampup.
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
    private HashMap<ClassKey, JsonSerializer<Object>> _readOnlyMap = null;

    public SerializerCache() {
    }

    /**
     * Method that can be called to get a read-only instance populated from the
     * most recent version of the shared lookup Map.
     */
    public HashMap<ClassKey, JsonSerializer<Object>> getReadOnlyLookupMap()
    {
        synchronized (this) {
            if (_readOnlyMap == null) {
                _readOnlyMap = (HashMap<ClassKey,JsonSerializer<Object>>) _sharedMap.clone();
            }
            return _readOnlyMap;
        }
    }

    /**
     * Method that checks if the shared (and hence, synchronized) lookup Map might have
     * the serializer already.
     */
    public JsonSerializer<Object> findSerializer(ClassKey key)
    {
        synchronized (this) {
            return _sharedMap.get(key);
        }
    }

    /**
     * Method called if none of lookups succeeded, and caller had to construct
     * a serializer. If so, we will update the shared lookup map so that it
     * can be resolved via it next time.
     */
    public void addSerializer(ClassKey key, JsonSerializer<Object> ser)
    {
        synchronized (this) {
            if (_sharedMap.put(key, ser) == null) {
                // let's invalidate the read-only copy, too, to get it updated
                _readOnlyMap = null;
            }
        }
    }
}

