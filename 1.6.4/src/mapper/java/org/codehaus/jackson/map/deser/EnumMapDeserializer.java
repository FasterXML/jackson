package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.TypeDeserializer;

/**
 * 
 * <p>
 * Note: casting within this class is all messed up -- just could not figure out a way
 * to properly deal with recursive definition of "EnumMap<K extends Enum<K>, V>
 * 
 * @author tsaloranta
 */
public final class EnumMapDeserializer
    extends StdDeserializer<EnumMap<?,?>>
{
    final EnumResolver<?> _enumResolver;

    final JsonDeserializer<Object> _valueDeserializer;

    public EnumMapDeserializer(EnumResolver<?> enumRes, JsonDeserializer<Object> valueDes)
    {
        super(EnumMap.class);
        _enumResolver = enumRes;
        _valueDeserializer = valueDes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EnumMap<?,?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw ctxt.mappingException(EnumMap.class);
        }
        EnumMap result = constructMap();

        while ((jp.nextToken()) != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            Enum<?> key = _enumResolver.findEnum(fieldName);
            if (key == null) {
                throw ctxt.weirdStringException(_enumResolver.getEnumClass(), "value not one of declared Enum instance names");
            }
            // And then the value...
            JsonToken t = jp.nextToken();
            /* note: MUST check for nulls separately: deserializers will
             * not handle them (and maybe fail or return bogus data)
             */
            Object value = (t == JsonToken.VALUE_NULL) ?
                null :  _valueDeserializer.deserialize(jp, ctxt);
            result.put(key, value);
        }
        return result;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(jp, ctxt);
    }
    
    @SuppressWarnings("unchecked") 
    private EnumMap<?,?> constructMap()
    {
        Class<? extends Enum<?>> enumCls = _enumResolver.getEnumClass();
    	return new EnumMap(enumCls);
    }
}

