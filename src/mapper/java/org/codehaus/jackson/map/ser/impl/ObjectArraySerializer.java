package org.codehaus.jackson.map.ser.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ResolvableSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.annotate.JacksonStdImpl;
import org.codehaus.jackson.map.ser.ArraySerializers;
import org.codehaus.jackson.map.ser.ContainerSerializerBase;
import org.codehaus.jackson.map.type.ArrayType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.schema.SchemaAware;
import org.codehaus.jackson.type.JavaType;

/**
 * Generic serializer for Object arrays (<code>Object[]</code>).
 */
@JacksonStdImpl
public class ObjectArraySerializer
    extends ArraySerializers.AsArraySerializer<Object[]>
    implements ResolvableSerializer
{
    /**
     * Whether we are using static typing (using declared types, ignoring
     * runtime type) or not for elements.
     */
    protected final boolean _staticTyping;

    /**
     * Declared type of element entries
     */
    protected final JavaType _elementType;

    /**
     * Value serializer to use, if it can be statically determined.
     * 
     * @since 1.5
     */
    protected JsonSerializer<Object> _elementSerializer;

    public ObjectArraySerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer vts, BeanProperty property)
    {
        super(Object[].class, vts, property);
        _elementType = elemType;
        _staticTyping = staticTyping;
    }

    @Override
    public ContainerSerializerBase<?> _withValueTypeSerializer(TypeSerializer vts)
    {
        return new ObjectArraySerializer(_elementType, _staticTyping, vts, _property);
    }
    
    @Override
    public void serializeContents(Object[] value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final int len = value.length;
        if (len == 0) {
            return;
        }
        if (_elementSerializer != null) {
            serializeContentsUsing(value, jgen, provider, _elementSerializer);
            return;
        }
        if (_valueTypeSerializer != null) {
            serializeTypedContents(value, jgen, provider);
            return;
        }
        JsonSerializer<Object> prevSerializer = null;
        Class<?> prevClass = null;
        int i = 0;
        for (; i < len; ++i) {
            Object elem = value[i];
            if (elem == null) {
                provider.getNullValueSerializer().serialize(null, jgen, provider);
            } else {
                // Minor optimization to avoid most lookups:
                Class<?> cc = elem.getClass();
                JsonSerializer<Object> currSerializer;
                if (cc == prevClass) {
                    currSerializer = prevSerializer;
                } else {
                    // true -> do cache
                    currSerializer = provider.findValueSerializer(cc, _property);
                    prevSerializer = currSerializer;
                    prevClass = cc;
                }
                try {
                    currSerializer.serialize(elem, jgen, provider);
                } catch (IOException ioe) {
                    throw ioe;
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    /* 05-Mar-2009, tatu: But one nasty edge is when we get
                     *   StackOverflow: usually due to infinite loop. But that gets
                     *   hidden within an InvocationTargetException...
                     */
                    Throwable t = e;
                    while (t instanceof InvocationTargetException && t.getCause() != null) {
                        t = t.getCause();
                    }
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                    throw JsonMappingException.wrapWithPath(t, elem, i);
                }
            }
        }
    }

    public void serializeContentsUsing(Object[] value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser)
        throws IOException, JsonGenerationException
    {
        final int len = value.length;
        final TypeSerializer typeSer = _valueTypeSerializer;
        for (int i = 0; i < len; ++i) {
            Object elem = value[i];
            if (elem == null) {
                provider.getNullValueSerializer().serialize(null, jgen, provider);
                continue;
            }
            try {
                if (typeSer == null) {
                    ser.serialize(elem, jgen, provider);
                } else {
                    ser.serializeWithType(elem, jgen, provider, typeSer);
                }
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception e) {
                Throwable t = e;
                while (t instanceof InvocationTargetException && t.getCause() != null) {
                    t = t.getCause();
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw JsonMappingException.wrapWithPath(t, elem, i);
            }
        }
    }

    public void serializeTypedContents(Object[] value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final int len = value.length;
        final TypeSerializer typeSer = _valueTypeSerializer;
        JsonSerializer<Object> prevSerializer = null;
        Class<?> prevClass = null;
        int i = 0;
        for (; i < len; ++i) {
            Object elem = value[i];
            if (elem == null) {
                provider.getNullValueSerializer().serialize(null, jgen, provider);
                continue;
            }
            // Minor optimization to avoid most lookups:
            Class<?> cc = elem.getClass();
            JsonSerializer<Object> currSerializer;
            if (cc == prevClass) {
                currSerializer = prevSerializer;
            } else {
                currSerializer = provider.findValueSerializer(cc, _property);
                prevSerializer = currSerializer;
                prevClass = cc;
            }
            try {
                currSerializer.serializeWithType(elem, jgen, provider, typeSer);
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception e) {
                Throwable t = e;
                while (t instanceof InvocationTargetException && t.getCause() != null) {
                    t = t.getCause();
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw JsonMappingException.wrapWithPath(t, elem, i);
            }
        }
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        ObjectNode o = createSchemaNode("array", true);
        if (typeHint != null) {
            JavaType javaType = TypeFactory.type(typeHint);
            if (javaType.isArrayType()) {
                Class<?> componentType = ((ArrayType) javaType).getContentType().getRawClass();
                // 15-Oct-2010, tatu: We can't serialize plain Object.class; but what should it produce here? Untyped?
                if (componentType == Object.class) {
                    o.put("items", JsonSchema.getDefaultSchemaNode());
                } else {
                    JsonSerializer<Object> ser = provider.findValueSerializer(componentType, _property);
                    JsonNode schemaNode = (ser instanceof SchemaAware) ?
                            ((SchemaAware) ser).getSchema(provider, null) :
                            JsonSchema.getDefaultSchemaNode();
                    o.put("items", schemaNode);
                }
            }
        }
        return o;
    }

    /**
     * Need to get callback to resolve value serializer, if static typing
     * is used (either being forced, or because value type is final)
     */
    public void resolve(SerializerProvider provider)
        throws JsonMappingException
    {
        if (_staticTyping) {
            _elementSerializer = provider.findValueSerializer(_elementType, _property);
        }
    }        
}
