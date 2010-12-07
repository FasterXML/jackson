package org.codehaus.jackson.map.ser;

import java.util.Map;

import org.codehaus.jackson.map.JsonSerializer;

public class JsonSerializerMap
{
    private final Bucket[] _buckets;

    private final int _size;
    
    public JsonSerializerMap(Map<Object,JsonSerializer<Object>> serializers)
    {
        int size = findSize(serializers.size());
        _size = size;
        int hashMask = (size-1);
        Bucket[] buckets = new Bucket[size];
        for (Map.Entry<Object,JsonSerializer<Object>> entry : serializers.entrySet()) {
            Object key = entry.getKey();
            int index = key.hashCode() & hashMask;
            buckets[index] = new Bucket(buckets[index], key, entry.getValue());
        }
        _buckets = buckets;
    }
    
    private final static int findSize(int size)
    {
        // For small enough results (64 or less), we'll require <= 50% fill rate; otherwise 80%
        int needed = (size <= 64) ? (size + size) : (size + (size >> 2));
        int result = 8;
        while (result < needed) {
            result += result;
        }
        return result;
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public int size() { return _size; }
    
    public JsonSerializer<Object> find(Object key)
    {
        int index = key.hashCode() & (_buckets.length-1);
        Bucket bucket = _buckets[index];
        while (bucket != null) {
            if (key.equals(bucket.key)) {
                return bucket.value;
            }
            bucket = bucket.next;
        }
        return null;
    }

    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */
    
    private final static class Bucket
    {
        public final Object key;
        public final JsonSerializer<Object> value;
        public final Bucket next;
        
        public Bucket(Bucket next, Object key, JsonSerializer<Object> value)
        {
            this.next = next;
            this.key = key;
            this.value = value;
        }
    }
}
