package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Deserializer only used for abstract types used as placeholders during polymorphic
 * type handling deserialization. If so, there is no real deserializer associated
 * with nominal type, just {@link TypeDeserializer}; and any calls that do not
 * pass such resolver will result in an error.
 * 
 * @author tatu
 * 
 * @since 1.6
 */
public class AbstractDeserializer
    extends JsonDeserializer<Object>
{
    protected final JavaType _baseType;
    
    public AbstractDeserializer(JavaType bt)
    {
        _baseType = bt;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // should we check that type is as expected?
        return typeDeserializer.deserializeTypedFromObject(jp, ctxt);
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        // no can do:
        throw ctxt.instantiationException(_baseType.getClass(), "abstract types can only be instantiated with additional type information");
    }
}
