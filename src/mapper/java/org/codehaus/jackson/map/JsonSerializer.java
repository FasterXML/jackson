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
     * Alternate serialization method that is to work like {@link #serialize} except
     * that if results are output as a JSON Object, START_OBJECT/END_OBJECT
     * markers are not to be output. Note that this only affects this serializer,
     * and not recursive calls serializer may make.
     *<p>
     * Default implementation simply calls {@link #serialize}; sub-classes that would
     * output
     * 
     * @param value Value to serialize; can <b>not</b> be null.
     * @param jgen Generator used to output resulting Json content
     * @param provider Provider that can be used to get serializers for
     *   serializing Objects value contains, if any.
     */
    public void serializeFields(T value, JsonGenerator jgen, SerializerProvider provider)
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
     * annotation {@link org.codehaus.jackson.map.annotate.JsonSerialize}
     * (and deprecated {@link org.codehaus.jackson.annotate.JsonUseSerializer}).
     */
    public abstract static class None
        extends JsonSerializer<Object> { }
}
