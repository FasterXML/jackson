package org.codehaus.jackson.map;

import java.util.*;

import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.type.JavaType;

/**
 * Basic container for information gathered by {@link ClassIntrospector} to
 * help in constructing serializers and deserializers.
 * 
 * @author tatu
 */
public abstract class BeanDescription
{
    /*
    /******************************************************
    /* Configuration
    /******************************************************
     */

    /**
     * Bean type information, including raw class and possible
     * * generics information
     */
    protected final JavaType _type;

    /*
    /******************************************************
    /* Life-cycle
    /******************************************************
     */

    protected BeanDescription(JavaType type)
    {
    	_type = type;
    }

    /*
    /******************************************************
    /* Simple accesors
    /******************************************************
     */

    /**
     * Method for accessing declared type of bean being introspected,
     * including full generic type information (from declaration)
     */
    public JavaType getType() { return _type; }

    public Class<?> getBeanClass() { return _type.getRawClass(); }
   
    /*
    /******************************************************
    /* Basic API
    /******************************************************
     */

    /**
     * @param visibilityChecker Object that determines whether
     *    methods have enough visibility to be auto-detectable as getters
     * @param ignoredProperties (optional, may be null) Names of properties
     *   to ignore; getters for these properties are not to be returned.
     *   
     * @return Ordered Map with logical property name as key, and
     *    matching getter method as value.
     */
    public abstract LinkedHashMap<String,AnnotatedMethod> findGetters(VisibilityChecker<?> visibilityChecker,
            Collection<String> ignoredProperties);

    /**
     * @param vchecker (optional) Object that determines whether specific methods
     *   have enough visibility to be considered as auto-detectable setters.
     *   If null, auto-detection is disabled
     * 
     * @return Ordered Map with logical property name as key, and
     *    matching setter method as value.
     */
    public abstract LinkedHashMap<String,AnnotatedMethod> findSetters(VisibilityChecker<?> vchecker);
}
