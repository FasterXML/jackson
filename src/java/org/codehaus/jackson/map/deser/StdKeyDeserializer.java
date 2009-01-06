package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.DeserializationContext;

/**
 * Base class for simple key deserializers.
 */
public abstract class StdKeyDeserializer
    extends KeyDeserializer
{
    public final Object deserializeKey(String key, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return null;
        //return JsonMappingException.from(_parser, "Can not deserialize "+targetClass.getName()+" out of "+_parser.getCurrentToken()+" token");
    }
}
