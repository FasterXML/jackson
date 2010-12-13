package org.codehaus.jackson.map.ser.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.ser.SerializerBase;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Intermediate base class for Lists, Collections and Arrays
 * that contain static (non-dynamic) value types.
 * 
 * @since 1.7
 */
public abstract class StaticListSerializerBase<T extends Collection<?>>
    extends SerializerBase<T>
{
    /**
     * Property that contains String List to serialize, if known.
     */
    protected final BeanProperty _property;

    protected StaticListSerializerBase(Class<?> cls, BeanProperty property)
    {
        super(cls, false);
        _property = property;
    }

    @Override
    public final void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeStartArray();
        serializeContents(value, jgen, provider);
        jgen.writeEndArray();
    }
    
    @Override
    public final void serializeWithType(T value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        typeSer.writeTypePrefixForArray(value, jgen);
        serializeContents(value, jgen, provider);
        typeSer.writeTypeSuffixForArray(value, jgen);
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        ObjectNode o = createSchemaNode("array", true);
        o.put("items", contentSchema());
        return o;
    }

    /*
    /**********************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************
     */

    protected abstract void serializeContents(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException;

    protected abstract JsonNode contentSchema();    
}
