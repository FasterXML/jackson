package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.type.JavaType;

/**
 * Intermediate base deserializer class that adds more shared accessor
 * so that other classes can access information about contained (value)
 * types
 * 
 * @since 1.6
 */
public abstract class ContainerDeserializer<T>
    extends StdDeserializer<T>
{
    protected ContainerDeserializer(Class<?> selfType)
    {
        super(selfType);
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Accessor for declared type of contained value elements; either exact
     * type, or one of its supertypes.
     */
    public abstract JavaType getContentType();

    /**
     * Accesor for deserialized use for deserializing content values.
     */
    public abstract JsonDeserializer<Object> getContentDeserializer();
}
