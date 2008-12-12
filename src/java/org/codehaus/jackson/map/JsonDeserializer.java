package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;

/**
 * Abstract class that defines API used by {@link JavaTypeMapper} (and
 * other chained {@link JsonDeserializer}s too) to deserialize Objects of
 * arbitrary types from JSON, using provided {@link JsonParser}.
 */
public abstract class JsonDeserializer<T>
{
    /**
     * Method that can be called to ask implementation to deserialize
     * json content into the value type this serializer handles.
     *
     * @param jp Parsed used for reading Json content
     * @param provider Provider that can be used to get serializers for
     *   serializing Objects value contains, if any.
     *
     * @return Deserializer value
     */
    public abstract T deserialize(JsonParser jp, JsonDeserializerProvider provider)
        throws IOException, JsonParseException;
}
