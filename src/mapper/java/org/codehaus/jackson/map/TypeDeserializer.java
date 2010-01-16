package org.codehaus.jackson.map;

import org.codehaus.jackson.map.annotate.JsonTypeInfo;

/**
 * Interface for deserializing type information from JSON content, to
 * type-safely deserialize data into correct polymorphic instance
 * (when type inclusion has been enabled for type handled).
 * 
 * @since 1.5
 * @author tatus
 */
public abstract class TypeDeserializer
{
    /*
    /**********************************************************
    /* Introspection
    /**********************************************************
     */

    /**
     * Accessor for type information inclusion method
     * that deserializer uses; indicates how type information
     * is (expected to be) embedded in JSON input.
     */
    public abstract JsonTypeInfo.As getTypeInclusion();

    /**
     * Accessor for type information id method, what is the
     * serialization method for embedded type information.
     */
    public abstract JsonTypeInfo.Id getTypeId();

    /**
     * Name of property that contains type information, if
     * property-based inclusion is used.
     */
    public abstract String propertyName();
    
    /*
    /*********************************************************
    /* Type deserialization methods
    /**********************************************************
     */
}
    