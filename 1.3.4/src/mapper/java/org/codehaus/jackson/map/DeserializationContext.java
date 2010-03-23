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

    public boolean isEnabled(DeserializationConfig.Feature feat) {
    	return _config.isEnabled(feat);
    }

    public Base64Variant getBase64Variant() {
        return _config.getBase64Variant();
    }

    public abstract JsonParser getParser();

    /*
    //////////////////////////////////////////////////////////////
    // Methods for accessing reusable/recyclable helper objects
    //////////////////////////////////////////////////////////////
    */

    /**
     * Method that can be used to get access to a reusable ObjectBuffer,
     * useful for constructing Object arrays and Lists.
     */
    public abstract ObjectBuffer leaseObjectBuffer();

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

    public abstract JsonMappingException mappingException(Class<?> targetClass);
    public abstract JsonMappingException instantiationException(Class<?> instClass, Exception e);
    
    public abstract JsonMappingException weirdStringException(Class<?> instClass, String msg);
    public abstract JsonMappingException weirdNumberException(Class<?> instClass, String msg);

    public abstract JsonMappingException weirdKeyException(Class<?> keyClass, String keyValue, String msg);

    /**
     * @param instanceOrClass Either value being populated (if one has been
     *   instantiated), or Class that indicates type that would be (or
     *   have been) instantiated
     */
    public abstract JsonMappingException unknownFieldException(Object instanceOrClass, String fieldName);
}
