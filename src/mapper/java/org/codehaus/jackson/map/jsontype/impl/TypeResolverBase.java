package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.JsonTypeResolver;

public abstract class TypeResolverBase extends JsonTypeResolver
{
    protected final Class<?> _baseType;

    protected final JsonTypeInfo.As _includeAs;

    protected final String _propertyName;
    
    protected TypeResolverBase(Class<?> bt, JsonTypeInfo.As includeAs,
            String propName)
    {
        _baseType = bt;
        if ((includeAs != JsonTypeInfo.As.PROPERTY)
           && (includeAs != JsonTypeInfo.As.WRAPPER)
           && (includeAs != JsonTypeInfo.As.ARRAY)
           ) {
            throw new IllegalArgumentException("Invalid value for includeAs: "+includeAs);
        }
        _includeAs = includeAs;
        // ok to have empty/null property name for some combinations
        _propertyName = propName;
    }
    
    @Override
    public Class<?> getBaseType() {
        return _baseType;
    }

    public final void writeTypePrefix(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        if (_includeAs == JsonTypeInfo.As.ARRAY) {
            jgen.writeStartArray();
            jgen.writeString(typeAsString(value));
        } else { // 
            jgen.writeStartObject();
            if (_includeAs == JsonTypeInfo.As.PROPERTY) {
                jgen.writeStringField(_propertyName, typeAsString(value));
            } else { // As.WRAPPER
                jgen.writeObjectFieldStart(typeAsString(value));
            }
        }
    }

    public final void writeTypeSuffix(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        if (_includeAs == JsonTypeInfo.As.ARRAY) {
            // close wrapping array
            jgen.writeEndArray();
        } else { // 
            // close the actual JSON Object that has data, type
            jgen.writeEndObject();
            // and if using wrapping, wrapper JSON Object as well
            if (_includeAs == JsonTypeInfo.As.WRAPPER) {
                jgen.writeEndObject();
            }
        }        
    }
    
    /*
    ************************************************************
    * Methods for sub-classes to implement
    ************************************************************
     */

    protected abstract String typeAsString(Object value);
}
