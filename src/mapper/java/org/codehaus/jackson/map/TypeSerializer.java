package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;

/**
 * Interface for serializing type information regarding instances of specified
 * base type (super class), so that exact subtype can be properly deserialized
 * later on.
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
     * Accessor for type information id method, what is the
     * serialization method for type information (i.e. what
     * is embedded in resulting JSON)
     */
    public abstract JsonTypeInfo.Id getTypeId();

    public abstract String propertyName();
    
    /*
     **********************************************************
     * Type serialization methods
     **********************************************************
     */
    
    /**
     * Method called to write initial part of type information for given
     * stand-alone value.
     * Method will be called when writing stand-alone (root-level) and
     * list/array - contained values, but not for 
     * Method is to output opening START_OBJECT, possibly followed by zero or more
     * type information entries; after which caller can append other entries.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     */
    public abstract void writeTypePrefixForValue(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException;

    /**
     * Method called to write initial part of type information for given
     * field and its value.
     * Method will be called when writing values directly contained in Maps and POJOs,
     * but not for root-level or list/array contained values.
     * Method is to output field name (usually one passed in as argument; but potentially
     * modified by type serializer) and opening START_OBJECT, possibly followed by zero or more
     * type information entries; after which caller can append other entries.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     * @param fieldName Planned field name; may be used as is by type serializer, or
     *   modified
     */
    public abstract void writeTypePrefixForField(Object value, JsonGenerator jgen, String fieldName)
        throws IOException, JsonProcessingException;
    
    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypePrefixForValue}.
     * It needs to write closing END_OBJECT marker, and any other decoration
     * that needs to be matched.
     */
    public abstract void writeTypeSuffixForValue(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException;

    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypePrefixForField}.
     * It needs to write closing END_OBJECT marker, and any other decoration
     * that needs to be matched.
     */
    public abstract void writeTypeSuffixForField(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException;
}
