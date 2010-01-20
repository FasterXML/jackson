package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;

/**
 * Type serializer that preferably embeds type information as an additional
 * JSON Object property, if possible (when resulting serialization would
 * use JSON Object); and if not, uses an array wrapper (similar to how
 * {@link JsonTypeInfo.As.WRAPPER_ARRAY} always works) as a fallback.
 * 
 * @since 1.5
 * @author tatus
 */
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
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeStartObject();
        jgen.writeStringField(_propertyName, _typeConverter.typeAsString(value));
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeStartArray();
        jgen.writeString(_typeConverter.typeAsString(value));
        jgen.writeStartArray();
    }
    
    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException
    {
        jgen.writeStartArray();
        jgen.writeString(_typeConverter.typeAsString(value));
    }

    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeEndObject();
    }

    @Override
    public void writeTypeSuffixForArray(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // first wrapping array
        jgen.writeEndArray();
        // then array caller wants
        jgen.writeEndArray();
    }
    
    @Override
    public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // just have the wrapper array
        jgen.writeEndArray();
    }
}
