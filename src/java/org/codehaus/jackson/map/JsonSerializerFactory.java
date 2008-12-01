package org.codehaus.jackson.map;

/**
 * Abstract class that defines API used by {@link JsonSerializerProvider}
 * to obtain actual
 * {@link JsonSerializer} instances from multiple distinct factories.
 */
public interface JsonSerializerFactory
{
    /**
     * Method called to create (or, for completely immutable serializers,
     * reuse) a serializer for given type.
     */
    public <T> JsonSerializer<T> createSerializer(Class<T> type);
}

