package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.util.ClassUtil;

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

    /**
     * If element instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    final TypeDeserializer _valueTypeDeserializer;

    /**
     * We will use the default constructor of the class for
     * instantiation
     */
    final Constructor<Collection<Object>> _defaultCtor;
    
    @Deprecated
    public CollectionDeserializer(Class<?> collectionClass, JsonDeserializer<Object> valueDeser)
    {
        this(collectionClass, valueDeser, null);
    }

    @SuppressWarnings("unchecked")
    public CollectionDeserializer(Class<?> collectionClass, JsonDeserializer<Object> valueDeser,
            TypeDeserializer valueTypeDeser)
    {
        this(collectionClass, valueDeser, valueTypeDeser,
             ClassUtil.findConstructor((Class<Collection<Object>>) collectionClass, true));
    }

    @SuppressWarnings("unchecked")
    public CollectionDeserializer(Class<?> collectionClass, JsonDeserializer<Object> valueDeser,
                                  TypeDeserializer valueTypeDeser,
                                  Constructor<Collection<Object>> ctor)
    {
        super(collectionClass);
        _collectionClass = (Class<Collection<Object>>) collectionClass;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = valueTypeDeser;
        _defaultCtor = ctor;
    }

    @Override
    public Collection<Object> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        Collection<Object> result;
        try {
            result = _defaultCtor.newInstance();
        } catch (Exception e) {
            throw ctxt.instantiationException(_collectionClass, e);
        }
        return deserialize(jp, ctxt, result);
    }

    @Override
    public Collection<Object> deserialize(JsonParser jp, DeserializationContext ctxt,
                                          Collection<Object> result)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_ARRAY
        if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
            throw ctxt.mappingException(_collectionClass);
        }

        JsonDeserializer<Object> valueDes = _valueDeserializer;
        JsonToken t;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            Object value;
            
            if (t == JsonToken.VALUE_NULL) {
                value = null;
            } else if (typeDeser == null) {
                value = valueDes.deserialize(jp, ctxt);
            } else {
                value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
            }
            result.add(value);
        }
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromArray(jp, ctxt);
    }
}
