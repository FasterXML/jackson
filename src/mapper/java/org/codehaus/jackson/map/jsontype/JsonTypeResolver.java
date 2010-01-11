package org.codehaus.jackson.map.jsontype;

/**
 * Resolver object used for serializing additional type information into
 * JSON, to allow for deserializing polymorphic subtypes.
 * Actual type metadata included in serialized JSON can vary, so different
 * resolves modify produced JSON differently.
 * 
 * @since 1.5
 * @author tatu
 */
public abstract class JsonTypeResolver
{
    /**
     * Method to access base type of all types resolved by this
     * resolver.
     */
    public abstract Class<?> getBaseType();
}
