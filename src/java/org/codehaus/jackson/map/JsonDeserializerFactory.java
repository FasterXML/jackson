package org.codehaus.jackson.map;

/**
 * Abstract class that defines API used by {@link JsonDeserializerProvider}
 * to obtain actual
 * {@link JsonDeserializer} instances from multiple distinct factories.
 */
public abstract class JsonDeserializerFactory
{
    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer for given type.
     *
     * @param type Type to be deserialized
     */
    public abstract <T> JsonDeserializer<T> createDeserializer(Class<T> type);
}
