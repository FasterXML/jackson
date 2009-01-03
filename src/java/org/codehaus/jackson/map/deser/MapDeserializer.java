package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonDeserializationContext;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.type.*;

/**
 * Basic serializer that can take Json "Object" structure and
 * construct a {@link java.util.Map} instance
 */
public class MapDeserializer
    extends StdDeserializer<Map<?,?>>
{
    final Class<Map<Object,Object>> _mapClass;

    /**
     * Key deserializer used, if not null. If null, String from json
     * content is used as is.
     */
    final KeyDeserializer _keyDeserializer;

    /**
     * Value deserializer.
     */
    final JsonDeserializer<Object> _valueDeserializer;

    public MapDeserializer(MapType mapType, KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser)
    {
        _mapClass = mapType.asMapClass();
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
    }

    public Map<?,?> deserialize(JsonParser jp, JsonDeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw mappingException(jp, _mapClass);
        }

        Map<Object,Object> result;
        try {
            result = _mapClass.newInstance();
        } catch (Exception e) {
            throw instantiationException(jp, _mapClass, e);
        }
        @SuppressWarnings("unused")
		JsonToken t;
        KeyDeserializer keyDes = _keyDeserializer;
        JsonDeserializer<Object> valueDes = _valueDeserializer;

        while ((t = jp.nextToken()) != JsonToken.END_OBJECT) {
            // Must point to field name
            String fieldName = jp.getCurrentName();
            Object key = (keyDes == null) ? fieldName : keyDes.deserializeKey(fieldName, ctxt);
            // And then the value...
            jp.nextToken();
            Object value = valueDes.deserialize(jp, ctxt);
            /* !!! 23-Dec-2008, tatu: should there be an option to verify
             *   that there are no duplicate field names? (and/or what
             *   to do, keep-first or keep-last)
             */
            result.put(key, value);
        }
        return result;
    }
}

