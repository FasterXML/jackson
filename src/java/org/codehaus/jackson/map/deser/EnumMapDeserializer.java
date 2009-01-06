package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.type.*;

public final class EnumMapDeserializer
    extends StdDeserializer<EnumMap<?,?>>
{
    final EnumResolver _keyResolver;

    final JsonDeserializer<Object> _valueDeserializer;

    public EnumMapDeserializer(EnumResolver keyRes, JsonDeserializer<Object> valueDes)
    {
        _keyResolver = keyRes;
        _valueDeserializer = valueDes;
    }

    public EnumMap<?,?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw ctxt.mappingException(EnumMap.class);
        }

        // !!! TBI
        return null;
    }
}

