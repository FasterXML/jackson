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
}
