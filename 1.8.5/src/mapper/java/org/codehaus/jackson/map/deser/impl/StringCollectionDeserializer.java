package org.codehaus.jackson.map.deser.impl;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.annotate.JacksonStdImpl;
import org.codehaus.jackson.map.deser.ContainerDeserializer;
import org.codehaus.jackson.type.JavaType;

@JacksonStdImpl
public final class StringCollectionDeserializer
    extends ContainerDeserializer<Collection<String>>
{
    // // Configuration

    protected final JavaType _collectionType;

    /**
     * Value deserializer; needed even if it is the standard String
     * deserializer
     */
    protected final JsonDeserializer<String> _valueDeserializer;

    /**
     * Flag that indicates whether value deserializer is the standard
     * Jackson-provided one; if it is, we can use more efficient
     * handling.
     */
    protected final boolean _isDefaultDeserializer;

    /**
     * We will use the default constructor of the collection class for 
     * instantiating result.
     */
    final Constructor<Collection<String>> _defaultCtor;
    
    @SuppressWarnings("unchecked")
    public StringCollectionDeserializer(JavaType collectionType, JsonDeserializer<?> valueDeser,
            Constructor<?> ctor)
    {
        super(collectionType.getRawClass());
        _collectionType = collectionType;
        _valueDeserializer = (JsonDeserializer<String>) valueDeser;
        _defaultCtor = (Constructor<Collection<String>>) ctor;
        _isDefaultDeserializer = isDefaultSerializer(valueDeser);
    }

    /*
    /**********************************************************
    /* ContainerDeserializer API
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _collectionType.getContentType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonDeserializer<Object> getContentDeserializer() {
        JsonDeserializer<?> deser = _valueDeserializer;
        return (JsonDeserializer<Object>) deser;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */
    
    @Override
    public Collection<String> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        Collection<String> result;
        try {
            result = (Collection<String>) _defaultCtor.newInstance();
        } catch (Exception e) {
            throw ctxt.instantiationException(_collectionType.getRawClass(), e);
        }
        return deserialize(jp, ctxt, result);
    }

    @Override
    public Collection<String> deserialize(JsonParser jp, DeserializationContext ctxt,
                                          Collection<String> result)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_ARRAY
        if (!jp.isExpectedStartArrayToken()) {
            return handleNonArray(jp, ctxt, result);
        }

        if (!_isDefaultDeserializer) {
            return deserializeUsingCustom(jp, ctxt, result);
        }
        JsonToken t;

        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            result.add((t == JsonToken.VALUE_NULL) ? null : jp.getText());
        }
        return result;
    }
    
    private Collection<String> deserializeUsingCustom(JsonParser jp, DeserializationContext ctxt,
            Collection<String> result)
        throws IOException, JsonProcessingException
    {
        JsonToken t;
        final JsonDeserializer<String> deser = _valueDeserializer;

        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            String value;

            if (t == JsonToken.VALUE_NULL) {
                value = null;
            } else {
                value = deser.deserialize(jp, ctxt);
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

    /**
     * Helper method called when current token is no START_ARRAY. Will either
     * throw an exception, or try to handle value as if member of implicit
     * array, depending on configuration.
     */
    private final Collection<String> handleNonArray(JsonParser jp, DeserializationContext ctxt,
            Collection<String> result)
        throws IOException, JsonProcessingException
    {
        // [JACKSON-526]: implicit arrays from single values?
        if (!ctxt.isEnabled(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
            throw ctxt.mappingException(_collectionType.getRawClass());
        }
        // Strings are one of "native" (intrinsic) types, so there's never type deserializer involved
        JsonDeserializer<String> valueDes = _valueDeserializer;
        JsonToken t = jp.getCurrentToken();

        String value;
        
        if (t == JsonToken.VALUE_NULL) {
            value = null;
        } else {
            value = (valueDes == null) ? jp.getText() : valueDes.deserialize(jp, ctxt);
        }
        result.add(value);
        return result;
    }
    
}
