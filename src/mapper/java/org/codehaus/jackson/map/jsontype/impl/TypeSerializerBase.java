package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;

public abstract class TypeSerializerBase extends TypeSerializer
{
    protected final JsonTypeInfo.As _includeAs;

    protected final String _propertyName;
    
    protected TypeSerializerBase(JsonTypeInfo.As includeAs, String propName)
    {
        if (includeAs == null) {
            throw new IllegalArgumentException("Invalid value for includeAs: "+includeAs);
        }
        _includeAs = includeAs;
        // ok to have empty/null property name for some combinations
        _propertyName = propName;
    }

    /**
     * Method called to serialize given Object specifically as JSON Object field value.
     */
    public void serializeAsField(Object value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser, String fieldName)
        throws IOException, JsonProcessingException
    {
        switch (_includeAs) {
        case ARRAY:
            jgen.writeArrayFieldStart(fieldName);
            jgen.writeString(typeAsString(value));
            ser.serialize(value, jgen, provider);
            jgen.writeEndArray();
            break;
        case PROPERTY:
            jgen.writeObjectFieldStart(fieldName);
            jgen.writeStringField(_propertyName, typeAsString(value));
            ser.serialize(value, jgen, provider);
            jgen.writeEndObject();
            break;
        case NAME_OF_PROPERTY:
            // Can do this properly, since we are writing JSON Object property
            jgen.writeFieldName(typeAsString(value));
            ser.serialize(value, jgen, provider);
            break;
        case WRAPPER:
            jgen.writeObjectFieldStart(fieldName);
            jgen.writeObjectFieldStart(typeAsString(value));
            jgen.writeStringField(_propertyName, typeAsString(value));
            jgen.writeEndObject();
            jgen.writeEndObject();
            jgen.writeStartObject();
        }
    }

    /**
     * Method called to serialize given Object as JSON Value (but not as a field value)
     */
    public void serializeAsValue(Object value, JsonGenerator jgen, SerializerProvider provider,
        JsonSerializer<Object> ser)
        throws IOException, JsonProcessingException
    {
            switch (_includeAs) {
            case ARRAY:
                jgen.writeStartArray();
                jgen.writeString(typeAsString(value));
                ser.serialize(value, jgen, provider);
                jgen.writeEndArray();
                break;
            case PROPERTY:
                jgen.writeStartObject();
                jgen.writeStringField(_propertyName, typeAsString(value));
                ser.serialize(value, jgen, provider);
                jgen.writeEndObject();
                break;
            case NAME_OF_PROPERTY:
                /* Since we are specifically not writing an object field value,
                 * need to do something else: for now, let's use extra wrappign
                 */
                // fall through
            case WRAPPER:
                jgen.writeStartObject();
                jgen.writeFieldName(typeAsString(value));
                ser.serialize(value, jgen, provider);
                jgen.writeEndObject();
            }
    }
    
    /*
    ************************************************************
    * Methods for sub-classes to implement
    ************************************************************
     */

    protected abstract String typeAsString(Object value);
}
