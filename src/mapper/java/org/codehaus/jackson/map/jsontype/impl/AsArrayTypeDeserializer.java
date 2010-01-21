package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.type.JavaType;

public class AsArrayTypeDeserializer extends TypeDeserializerBase
{
    public AsArrayTypeDeserializer(Class<?> bt, TypeConverter conv)
    {
        super(bt, conv);
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() {
        return JsonTypeInfo.As.WRAPPER_ARRAY;
    }

    /**
     * Deserializing with 'WRAPPER_ARRAY' inclusion mechanism is easy; just
     * need to find the 2 element array.
     */
    @Override
    public Object deserializeTypedArray(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        String typeId = _locateTypeId(jp, ctxt);
        JavaType type = resolveType(typeId);
        JsonDeserializer<Object> deser = ctxt.getDeserializerProvider().findValueDeserializer(ctxt.getConfig(), type, null, null);
        jp.nextToken();
        Object value = deser.deserialize(jp, ctxt);
        // And then need the closing END_ARRAY
        if (jp.nextToken() != JsonToken.END_ARRAY) {
            throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY,
                    "expected closing END_ARRAY after type information and deserialized value");
        }
        return value;
    }    

    @Override
    public Object deserializeTypedObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        String typeId = _locateTypeId(jp, ctxt);
        JavaType type = resolveType(typeId);
        JsonDeserializer<Object> deser = ctxt.getDeserializerProvider().findValueDeserializer(ctxt.getConfig(), type, null, null);
        jp.nextToken();
        Object value = deser.deserialize(jp, ctxt);
        // And then need the closing END_ARRAY
        if (jp.nextToken() != JsonToken.END_ARRAY) {
            throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY,
                    "expected closing END_ARRAY after type information and deserialized value");
        }
        return value;
    }

    @Override
    public Object deserializeTypedScalar(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        String typeId = _locateTypeId(jp, ctxt);
        JavaType type = resolveType(typeId);
        JsonDeserializer<Object> deser = ctxt.getDeserializerProvider().findValueDeserializer(ctxt.getConfig(), type, null, null);
        jp.nextToken();
        Object value = deser.deserialize(jp, ctxt);
        // And then need the closing END_ARRAY
        if (jp.nextToken() != JsonToken.END_ARRAY) {
            throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY,
                    "expected closing END_ARRAY after type information and deserialized value");
        }
        return value;
    }    

    protected final String _locateTypeId(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
            throw ctxt.wrongTokenException(jp, JsonToken.START_ARRAY,
                    "need JSON Array to contain As.WRAPPER_ARRAY type information for class "+baseTypeName());
        }
        // And then type id as a String
        if (jp.nextToken() != JsonToken.VALUE_STRING) {
            throw ctxt.wrongTokenException(jp, JsonToken.VALUE_STRING,
                    "need JSON String that contains type id (for subtype of "+baseTypeName()+")");
        }
        return jp.getText();
    }
}
