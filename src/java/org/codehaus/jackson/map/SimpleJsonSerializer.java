package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;

/**
 * Abstract class that defines API for "simple" Serializers (as different from
 * "regular" ones, {@link JsonSerializer}) used by {@link JavaTypeMapper} (and
 * other chained {@link JsonSerializer}s too) for serializing
 * entries in certain special cases, such as value being null.
 * Specifically, entries are essentially literals/constants, since no
 * value entry is passed to serialization method.
 */
public abstract class SimpleJsonSerializer
{
    /**
     * Method that can be called to ask implementation to serialize
     * entries serializer handles.
     */
    public abstract void serialize(JsonGenerator jgen)
        throws IOException, JsonGenerationException;
}
