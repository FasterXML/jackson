package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.TypeDeserializer;

/**
 * Base class for deserializers that handle types that are serialized
 * as JSON scalars (non-structured, i.e. non-Object, non-Array, values).
 * 
 * @author tatu
 */
public abstract class StdScalarDeserializer<T> extends StdDeserializer<T>
{
    protected StdScalarDeserializer(Class<?> vc) {
        super(vc);
    } 

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        return typeDeserializer.deserializeTypedFromScalar(jp, ctxt);
    }
}
