package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;

/**
 * Interface for resolve Java type (class) into JSON type information, written
 * in JSON content. It acts as sort of serializer decorator, but due to special
 * handling needed for some type serialization schemes (specifically, ones that
 * modify JSON field names based on type), can not be implemented as plain
 * {@link JsonSerializer} decorator.
 * 
 * @since 1.5
 * @author tatus
 */
public abstract class TypeSerializer
{
    /**
     * Method called to serialize given Object as a JSON object field (with suggested field
     * name), using given underlying serializer for serializing data, and whatever type
     * resolution mechanism this serializer uses.
     */
    public abstract void serializeAsField(Object value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser, String fieldName)
        throws IOException, JsonProcessingException;

    /**
     * Method called to serializer given Object as JSON Value (but not as a field value)
     */
    public abstract void serializeAsValue(Object value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser)
        throws IOException, JsonProcessingException;
}
