package org.codehaus.jackson.map;

import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Abstract class that defines API used by {@link SerializerProvider}
 * to obtain actual
 * {@link JsonSerializer} instances from multiple distinct factories.
 */
public abstract class SerializerFactory
{
    /*
    /********************************************************
    /* Basic SerializerFactory API:
    /********************************************************
     */

    /**
     * Method called to create (or, for immutable serializers, reuse)
     * a serializer for given type.
     *
     * @param type Type to be serialized
     * @param config Generic serialization configuration
     * 
     * @deprecated Use {@link #createSerializer(JavaType,SerializationConfig)} instead
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public <T> JsonSerializer<T> createSerializer(Class<T> type, SerializationConfig config) {
        return (JsonSerializer<T>) createSerializer(TypeFactory.type(type), config);        
    }

    /**
     * Method called to create (or, for immutable serializers, reuse)
     * a serializer for given type.
     *<p>
     * Default implementation just calls {@link #createSerializer(Class, SerializationConfig)};
     * sub-classes need to override method
     *
     * @param type Type to be serialized
     * @param config Generic serialization configuration
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> createSerializer(JavaType type, SerializationConfig config) {
        return (JsonSerializer<Object>) createSerializer(type.getRawClass(), config);
    }
    
    /**
     * Method called to create a type information serializer for given base type,
     * if one is needed. If not needed (no polymorphic handling configured), should
     * return null.
     *
     * @param baseType Declared type to use as the base type for type information serializer
     * 
     * @return Type serializer to use for the base type, if one is needed; null if not.
     * 
     * @since 1.5
     */
    public TypeSerializer createTypeSerializer(JavaType baseType, SerializationConfig config)
    {
        // Default implementation returns null for backwards compatibility reasons.
        return null;
    }

    /*
    /********************************************************
    /* Additional configuration
    /********************************************************
     */

    /**
     * Method that can be used to register additional serializers to be provided;
     * will return a factory that uses specified provider, usually a newly
     * constructed factory instance.
     *<p>
     * Note that custom sub-classes generally <b>must override</b> implementation
     * of this method, as it usually requires instantiating a new instance of
     * factory type. Check out javadocs for
     * {@link org.codehaus.jackson.map.ser.BeanSerializerFactory} for more details.
     * 
     * @since 1.7
     */
    public SerializerFactory withAdditionalSerializers(Serializers additionalSerializer) {
        throw new UnsupportedOperationException();
    }
}
