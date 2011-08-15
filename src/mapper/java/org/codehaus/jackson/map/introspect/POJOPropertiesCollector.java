package org.codehaus.jackson.map.introspect;

import java.util.*;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.type.JavaType;

/**
 * Helper class used for aggregating information about all possible
 * properties of a POJO.
 * 
 * @since 1.9
 */
public class POJOPropertiesCollector
{
    protected final HashMap<String, POJOPropertyCollector> _properties = new HashMap<String, POJOPropertyCollector>();
    
    public POJOPropertiesCollector() { }

    public void collectFor(JavaType type, MapperConfig<?> config)
    {
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
    }
}
