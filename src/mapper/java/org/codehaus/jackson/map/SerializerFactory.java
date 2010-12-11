package org.codehaus.jackson.map;

import org.codehaus.jackson.map.introspect.AnnotatedMember;
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
      * Method called to create (or, for immutable serializers, reuse) a serializer for given type. 
      */
    public abstract JsonSerializer<Object> createSerializer(SerializationConfig config, JavaType baseType,
            AnnotatedMember property, String propertyName);
    
    /**
     * Method called to create a type information serializer for given base type,
     * if one is needed. If not needed (no polymorphic handling configured), should
     * return null.
     *
     * @param baseType Declared type to use as the base type for type information serializer
     * 
     * @return Type serializer to use for the base type, if one is needed; null if not.
     * 
     * @since 1.7
     */
    public abstract TypeSerializer createTypeSerializer(SerializationConfig config, JavaType baseType,
            AnnotatedMember property, String propertyName);
    
    /*
    /********************************************************
    /* Deprecated (as of 1.7) SerializerFactory API:
    /********************************************************
     */

    /**
     * Deprecated version of accessor for type id serializer: as of 1.7 one needs
     * to instead call version that passes property information through.
     * 
     * @since 1.5
     * 
     * @deprecated Since 1.7, call variant with more arguments
     */
    @Deprecated
    public final JsonSerializer<Object> createSerializer(JavaType type, SerializationConfig config) {
        return createSerializer(config, type, null, null);
    }
    
    /**
     * Deprecated version of accessor for type id serializer: as of 1.7 one needs
     * to instead call version that passes property information through.
     * 
     * @since 1.5
     * 
     * @deprecated Since 1.7, call variant with more arguments
     */
    @Deprecated
    public final TypeSerializer createTypeSerializer(JavaType baseType, SerializationConfig config) {
        return createTypeSerializer(config, baseType, null, null);
    }

    /*
    /**********************************************************
    /* Additional configuration
    /**********************************************************
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
    public abstract SerializerFactory withAdditionalSerializers(Serializers additionalSerializer);
}
