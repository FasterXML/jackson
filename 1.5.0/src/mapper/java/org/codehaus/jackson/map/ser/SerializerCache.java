package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.type.JavaType;
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
 *<p>
 * Since version 1.5 cache will actually contain three kinds of entries,
 * based on combination of class pair key. First class in key is for the
 * type to serialize, and second one is type used for determining how
 * to resolve value type. One (but not both) of entries can be null.
 */
public final class SerializerCache
{
    /**
     * Shared, modifiable map; all access needs to be through synchronized blocks.
     *<p>
     * NOTE: keys are of various types (see below for key types), in addition to
     * basic {@link JavaType} used for "untyped" serializers.
     */
    private HashMap<Object, JsonSerializer<Object>> _sharedMap = new HashMap<Object, JsonSerializer<Object>>(64);

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
     * untyped serializer for given type.
     */
    public JsonSerializer<Object> untypedValueSerializer(Class<?> type)
    {
        synchronized (this) {
            return _sharedMap.get(new UntypedKeyRaw(type));
        }
    }

    /**
     * @since 1.5
     */
    public JsonSerializer<Object> untypedValueSerializer(JavaType type)
    {
        synchronized (this) {
            return _sharedMap.get(type);
        }
    }

    public JsonSerializer<Object> typedValueSerializer(JavaType type)
    {
        synchronized (this) {
            return _sharedMap.get(new TypedKeyFull(type));
        }
    }

    public JsonSerializer<Object> typedValueSerializer(Class<?> cls)
    {
        synchronized (this) {
            return _sharedMap.get(new TypedKeyRaw(cls));
        }
    }
    
    /**
     * Method called if none of lookups succeeded, and caller had to construct
     * a serializer. If so, we will update the shared lookup map so that it
     * can be resolved via it next time.
     */
    public void addTypedSerializer(JavaType type, JsonSerializer<Object> ser)
    {
        synchronized (this) {
            if (_sharedMap.put(new TypedKeyFull(type), ser) == null) {
                // let's invalidate the read-only copy, too, to get it updated
                _readOnlyMap = null;
            }
        }
    }

    public void addTypedSerializer(Class<?> cls, JsonSerializer<Object> ser)
    {
        synchronized (this) {
            if (_sharedMap.put(new TypedKeyRaw(cls), ser) == null) {
                // let's invalidate the read-only copy, too, to get it updated
                _readOnlyMap = null;
            }
        }
    }
    
    public void addNonTypedSerializer(Class<?> type, JsonSerializer<Object> ser)
    {
        synchronized (this) {
            if (_sharedMap.put(new UntypedKeyRaw(type), ser) == null) {
                // let's invalidate the read-only copy, too, to get it updated
                _readOnlyMap = null;
            }
        }
    }

    /**
     * @since 1.5
     */
    public void addNonTypedSerializer(JavaType type, JsonSerializer<Object> ser)
    {
        synchronized (this) {
            if (_sharedMap.put(type, ser) == null) {
                // let's invalidate the read-only copy, too, to get it updated
                _readOnlyMap = null;
            }
        }
    }
    
    /**
     * @since 1.4
     */
    public synchronized int size() {
        return _sharedMap.size();
    }

    public synchronized void flush() {
        _sharedMap.clear();
    }

    /*
    /**************************************************************
    /* Helper class(es)
    /**************************************************************
     */

    /**
     * Key object used for "typed" serializers (type serializer wrapping
     * value serializer). These are only used
     * for root-level (and similar) access.
     */
    public final static class TypedKeyRaw
    {
         Class<?> _type;

        int _hashCode;

        public TypedKeyRaw(Class<?> cls) {
            reset(cls);
        }

        public void reset(Class<?> cls) {
            _type = cls;
            _hashCode = cls.getName().hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) return true;
            if (o == null) return false;
            if (o.getClass() != getClass()) return false;
            return ((TypedKeyRaw) o)._type == _type;
        }

        @Override public int hashCode() { return _hashCode; }
    }

    public final static class TypedKeyFull
    {
        JavaType _type;

        public TypedKeyFull(JavaType type) {
            _type = type;
        }

        public void reset(JavaType type) {
            _type = type;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) return true;
            if (o == null) return false;
            if (o.getClass() != getClass()) return false;
            return ((TypedKeyFull) o)._type.equals(_type);
        }

        @Override public int hashCode() { return _type.hashCode(); }
    }

    public final static class UntypedKeyRaw
    {
         Class<?> _type;

        int _hashCode;

        public UntypedKeyRaw(Class<?> cls) {
            reset(cls);
        }

        public void reset(Class<?> cls) {
            _type = cls;
            _hashCode = cls.getName().hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) return true;
            if (o == null) return false;
            if (o.getClass() != getClass()) return false;
            return ((UntypedKeyRaw) o)._type == _type;
        }

        @Override public int hashCode() { return _hashCode; }
    }
}
