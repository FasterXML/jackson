package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;

/**
 * More specialized type serializer which will try to add type information
 * as modified field name; but failing that will use inline property name
 * (same as what {@link AsPropertyTypeSerializer} does)
 * 
 * @since 1.5
 *
 * @author tatu
 */
public class AsPropertyNameTypeSerializer
    extends TypeSerializerBase
{
    protected final String _propertyName;
        
    public AsPropertyNameTypeSerializer(TypeConverter conv, String propName)
    {
        super(conv);
        _propertyName = propName;
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() { return JsonTypeInfo.As.NAME_OF_PROPERTY; }

    @Override
    public String propertyName() { return _propertyName; }
    
    @Override
    public void writeTypePrefixForField(Object value, JsonGenerator jgen, String fieldName)
        throws IOException, JsonProcessingException
    {
        jgen.writeObjectFieldStart(_typeConverter.typeAsString(value));
    }

    @Override
    public void writeTypePrefixForValue(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        /* For non-field/entry values, we must use some other scheme (no field
         * name to modify). Let's use wrapping, since that is simpler and bit
         * more efficient
         */
        jgen.writeStartObject();
        jgen.writeObjectFieldStart(_typeConverter.typeAsString(value));
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
        // Need "extra" close, since we had to use wrapping
        jgen.writeEndObject();
        jgen.writeEndObject();
    }
}

