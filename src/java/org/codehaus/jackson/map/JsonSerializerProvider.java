package org.codehaus.jackson.map;

/**
 * Abstract class that defines API used by {@link JavaTypeMapper} and
 * {@link JsonSerializer}s to obtain serializers capable of serializing
 * instances of specific types.
 */
public abstract class JsonSerializerProvider
{
    /*
    //////////////////////////////////////////////////////
    // General serializer locating methods
    //////////////////////////////////////////////////////
     */

    /**
     * Method called to get hold of a serializer for a value of given type;
     * or if no such serializer can be found, a default handler (which
     * may do a best-effort generic serialization or just simply
     * throw an exception when invoked).
     *<p>
     * Note: this method is only called for non-null values; not for keys
     * or null values. For these, check out other accessor methods.
     */
    public abstract <T> JsonSerializer<T> findValueSerializer(Class<T> type);

    /*
    //////////////////////////////////////////////////////
    // Accessors for specialized serializers
    //////////////////////////////////////////////////////
     */

    /**
     * Method called to get the serializer to use for serializing
     * Map keys that are not nulls (for null keys,
     * {@link #getNullKeySerializer} is called instead).
     * Separation from regular
     * {@link #findSerializer} method is due to Json only allowing
     * Json Strings as field names; and hence different serialization
     * strategy may be needed.
     */
    public abstract <T> JsonSerializer<T> findNonNullKeySerializer(Class<T> type);

    /**
     * Method called to get the serializer to use for serializing
     * Map keys that are nulls: this is needed since Json does not allow
     * any non-String value as key, including null.
     *<p>
     * Typically, returned serializer
     * will either throw an exception, or use an empty String; but
     * other behaviors are possible.
     */
    public abstract JsonSerializer<?> getNullKeySerializer();

    /**
     * Method called to get the serializer to use for serializing
     * values (root level, Array members or List field values)
     * that are nulls. Specific accessor is needed because nulls
     * in Java do not contain type information.
     *<p>
     * Typically returned serializer just writes out Json literal
     * null value.
     */
    public abstract JsonSerializer<?> getNullValueSerializer();
}
