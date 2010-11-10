package org.codehaus.jackson.map;

import org.codehaus.jackson.type.JavaType;

/**
 * Defines interface for resolvers that can resolve abstract types into concrete
 * ones; either by using static mappings, or possibly by materializing
 * implementations dynamically.
 * 
 * @since 1.6
 */
public abstract class AbstractTypeResolver
{
    /**
     * Method called to try to resolve an abstract type into
     * concrete type, usually for purposes of deserializing.
     * 
     * @param config Deserialization configuration in use
     * @param type Abstract type (with generic type parameters if any)
     *   to resolve
     * 
     * @return Resolved concrete type (which should retain generic
     *    type parameters of input type, if any), if resolution succeeds;
     *    null if resolver does not know how to resolve type
     */
    public abstract JavaType resolveAbstractType(DeserializationConfig config, JavaType type);
}
