package org.codehaus.jackson.map;

import org.codehaus.jackson.type.JavaType;

/**
 * Interface that defines API for simple extensions that can provide additional serializers
 * for various types. Access is by a single callback method; instance is to either return
 * a configured {@link JsonSerializer} for specified type, or null to indicate that it
 * does not support handling of the type. In latter case, further calls can be made
 * for other providers; in former case returned serializer is used for handling of
 * instances of specified type.
 * 
 * @since 1.7
 */
public interface Serializers
{
    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified type. Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     * 
     * @param type Fully resolved type of instances to serialize
     * @param config Serialization configuration in use
     * @param beanDesc Additional information about type; will always be of type
     *    {@link org.codehaus.jackson.map.introspect.BasicBeanDescription} (that is,
     *    safe to cast to this more specific type)
     * @param property Property that contains values to serialize
     *    
     * @return Configured serializer to use for the type; or null if implementation
     *    does not recognize or support type
     */
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc, BeanProperty property);
}
