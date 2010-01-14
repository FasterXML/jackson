package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;

public class AsPropertyTypeSerializer
    extends TypeSerializerBase
{
    protected final String _propertyName;

    public AsPropertyTypeSerializer(TypeConverter conv, String propName)
    {
        super(conv);
        _propertyName = propName;
    }

    @Override
    public String propertyName() { return _propertyName; }

    @Override
    public JsonTypeInfo.As getTypeInclusion() { return JsonTypeInfo.As.PROPERTY; }
    
    @Override
    public void writeTypePrefixForField(Object value, JsonGenerator jgen, String fieldName)
        throws IOException, JsonProcessingException
    {
        jgen.writeObjectFieldStart(fieldName);
        jgen.writeStringField(_propertyName, _typeConverter.typeAsString(value));
    }

    @Override
    public void writeTypePrefixForValue(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException
    {
        jgen.writeStartObject();
        jgen.writeStringField(_propertyName, _typeConverter.typeAsString(value));
    }

    @Override
    public void writeTypeSuffixForField(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeEndObject();
    }

    @Override
    public void writeTypeSuffixForValue(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeEndObject();
    }
}
