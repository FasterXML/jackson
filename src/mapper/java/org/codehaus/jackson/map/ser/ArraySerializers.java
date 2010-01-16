package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import org.codehaus.jackson.*;
import org.codehaus.jackson.schema.SchemaAware;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.type.ArrayType;

/**
 * Dummy container class to group standard array serializer implementations.
 *<p>
 * TODO: as per [JACKSON-55], should try to add path info for all serializers;
 * is still missing those for some container types.
 */
public final class ArraySerializers
{
    private ArraySerializers() { }

    /*
     ****************************************************************
     * Factory methods
     ****************************************************************
     */

    public static JsonSerializer<?> objectArraySerializer(JavaType elementType, boolean staticTyping,
            TypeSerializer ets)
    {
        return new ObjectArraySerializer(elementType, staticTyping, ets);
    }

    /*
     ****************************************************************
     * Base classes
     ****************************************************************
     */
    
    /**
     * Base class for serializers that will output contents as JSON
     * arrays.
     */
     private abstract static class AsArraySerializer<T>
        extends SerializerBase<T>
    {
        protected AsArraySerializer(Class<T> cls) {
            super(cls);
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

        protected abstract void serializeContents(T value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException;
    }
    
    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, arrays
    ////////////////////////////////////////////////////////////
     */

    /**
     * Generic serializer for Object arrays (<code>Object[]</code>).
     */
    public final static class ObjectArraySerializer
        extends AsArraySerializer<Object[]>
        implements ResolvableSerializer
    {
        public final static ObjectArraySerializer instance = new ObjectArraySerializer();

        protected final boolean _staticTyping;

        protected final JavaType _elementType;
        
        /**
         * Type serializer used for values, if any.
         */
        protected final TypeSerializer _elementTypeSerializer;

        /**
         * Value serializer to use, if it can be statically determined
         * 
         * @since 1.5
         */
        protected JsonSerializer<Object> _elementSerializer;
        
        public ObjectArraySerializer() {
            this(null, false, null);
        }

        public ObjectArraySerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer ets)
        {
            super(Object[].class);
            _elementType = elemType;
            _staticTyping = staticTyping;
            _elementTypeSerializer = ets;
        }
        
        @Override
        public void serializeContents(Object[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            if (_elementSerializer != null) {
                serializeContentsUsing(value, jgen, provider, _elementSerializer);
                return;
            }
            if (_elementTypeSerializer != null) {
                serializeTypedContents(value, jgen, provider);
                return;
            }
            final int len = value.length;
            if (len == 0) {
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
                        currSerializer = provider.findValueSerializer(cc);
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
            final TypeSerializer typeSer = _elementTypeSerializer;
            for (int i = 0; i < len; ++i) {
                Object elem = value[i];
                if (elem == null) {
                    provider.getNullValueSerializer().serialize(null, jgen, provider);
                } else {
                    try {
                        if (typeSer == null) {
                            ser.serialize(elem, jgen, provider);
                        } else {
                            ser.serializeWithType(elem, jgen, provider, typeSer);
                        }
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

        public void serializeTypedContents(Object[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            final int len = value.length;
            if (len == 0) {
                return;
            }
            final TypeSerializer typeSer = _elementTypeSerializer;
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
                    currSerializer = provider.findValueSerializer(cc);
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
        
        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            ObjectNode o = createSchemaNode("array", true);
            if (typeHint != null) {
                JavaType javaType = TypeFactory.type(typeHint);
                if (javaType.isArrayType()) {
                    Class<?> componentType = ((ArrayType) javaType).getContentType().getRawClass();
                    JsonSerializer<Object> ser = provider.findValueSerializer(componentType);
                    JsonNode schemaNode = (ser instanceof SchemaAware) ?
                            ((SchemaAware) ser).getSchema(provider, null) :
                            JsonSchema.getDefaultSchemaNode();
                    o.put("items", schemaNode);
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
                _elementSerializer = provider.findValueSerializer(_elementType.getRawClass());
            }
        }        
    }

    public final static class StringArraySerializer
        extends AsArraySerializer<String[]>
    {
        public StringArraySerializer() { super(String[].class); }

        @Override
        public void serializeContents(String[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            final int len = value.length;
            if (len > 0) {
                /* 08-Dec-2008, tatus: If we want this to be fully overridable
                 *  (for example, to support String cleanup during writing
                 *  or something), we should find serializer  by provider.
                 *  But for now, that seems like an overkill: and caller can
                 *  add custom serializer if that is needed as well.
                 * (ditto for null values)
                 */
                //JsonSerializer<String> ser = (JsonSerializer<String>)provider.findValueSerializer(String.class);
                for (int i = 0; i < len; ++i) {
                    String str = value[i];
                    if (str == null) {
                        jgen.writeNull();
                    } else {
                        //ser.serialize(value[i], jgen, provider);
                        jgen.writeString(value[i]);
                    }
                }
            }
        }

        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("string"));
            return o;
        }
    }

    public final static class BooleanArraySerializer
        extends AsArraySerializer<boolean[]>
    {
        public BooleanArraySerializer() { super(boolean[].class); }
        
        @Override
        public void serializeContents(boolean[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeBoolean(value[i]);
            }
        }

        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("boolean"));
            return o;
        }
    }

    /**
     * Unlike other integral number array serializers, we do not just print out byte values
     * as numbers. Instead, we assume that it would make more sense to output content
     * as base64 encoded bytes (using default base64 encoding).
     *<p>
     * NOTE: since it is NOT serialized as an array, can not use AsArraySerializer as base
     */
    public final static class ByteArraySerializer
        extends SerializerBase<byte[]>
    {
        public ByteArraySerializer() { super(byte[].class); }

        @Override
        public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBinary(value);
        }

        public void serializeWithType(byte[] value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            typeSer.writeTypePrefixForScalar(value, jgen);
            jgen.writeBinary(value);
            typeSer.writeTypeSuffixForScalar(value, jgen);
        }
        
        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            ObjectNode itemSchema = createSchemaNode("string"); //binary values written as strings?
            o.put("items", itemSchema);
            return o;
        }
    }

    public final static class ShortArraySerializer
        extends AsArraySerializer<short[]>
    {
        public ShortArraySerializer() { super(short[].class); }

        @SuppressWarnings("cast")
        @Override
        public void serializeContents(short[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber((int)value[i]);
            }
        }

        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            //no "short" type defined by json
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("integer"));
            return o;
        }
    }

    /**
     * Character arrays are different from other integral number arrays in that
     * they are most likely to be textual data, and should be written as
     * Strings, not arrays of entries.
     *<p>
     * NOTE: since it is NOT serialized as an array, can not use AsArraySerializer as base
     */
    public final static class CharArraySerializer
        extends SerializerBase<char[]>
    {
        public CharArraySerializer() { super(char[].class); }
        
        @Override
        public void serialize(char[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value, 0, value.length);
        }

        @Override
        public void serializeWithType(char[] value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            typeSer.writeTypePrefixForScalar(value, jgen);
            jgen.writeString(value, 0, value.length);
            typeSer.writeTypeSuffixForScalar(value, jgen);
        }
        
        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            ObjectNode itemSchema = createSchemaNode("string");
            itemSchema.put("type", "string");
            o.put("items", itemSchema);
            return o;
        }
    }


    public final static class IntArraySerializer
        extends AsArraySerializer<int[]>
    {
        public IntArraySerializer() { super(int[].class); }

        @Override
        public void serializeContents(int[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("integer"));
            return o;
        }
    }

    public final static class LongArraySerializer
        extends AsArraySerializer<long[]>
    {
        public LongArraySerializer() { super(long[].class); }

        @Override
        public void serializeContents(long[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("number", true));
            return o;
        }
    }

    public final static class FloatArraySerializer
        extends AsArraySerializer<float[]>
    {
        public FloatArraySerializer() { super(float[].class); }

        @Override
        public void serializeContents(float[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("number"));
            return o;
        }
    }

    public final static class DoubleArraySerializer
        extends AsArraySerializer<double[]>
    {
        public DoubleArraySerializer() { super(double[].class); }
        
        @Override
        public void serializeContents(double[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
        }

        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            ObjectNode o = createSchemaNode("array", true);
            o.put("items", createSchemaNode("number"));
            return o;
        }
    }
}
