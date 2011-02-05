package org.codehaus.jackson.map;

import org.codehaus.jackson.map.ser.BeanSerializerModifier;
import org.codehaus.jackson.type.JavaType;

/**
 * Abstract class that defines API used by {@link SerializerProvider}
 * to obtain actual
 * {@link JsonSerializer} instances from multiple distinct factories.
 */
public abstract class SerializerFactory
{

    /*
    /**********************************************************
    /* Helper class to contain configuration settings
    /**********************************************************
     */

    /**
     * Configuration settings container class for bean serializer factory.
     * 
     * @since 1.7
     */
    public abstract static class Config
    {
        /**
         * Method for creating a new instance with additional serializer provider.
         */
        public abstract Config withAdditionalSerializers(Serializers additional);

        /**
         * Method for creating a new instance with additional bean serializer modifier.
         */
        public abstract Config withSerializerModifier(BeanSerializerModifier modifier);
        
        public abstract boolean hasSerializers();

        public abstract boolean hasSerializerModifiers();
        
        public abstract Iterable<Serializers> serializers();
        
        public abstract Iterable<BeanSerializerModifier> serializerModifiers();
    }

    /*
    /**********************************************************
    /* Additional configuration
    /**********************************************************
     */

    /**
     * @since 1.7
     */
    public abstract Config getConfig();
    
    /**
     * Method used for creating a new instance of this factory, but with different
     * configuration. Reason for specifying factory method (instead of plain constructor)
     * is to allow proper sub-classing of factories.
     *<p>
     * Note that custom sub-classes generally <b>must override</b> implementation
     * of this method, as it usually requires instantiating a new instance of
     * factory type. Check out javadocs for
     * {@link org.codehaus.jackson.map.ser.BeanSerializerFactory} for more details.
     * 
     * @since 1.7
     */
    public abstract SerializerFactory withConfig(Config config);

    /**
     * Convenience method for creating a new factory instance with additional serializer
     * provider; equivalent to calling
     *<pre>
     *   withConfig(getConfig().withAdditionalSerializers(additional));
     *<pre>
     * 
     * @since 1.7
     */
    public final SerializerFactory withAdditionalSerializers(Serializers additional) {
        return withConfig(getConfig().withAdditionalSerializers(additional));
    }

    /**
     * Convenience method for creating a new factory instance with additional bean
     * serializer modifier; equivalent to calling
     *<pre>
     *   withConfig(getConfig().withSerializerModifier(modifier));
     *<pre>
     * 
     * @since 1.7
     */
    public final SerializerFactory withSerializerModifier(BeanSerializerModifier modifier) {
        return withConfig(getConfig().withSerializerModifier(modifier));
    }
    
    /*
    /**********************************************************
    /* Basic SerializerFactory API:
    /**********************************************************
     */

    /**
      * Method called to create (or, for immutable serializers, reuse) a serializer for given type. 
      */
    public abstract JsonSerializer<Object> createSerializer(SerializationConfig config, JavaType baseType,
            BeanProperty property);
    
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
            BeanProperty property);
    
    /*
    /**********************************************************
    /* Deprecated (as of 1.7) SerializerFactory API:
    /**********************************************************
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
        return createSerializer(config, type, null);
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
        return createTypeSerializer(config, baseType, null);
    }
}
