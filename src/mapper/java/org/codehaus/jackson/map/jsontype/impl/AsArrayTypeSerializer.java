package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;

/**
 * Type serializer that will embed type information in an array,
 * as the first element, and actual value as the second element.
 * 
 * @since 1.5
 * @author tatu
 */
public class AsArrayTypeSerializer
    extends TypeSerializerBase
{
    public AsArrayTypeSerializer(TypeConverter conv)
    {
        super(conv);
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() { return JsonTypeInfo.As.ARRAY; }
    
    @Override
    public void writeTypePrefixForField(Object value, JsonGenerator jgen, String fieldName)
        throws IOException, JsonProcessingException
    {
        jgen.writeArrayFieldStart(fieldName);
        jgen.writeString(_typeConverter.typeAsString(value));
    }

    @Override
    public void writeTypePrefixForValue(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException
    {
        jgen.writeStartArray();
        jgen.writeString(_typeConverter.typeAsString(value));
    }

    @Override
    public void writeTypeSuffixForValue(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException
    {
        jgen.writeEndArray();
    }

    @Override
    public void writeTypeSuffixForField(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException
    {
        jgen.writeEndArray();
    }

}
