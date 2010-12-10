package org.codehaus.jackson.map;

import org.codehaus.jackson.map.introspect.AnnotatedMember;

/**
 * Add-on interface that {@link JsonDeserializer}s can implement to get a callback
 * that can be used to create contextual instances of deserializer to use for
 * handling properties of supported type. This can be useful
 * for deserializers that can be configured by annotations, or should otherwise
 * have differing behavior depending on what kind of property is being deserialized.
 *
 * @param <T> Type of deserializer to contextualize
 * 
 * @since 1.7
 */
public interface ContextualDeserializer<T>
{
    /**
     * Method called to see if a different (or differently configured) deserializer
     * is needed to deserializer values of specified property.
     * Note that instance that this method is called on is typically shared one and
     * as a result method should <b>NOT</b> modify this instance but rather construct
     * and return a new instance. This instance should only be returned as-is, in case
     * it is already suitable for use.
     * 
     * @param property Method, field or constructor parameter that represents the property
     *   (and is used to assign deserialized value); contained within <code>contextType</code>.
     *   Should be available; but there may be cases where caller can not provide it (in
     *   which case implementations typically just return 'this')
     * @param propertyName Logical name of the property, if available (null if not; mostly
     *   just in cases where 'property' is also null)
     * 
     * @return Deserializer to use for deserializing values of specified property;
     *   may be this instance or a new instance.
     * 
     * @throws JsonMappingException
     */
    public JsonDeserializer<T> createContextual(AnnotatedMember property, String propertyName)
        throws JsonMappingException;
}
