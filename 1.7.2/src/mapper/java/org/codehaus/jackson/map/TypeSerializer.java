package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;

/**
 * Interface for serializing type information regarding instances of specified
 * base type (super class), so that exact subtype can be properly deserialized
 * later on. These instances are to be called by regular
 * {@link org.codehaus.jackson.map.JsonSerializer}s using proper contextual
 * calls, to add type information using mechanism type serializer was
 * configured with.
 * 
 * @since 1.5
 * @author tatus
 */
public abstract class TypeSerializer
{
    /*
     **********************************************************
     * Introspection
     **********************************************************
     */

    /**
     * Accessor for type information inclusion method
     * that serializer uses; indicates how type information
     * is embedded in resulting JSON.
     */
    public abstract JsonTypeInfo.As getTypeInclusion();

    /**
     * Name of property that contains type information, if
     * property-based inclusion is used.
     */
    public abstract String getPropertyName();
    
    /**
     * Accessor for object that handles conversions between
     * types and matching type ids.
     */
    public abstract TypeIdResolver getTypeIdResolver();
    
    /*
     **********************************************************
     * Type serialization methods
     **********************************************************
     */
    
    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as scalar JSON value (not as JSON
     * Object or Array).
     * This means that the context after call can not be that of JSON Object;
     * it may be Array or root context.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     */
    public abstract void writeTypePrefixForScalar(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException;

    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as JSON Object value (not as JSON
     * Array or scalar).
     * This means that context after call must be JSON Object, meaning that
     * caller can then proceed to output field entries.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     */
    public abstract void writeTypePrefixForObject(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException;

    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as JSON Array value (not as JSON
     * Object or scalar).
     * This means that context after call must be JSON Array, that is, there
     * must be an open START_ARRAY to write contents in.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     */
    public abstract void writeTypePrefixForArray(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException;
    
    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypePrefixForScalar}.
     * It needs to write closing END_OBJECT marker, and any other decoration
     * that needs to be matched.
     */
    public abstract void writeTypeSuffixForScalar(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException;

    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypePrefixForObject}.
     * It needs to write closing END_OBJECT marker, and any other decoration
     * that needs to be matched.
     */
    public abstract void writeTypeSuffixForObject(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException;

    public abstract void writeTypeSuffixForArray(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException;
}
