package org.codehaus.jackson.map.deser;

import java.util.Map;

/**
 * Helper class used for storing mapping from property name to
 * {@link SettableBeanProperty} instances.
 *<p>
 * Note that this class is used instead of generic {@link java.util.HashMap}
 * is performance: although default implementation is very good for generic
 * use cases, it can still be streamlined a bit for specific use case
 * we have.
 * 
 * @since 1.7
 */
final class BeanPropertyMap
{
    private final Bucket[] _buckets;

    private final int _size;
    
    public BeanPropertyMap(Map<String,SettableBeanProperty> properties)
    {
        int size = findSize(properties.size());
        _size = size;
        int hashMask = (size-1);
        Bucket[] buckets = new Bucket[size];
        for (Map.Entry<String,SettableBeanProperty> entry : properties.entrySet()) {
            String key = entry.getKey();
            int index = key.hashCode() & hashMask;
            buckets[index] = new Bucket(buckets[index], key, entry.getValue());
        }
        _buckets = buckets;
    }

    private final static int findSize(int size)
    {
        // For small enough results (32 or less), we'll require <= 50% fill rate; otherwise 80%
        int needed = (size <= 32) ? (size + size) : (size + (size >> 2));
        int result = 2;
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
    
    public SettableBeanProperty find(String key)
    {
        int index = key.hashCode() & (_buckets.length-1);
        Bucket bucket = _buckets[index];
        while (bucket != null) {
            if (bucket.key == key) {
                return bucket.value;
            }
            bucket = bucket.next;
        }
        // Do we need fallback for non-interned Strings?
        return _findWithEquals(key, index);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private SettableBeanProperty _findWithEquals(String key, int index)
    {
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
        public final Bucket next;
        public final String key;
        public final SettableBeanProperty value;
        
        public Bucket(Bucket next, String key, SettableBeanProperty value)
        {
            this.next = next;
            this.key = key;
            this.value = value;
        }
    }
}
