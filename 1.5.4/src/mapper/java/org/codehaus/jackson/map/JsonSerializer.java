package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;

/**
 * Abstract class that defines API used by {@link ObjectMapper} (and
 * other chained {@link JsonSerializer}s too) to serialize Objects of
 * arbitrary types into JSON, using provided {@link JsonGenerator}.
 */
public abstract class JsonSerializer<T>
{
    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles.
     *
     * @param value Value to serialize; can <b>not</b> be null.
     * @param jgen Generator used to output resulting Json content
     * @param provider Provider that can be used to get serializers for
     *   serializing Objects value contains, if any.
     */
    public abstract void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException;

    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles, using specified type serializer
     * for embedding necessary type information.
     *<p>
     * Default implementation will ignore serialization of type information,
     * and just calls {@link #serialize}: serializers that can embed
     * type information should override this to implement actual handling.
     *
     * @param value Value to serialize; can <b>not</b> be null.
     * @param jgen Generator used to output resulting Json content
     * @param provider Provider that can be used to get serializers for
     *   serializing Objects value contains, if any.
     * @param typeSer Type serializer to use for including type information
     *
     * @since 1.5
     */
    public void serializeWithType(T value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        serialize(value, jgen, provider);
    }
    
    /*
    ************************************************************
    * Introspection methods needed for type handling 
    ************************************************************
     */
    
    /**
     * Method for accessing type of Objects this serializer can handle.
     *<p>
     * Default implementation will return null, which essentially means
     * same as returning <code>Object.class</code> would; that is, that
     * nothing is known about handled type.
     */
    public Class<T> handledType() { return null; }
    
    /*
    //////////////////////////////////////////////////////
    // Helper class(es)
    //////////////////////////////////////////////////////
     */

    /**
     * This marker class is only to be used with annotations, to
     * indicate that <b>no serializer is configured</b>.
     *<p>
     * Specifically, this class is to be used as the marker for
     * annotation {@link org.codehaus.jackson.map.annotate.JsonSerialize}.
     */
    public abstract static class None
        extends JsonSerializer<Object> { }
}
