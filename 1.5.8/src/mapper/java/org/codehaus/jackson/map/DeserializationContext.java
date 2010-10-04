package org.codehaus.jackson.map;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.util.ArrayBuilders;
import org.codehaus.jackson.map.util.ObjectBuffer;
import org.codehaus.jackson.type.JavaType;

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
    /************************************************************
    /* Configuration methods
    /************************************************************
     */

    /**
     * Method for accessing configuration setting object for
     * currently active deserialization.
     */
    public DeserializationConfig getConfig() { return _config; }

    /**
     * Returns provider that can be used for dynamically locating
     * other deserializers during runtime.
     * 
     * @since 1.5
     */
    public DeserializerProvider getDeserializerProvider() {
        // will be overridden by impl class
        return null;
    }
    
    /**
     * Convenience method for checking whether specified on/off
     * feature is enabled
     */
    public boolean isEnabled(DeserializationConfig.Feature feat) {
    	return _config.isEnabled(feat);
    }

    /**
     * Convenience method for accessing the default Base64 encoding
     * used for decoding base64 encoded binary content.
     * Same as calling:
     *<pre>
     *  getConfig().getBase64Variant();
     *</pre>
     */
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
    // Methods for problem handling, reporting
    //////////////////////////////////////////////////////////////
    */

    /**
     * Method deserializers can call to inform configured {@link DeserializationProblemHandler}s
     * of an unrecognized property.
     * 
     * @return True if there was a configured problem handler that was able to handle the
     *   proble
     * 
     * @since 1.5
     */
    public abstract boolean handleUnknownProperty(JsonParser jp, JsonDeserializer<?> deser, Object instanceOrClass, String propName)
        throws IOException, JsonProcessingException;

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

    public abstract JsonMappingException instantiationException(Class<?> instClass, String msg);
    
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
     * Helper method for indicating that the current token was expected to be another
     * token.
     */
    public abstract JsonMappingException wrongTokenException(JsonParser jp, JsonToken expToken, String msg);
    
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

    /**
     * Helper method for constructing exception to indicate that given
     * type id (parsed from JSON) could not be converted to a Java type.
     * 
     * @since 1.5
     */
    public abstract JsonMappingException unknownTypeException(JavaType baseType, String id);
}
