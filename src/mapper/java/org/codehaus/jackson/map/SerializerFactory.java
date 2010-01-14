package org.codehaus.jackson.map;

/**
 * Abstract class that defines API used by {@link SerializerProvider}
 * to obtain actual
 * {@link JsonSerializer} instances from multiple distinct factories.
 */
public abstract class SerializerFactory
{
    /*
    /////////////////////////////////////////////////////////
    // Basic SerializerFactory API:
    /////////////////////////////////////////////////////////
     */

    /**
     * Method called to create (or, for completely immutable serializers,
     * reuse) a serializer for given type.
     *
     * @param type Type to be serialized
     * @param config Generic serialization configuration
     */
    public abstract <T> JsonSerializer<T> createSerializer(Class<T> type, SerializationConfig config);

    /**
     * Method called to create a type information serializer for given base type,
     * if one is needed. If not needed (no polymorphic handling configured), should
     * return null.
     *
     * @param baseType Declared type to use as the base type for type information serializer
     * 
     * @return Type serializer to use for the base type, if one is needed; null if not.
     */
    public abstract TypeSerializer createTypeSerializer(Class<?> baseType, SerializationConfig config);
}
