package org.codehaus.jackson.map.jsontype;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

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

    /*
    *************************************************************
    * Serialization methods
    *************************************************************
     */

    /**
     * Method called before properties of serialize Objects are written,
     * to add whatever type information should be added before
     * properties. This includes opening START_ELEMENT.
     */
    public abstract void writeTypePrefix(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException;

    /**
     * Method called after properties of serialize Objects have been written,
     * to add whatever type information should be added after
     * properties. This includes closing END_ELEMENT.
     */
    public abstract void writeTypeSuffix(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException;
    
    /*
     *************************************************************
     * Deserialization methods
     *************************************************************
      */
    
}

