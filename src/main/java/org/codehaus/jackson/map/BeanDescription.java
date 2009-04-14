package org.codehaus.jackson.map;

import java.util.LinkedHashMap;

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
    
    public abstract LinkedHashMap<String,AnnotatedMethod> findGetters(boolean autodetect);
}
