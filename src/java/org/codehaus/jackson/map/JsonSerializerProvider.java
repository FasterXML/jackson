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
     * Method called to get hold of a serializer for given type; or
     * if no such serializer can be found, a default handler (which
     * may do a best-effort generic serialization or just simply
     * throw an exception when invoked).
     */
    public abstract <T> JsonSerializer<T> findSerializer(Class<T> type);

    /*
    //////////////////////////////////////////////////////
    // Accessors for specialized serializers
    //////////////////////////////////////////////////////
     */

    /**
     * Method called to get the serializer to use for serializing
     * Map keys that are nulls: this is needed since Json does not allow
     * any non-String value as key, including null.
     * Typically, resulting serializer
     * will either throw an exception, or use an empty String; but
     * other behaviors are possible.
     */
    public abstract SimpleJsonSerializer getNullKeySerializer();

    /**
     * Method called to get the serializer to use for serializing
     * values (root level, Array members or List field values)
     * that are nulls. Specific accessor is needed because nulls
     * in Java do not contain type information.
     */
    public abstract SimpleJsonSerializer getNullValueSerializer();
}
