package org.codehaus.jackson.io;

import java.lang.ref.SoftReference;

import org.codehaus.jackson.util.BufferRecycler;

/**
 * Helper class used for efficient encoding of JSON String values (including
 * JSON field names) into Strings or UTF-8 byte arrays.
 *
 * @since 1.6
 */
public final class JsonStringEncoder
{
    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftRerefence}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between reader and writer instances.
     */
    final protected static ThreadLocal<SoftReference<JsonStringEncoder>> _threadEncoder
        = new ThreadLocal<SoftReference<JsonStringEncoder>>();

    /*
    /**********************************************************
    /* Construction, instance access
    /**********************************************************
     */
    
    public JsonStringEncoder() { }
    
    /**
     * Factory method for getting an instance; this is either recycled per-thread instance,
     * or a newly constructed one.
     */
    public static JsonStringEncoder getInstance()
    {
        SoftReference<JsonStringEncoder> ref = _threadEncoder.get();
        JsonStringEncoder enc = (ref == null) ? null : ref.get();

        if (enc == null) {
            enc = new JsonStringEncoder();
            _threadEncoder.set(new SoftReference<JsonStringEncoder>(enc));
        }
        return enc;
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    /**
     * Method that will quote text contents using JSON standard quoting,
     * and return results as a character array
     */
    public char[] quoteAsString(String text)
    {
        // !!! TBI
        return null;
    }

    /**
     * Will quote given JSON String value using standard quoting, encode
     * results as UTF-8, and return result as a byte array.
     */
    public byte[] quoteAsUTF8(String text)
    {
        // !!! TBI
        return null;
    }

    /**
     * Will encode given String as UTF-8 (without any quoting), return
     * resulting byte array.
     */
    public byte[] encodeAsUTF8(String text)
    {
        // !!! TBI
        return null;
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

}
