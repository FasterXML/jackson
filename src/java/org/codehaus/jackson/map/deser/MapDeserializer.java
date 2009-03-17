package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.KeyDeserializer;

/**
 * Basic serializer that can take Json "Object" structure and
 * construct a {@link java.util.Map} instance, with typed contents.
 *<p>
 * Note: for untyped content (one indicated by passing Object.class
 * as the type), {@link UntypedObjectDeserializer} is used instead.
 * It can also construct {@link java.util.Map}s, but not with specific
 * POJO types, only other containers and primitives/wrappers.
 */
public class MapDeserializer
    extends StdDeserializer<Map<?,?>>
{
    // // Configuration

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

    @SuppressWarnings("unchecked") 
    public MapDeserializer(Class<?> mapClass, KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser)
    {
        super(Map.class);
        _mapClass = (Class<Map<Object,Object>>) mapClass;
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
    }

    @Override
    public Map<?,?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw ctxt.mappingException(_mapClass);
        }

        Map<Object,Object> result;
        try {
            result = _mapClass.newInstance();
        } catch (Exception e) {
            throw ctxt.instantiationException(_mapClass, e);
        }
        KeyDeserializer keyDes = _keyDeserializer;
        JsonDeserializer<Object> valueDes = _valueDeserializer;

        while ((jp.nextToken()) != JsonToken.END_OBJECT) {
            // Must point to field name
            String fieldName = jp.getCurrentName();
            Object key = (keyDes == null) ? fieldName : keyDes.deserializeKey(fieldName, ctxt);
            // And then the value...
            JsonToken t = jp.nextToken();
            // Note: must handle null explicitly here; value deserializers won't
            Object value = (t == JsonToken.VALUE_NULL) ? null : valueDes.deserialize(jp, ctxt);
            /* !!! 23-Dec-2008, tatu: should there be an option to verify
             *   that there are no duplicate field names? (and/or what
             *   to do, keep-first or keep-last)
             */
            result.put(key, value);
        }
        return result;
    }
}
