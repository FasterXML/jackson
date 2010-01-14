package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;

public class AsWrapperTypeSerializer
    extends TypeSerializerBase
{
    public AsWrapperTypeSerializer(TypeConverter conv)
    {
        super(conv);
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() { return JsonTypeInfo.As.PROPERTY; }
    
    @Override
    public void writeTypePrefixForField(Object value, JsonGenerator jgen, String fieldName)
        throws IOException, JsonProcessingException
    {
        jgen.writeObjectFieldStart(fieldName);
        jgen.writeObjectFieldStart(_typeConverter.typeAsString(value));
    }

    @Override
    public void writeTypePrefixForValue(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException
    {
        jgen.writeStartObject();
        jgen.writeObjectFieldStart(_typeConverter.typeAsString(value));
    }

    @Override
    public void writeTypeSuffixForField(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeEndObject();
        jgen.writeEndObject();
    }

    public void writeTypeSuffixForValue(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeEndObject();
        jgen.writeEndObject();
    }
}
