package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.DeserializationContext;

/**
 * Basic serializer that can take Json "Array" structure and
 * construct a {@link java.util.Collection} instance, with typed contents.
 *<p>
 * Note: for untyped content (one indicated by passing Object.class
 * as the type), {@link UntypedObjectDeserializer} is used instead.
 * It can also construct {@link java.util.List}s, but not with specific
 * POJO types, only other containers and primitives/wrappers.
 */
public class CollectionDeserializer
    extends StdDeserializer<Collection<Object>>
{
    // // Configuration

    final Class<Collection<Object>> _collectionClass;

    /**
     * Value deserializer.
     */
    final JsonDeserializer<Object> _valueDeserializer;

    @SuppressWarnings("unchecked") 
    public CollectionDeserializer(Class<?> collectionClass, JsonDeserializer<Object> valueDeser)
    {
        super(Collection.class);
        _collectionClass = (Class<Collection<Object>>) collectionClass;
        _valueDeserializer = valueDeser;
    }

    public Collection<Object> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_ARRAY
        if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
            throw ctxt.mappingException(_collectionClass);
        }

        /* !!! 09-Jan-2009, tatu: Use temp array from context, to create
         *    list etc of suitable size?
         */
        Collection<Object> result;
        try {
            result = _collectionClass.newInstance();
        } catch (Exception e) {
            throw ctxt.instantiationException(_collectionClass, e);
        }
        JsonDeserializer<Object> valueDes = _valueDeserializer;
        JsonToken t;

        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            Object value = (t == JsonToken.VALUE_NULL) ? null : valueDes.deserialize(jp, ctxt);
            result.add(value);
        }
        return result;
    }
}
