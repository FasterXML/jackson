package org.codehaus.jackson.map;

import java.util.*;

import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.type.JavaType;

/**
 * Basic container for information gathered by {@link ClassIntrospector} to
 * help in constructing serializers and deserializers.
 * 
 * @author tsaloranta
 */
public abstract class BeanDescription
{
    /*
    ///////////////////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////////////////
     */

    /**
     * Bean type information, including raw class and possible
     * * generics information
     */
    protected final JavaType _type;

    /*
    ///////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////
     */

    protected BeanDescription(JavaType type)
    {
    	_type = type;
    }

    /*
    ///////////////////////////////////////////////////////
    // Simple accesors
    ///////////////////////////////////////////////////////
     */

    /**
     * Method for accessing declared type of bean being introspected,
     * including full generic type information (from declaration)
     */
    public JavaType getType() { return _type; }

    public Class<?> getBeanClass() { return _type.getRawClass(); }
   
    /*
    ///////////////////////////////////////////////////////
    // Basic API
    ///////////////////////////////////////////////////////
     */

    /**
     * @param ignoredProperties (optional, may be null) Names of properties
     *   to ignore; getters for these properties are not to be returned.
     */
    public abstract LinkedHashMap<String,AnnotatedMethod> findGetters(boolean autoDetectGetters, boolean autoDetectIsGetters, Collection<String> ignoredProperties);

    public abstract LinkedHashMap<String,AnnotatedMethod> findSetters(boolean autoDetect);
}
