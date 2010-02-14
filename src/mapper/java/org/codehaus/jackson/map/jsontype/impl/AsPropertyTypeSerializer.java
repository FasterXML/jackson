package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;

/**
 * Type serializer that preferably embeds type information as an additional
 * JSON Object property, if possible (when resulting serialization would
 * use JSON Object). If this is not possible (for JSON Arrays, scalars),
 * uses a JSON Array wrapper (similar to how
 * {@link JsonTypeInfo.As#WRAPPER_ARRAY} always works) as a fallback.
 * 
 * @since 1.5
 * @author tatus
 */
public class AsPropertyTypeSerializer
    extends AsArrayTypeSerializer
{
    protected final String _propertyName;

    public AsPropertyTypeSerializer(TypeIdResolver idRes, String propName)
    {
        super(idRes);
        _propertyName = propName;
    }

    @Override
    public String getPropertyName() { return _propertyName; }

    @Override
    public JsonTypeInfo.As getTypeInclusion() { return JsonTypeInfo.As.PROPERTY; }
    
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeStartObject();
        jgen.writeStringField(_propertyName, _idResolver.idFromValue(value));
    }

    //public void writeTypePrefixForArray(Object value, JsonGenerator jgen)
    //public void writeTypePrefixForScalar(Object value, JsonGenerator jgen)

    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeEndObject();
    }

    //public void writeTypeSuffixForArray(Object value, JsonGenerator jgen)
    //public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen)
}
