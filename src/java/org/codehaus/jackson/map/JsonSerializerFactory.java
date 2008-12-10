package org.codehaus.jackson.map;

/**
 * Abstract class that defines API used by {@link JsonSerializerProvider}
 * to obtain actual
 * {@link JsonSerializer} instances from multiple distinct factories.
 */
public abstract class JsonSerializerFactory
{
    /**
     * Method called to create (or, for completely immutable serializers,
     * reuse) a serializer for given type.
     *
     * @param type Type to be serialized
     */
    public abstract <T> JsonSerializer<T> createSerializer(Class<T> type);
}
