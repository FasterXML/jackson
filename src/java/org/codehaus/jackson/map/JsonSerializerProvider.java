package org.codehaus.jackson.map;

/**
 * Abstract class that defines API used by {@link JavaTypeMapper} and
 * {@link JsonSerializer}s to obtain serializers capable of serializing
 * instances of specific types.
 */
public abstract class JsonSerializerProvider
{
    /**
     * Method called to get hold of a serializer for given type; or
     * if no such serializer can be found, a default handler (which
     * may do a best-effort generic serialization or just simply
     * throw an exception when invoked).
     */
    public abstract <T> JsonSerializer<T> findSerializer(Class<T> type);
}
