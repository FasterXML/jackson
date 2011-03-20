package org.codehaus.jackson.map;

import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Helper class used for handling details of creating handler instances (things
 * like {@link JsonSerializer}s, {@link JsonDeserializer}s, various type
 * handlers) of specific types. Actual handler type has been resolved at this
 * point, so instantiator is strictly responsible for providing a configured
 * instance by constructing and configuring a new instance, or possibly by
 * recycling a shared instance. One use case is that of allowing
 * dependency injection, which would otherwise be difficult to do.
 *<p>
 * Care has to be taken to ensure that if instance returned is shared, it will
 * be thread-safe; caller will not synchronize access to returned instances.
 * 
 * @since 1.8
 */
public abstract class HandlerInstantiator
{
    /**
     * Accessor for obtaining default instantiator instance
     * (see {@link DefaultImpl} for details.
     */
    public static HandlerInstantiator defaultImplementation() {
        return new DefaultImpl();
    }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    /**
     * Method called to get an instance of deserializer of specified type.
     * 
     * @param config Deserialization configuration in effect
     * @param annotated Element (Class, Method, Field, constructor parameter) that
     *    had annotation defining class of deserializer to construct (to allow
     *    implementation use information from other annotations)
     * @param deserClass Class of deserializer instance to return
     * 
     * @return Deserializer instance to use
     */
    public abstract JsonDeserializer<?> deserializerInstance(DeserializationConfig config,
            Annotated annotated, Class<JsonDeserializer<?>> deserClass);

    /**
     * Method called to get an instance of serializer of specified type.
     * 
     * @param config Serialization configuration in effect
     * @param annotated Element (Class, Method, Field) that
     *    had annotation defining class of serializer to construct (to allow
     *    implementation use information from other annotations)
     * @param serClass Class of serializer instance to return
     * 
     * @return Serializer instance to use
     */
    public abstract JsonSerializer<?> serializerInstance(SerializationConfig config,
            Annotated annotated, Class<JsonSerializer<?>> serClass);

    /**
     * Method called to get an instance of TypeResolverBuilder of specified type.
     * 
     * @param config Mapper configuration in effect (either SerializationConfig or
     *   DeserializationConfig, depending on when instance is being constructed)
     * @param annotated annotated Element (Class, Method, Field) that
     *    had annotation defining class of builder to construct (to allow
     *    implementation use information from other annotations)
     * @param builderClass Class of builder instance to return
     * 
     * @return TypeResolverBuilder instance to use
     */
    public abstract TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config,
            Annotated annotated, Class<? extends TypeResolverBuilder<?>> builderClass);

    /**
     * Method called to get an instance of TypeIdResolver of specified type.
     * 
     * @param config Mapper configuration in effect (either SerializationConfig or
     *   DeserializationConfig, depending on when instance is being constructed)
     * @param annotated annotated Element (Class, Method, Field) that
     *    had annotation defining class of resolver to construct (to allow
     *    implementation use information from other annotations)
     * @param resolverClass Class of resolver instance to return
     * 
     * @return TypeResolverBuilder instance to use
     */
    public abstract TypeIdResolver typeIdResolverInstance(MapperConfig<?> config,
            Annotated annotated, Class<? extends TypeIdResolver> resolverClass);

    /*
    /**********************************************************
    /* Defaul implementation
    /**********************************************************
     */

    /**
     * Default implementation which simply uses reflection to locate
     * zero-argument constructor of given type, and create instance
     * using that (possibly forcing access if constructor is not
     * public; this can be prevented via configuration)
     */
    public static class DefaultImpl extends HandlerInstantiator
    {
        @Override
        public JsonDeserializer<?> deserializerInstance(DeserializationConfig config,
                Annotated annotated, Class<JsonDeserializer<?>> deserClass)
        {
            return ClassUtil.createInstance(deserClass, config.canOverrideAccessModifiers());
        }

        @Override
        public JsonSerializer<?> serializerInstance(SerializationConfig config,
                Annotated annotated, Class<JsonSerializer<?>> serClass)
        {
            return ClassUtil.createInstance(serClass, config.canOverrideAccessModifiers());
        }

        @Override
        public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config,
                Annotated annotated, Class<? extends TypeResolverBuilder<?>> builderClass)
        {
            return ClassUtil.createInstance(builderClass, config.canOverrideAccessModifiers());
        }

        @Override
        public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config,
                Annotated annotated, Class<? extends TypeIdResolver> resolverClass)
        {
            return ClassUtil.createInstance(resolverClass, config.canOverrideAccessModifiers());
        }
    }
}
