package org.codehaus.jackson.map;

import java.util.*;

import org.codehaus.jackson.map.introspect.AnnotatedMethod;

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
     * Class being introspected
     */
    protected final Class<?> _class;

    /*
    ///////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////
     */

    protected BeanDescription(Class<?> forClass)
    {
    	_class = forClass;
    }

    /*
    ///////////////////////////////////////////////////////
    // Simple accesors
    ///////////////////////////////////////////////////////
     */

    public Class<?> getBeanClass() { return _class; }
   
    /*
    ///////////////////////////////////////////////////////
    // Basic API
    ///////////////////////////////////////////////////////
     */

    /**
     * @param ignoredProperties (optional, may be null) Names of properties
     *   to ignore; getters for these properties are not to be returned.
     */
    public abstract LinkedHashMap<String,AnnotatedMethod> findGetters(boolean autodetect, Collection<String> ignoredProperties);

    public abstract LinkedHashMap<String,AnnotatedMethod> findSetters(boolean autodetect);
}
