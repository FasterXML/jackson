package org.codehaus.jackson.map.deser;

/**
 * Base class for deserializers that handle types that are serialized
 * as JSON scalars (non-structured, i.e. non-Object, non-Array, values).
 * 
 * @author tatu
 *
 * @param <T>
 */
public abstract class StdScalarDeserializer<T> extends StdDeserializer<T>
{
    protected StdScalarDeserializer(Class<?> vc) {
        super(vc);
    } 
}
