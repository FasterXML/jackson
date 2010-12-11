package org.codehaus.jackson.map;

import org.codehaus.jackson.map.introspect.AnnotatedMember;

/**
 * Add-on interface that {@link JsonSerializer}s can implement to get a callback
 * that can be used to create contextual instances of serializer to use for
 * handling properties of supported type. This can be useful
 * for serializers that can be configured by annotations, or should otherwise
 * have differing behavior depending on what kind of property is being serialized.
 *
 * @param <T> Type of serializer to contextualize
 * 
 * @since 1.7
 */
public interface ContextualSerializer<T>
{
    /**
     * Method called to see if a different (or differently configured) serializer
     * is needed to serialize values of specified property.
     * Note that instance that this method is called on is typically shared one and
     * as a result method should <b>NOT</b> modify this instance but rather construct
     * and return a new instance. This instance should only be returned as-is, in case
     * it is already suitable for use.
     * 
     * @param config Current serialization configuration
     * @param property Method or field that represents the property
     *   (and is used to access value to serialize).
     *   Should be available; but there may be cases where caller can not provide it and
     *   null is passed instead (in which case impls usually pass 'this' serializer as is)
     * @param propertyName Logical name of the property, if available (null if not; mostly
     *   just in cases where 'property' is also null)
     * 
     * @return Serializer to use for serializing values of specified property;
     *   may be this instance or a new instance.
     * 
     * @throws JsonMappingException
     */
    public JsonSerializer<T> createContextual(SerializationConfig config,
            AnnotatedMember property, String propertyName)
        throws JsonMappingException;
}
