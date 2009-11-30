package org.codehaus.jackson.map;

import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.util.ArrayBuilders;
import org.codehaus.jackson.map.util.ObjectBuffer;

/**
 * Context for deserialization process. Used to allow passing in configuration
 * settings and reusable temporary objects (scrap arrays, containers).
 */
public abstract class DeserializationContext
{
    protected final DeserializationConfig _config;
    
    protected DeserializationContext(DeserializationConfig config)
    {
        _config = config;
    }

    /*
    //////////////////////////////////////////////////////////////
    // Config methods
    //////////////////////////////////////////////////////////////
    */

    public DeserializationConfig getConfig() { return _config; }

    /**
     * Convenience method for checking whether specified on/off
     * feature is enabled
     */
    public boolean isEnabled(DeserializationConfig.Feature feat) {
    	return _config.isEnabled(feat);
    }

    public Base64Variant getBase64Variant() {
        return _config.getBase64Variant();
    }

    /**
     * Accessor for getting access to the underlying JSON parser used
     * for deserialization.
     */
    public abstract JsonParser getParser();

    /*
    //////////////////////////////////////////////////////////////
    // Methods for accessing reusable/recyclable helper objects
    //////////////////////////////////////////////////////////////
    */

    /**
     * Method that can be used to get access to a reusable ObjectBuffer,
     * useful for efficiently constructing Object arrays and Lists.
     * Note that leased buffers should be returned once deserializer
     * is done, to allow for reuse during same round of deserialization.
     */
    public abstract ObjectBuffer leaseObjectBuffer();

    /**
     * Method to call to return object buffer previously leased with
     * {@link #leaseObjectBuffer}.
     * 
     * @param buf Returned object buffer
     */
    public abstract void returnObjectBuffer(ObjectBuffer buf);

    /**
     * Method for accessing object useful for building arrays of
     * primitive types (such as int[]).
     */
    public abstract ArrayBuilders getArrayBuilders();

    /*
    //////////////////////////////////////////////////////////////
    // Parsing methods that may use reusable/-cyclable objects
    //////////////////////////////////////////////////////////////
    */

    public abstract java.util.Date parseDate(String dateStr)
        throws IllegalArgumentException;

    public abstract Calendar constructCalendar(Date d);

    /*
    //////////////////////////////////////////////////////////////
    // Methods for constructing exceptions
    //////////////////////////////////////////////////////////////
    */

    /**
     * Helper method for constructing generic mapping exception for specified type
     */
    public abstract JsonMappingException mappingException(Class<?> targetClass);

    /**
     * Helper method for constructing instantiation exception for specified type,
     * to indicate problem with physically constructing instance of
     * specified class (missing constructor, exception from constructor)
     */
    public abstract JsonMappingException instantiationException(Class<?> instClass, Exception e);
    
    /**
     * Helper method for constructing exception to indicate that input JSON
     * String was not in recognized format for deserializing into given type.
     */
    public abstract JsonMappingException weirdStringException(Class<?> instClass, String msg);

    /**
     * Helper method for constructing exception to indicate that input JSON
     * Number was not suitable for deserializing into given type.
     */
    public abstract JsonMappingException weirdNumberException(Class<?> instClass, String msg);

    /**
     * Helper method for constructing exception to indicate that given JSON
     * Object field name was not in format to be able to deserialize specified
     * key type.
     */
    public abstract JsonMappingException weirdKeyException(Class<?> keyClass, String keyValue, String msg);

    /**
     * Helper method for constructing exception to indicate that JSON Object
     * field name did not map to a known property of type being
     * deserialized.
     * 
     * @param instanceOrClass Either value being populated (if one has been
     *   instantiated), or Class that indicates type that would be (or
     *   have been) instantiated
     */
    public abstract JsonMappingException unknownFieldException(Object instanceOrClass, String fieldName);
}
