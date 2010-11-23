package org.codehaus.jackson.map;

import org.codehaus.jackson.type.JavaType;

/**
 * Interface that defines API for simple extensions that can provide additional deserializers
 * for various types. Access is by a single callback method; instance is to either return
 * a configured {@link JsonDeserializer} for specified type, or null to indicate that it
 * does not support handling of the type. In latter case, further calls can be made
 * for other providers; in former case returned deserializer is used for handling of
 * instances of specified type.
 * 
 * @since 1.7
 */
public interface Deserializers
{
    public JsonDeserializer<?> findDeserializer(JavaType type);
}
