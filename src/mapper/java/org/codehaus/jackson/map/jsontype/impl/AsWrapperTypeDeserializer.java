package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.type.JavaType;

public class AsWrapperTypeDeserializer extends TypeDeserializerBase
{
    public AsWrapperTypeDeserializer(Class<?> bt, TypeConverter c) {
        super(bt, c);
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() {
        return JsonTypeInfo.As.WRAPPER_OBJECT;
    }

    /**
     * Deserializing type id enclosed using WRAPPER_OBJECT style is straightforward
     */
    @Override
    public Object deserializeTypedObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // first, sanity checks
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw ctxt.wrongTokenException(jp, JsonToken.START_OBJECT,
                    "need JSON Object to contain As.WRAPPER_OBJECT type information for class "+baseTypeName());
        }
        // should always get field name, but just in case...
        if (jp.nextToken() != JsonToken.FIELD_NAME) {
            throw ctxt.wrongTokenException(jp, JsonToken.FIELD_NAME,
                    "need JSON String that contains type id (for subtype of "+baseTypeName()+")");
        }
        JavaType type = resolveType(jp.getText());
        JsonDeserializer<Object> deser = ctxt.getDeserializerProvider().findValueDeserializer(ctxt.getConfig(), type, null, null);
        jp.nextToken();
        Object value = deser.deserialize(jp, ctxt);
        // And then need the closing END_OBJECT
        if (jp.nextToken() != JsonToken.END_OBJECT) {
            throw ctxt.wrongTokenException(jp, JsonToken.END_OBJECT,
                    "expected closing END_OBJECT after type information and deserialized value");
        }
        return value;
    }    

    @Override
    public Object deserializeTypedArray(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return deserializeTypedObject(jp, ctxt);
    }

    @Override
    public Object deserializeTypedScalar(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return deserializeTypedObject(jp, ctxt);
    }
}
