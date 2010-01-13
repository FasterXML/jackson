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
    ////////////////////////////////////////////////////////////
    // Concrete serializers, arrays
    ////////////////////////////////////////////////////////////
     */

    /**
     * Generic serializer for Object arrays (<code>Object[]</code>).
     */
    public final static class ObjectArraySerializer
        extends SerializerBase<Object[]>
    {
        public final static ObjectArraySerializer instance = new ObjectArraySerializer();

        public ObjectArraySerializer() { super(Object[].class); }

        @Override
        public void serialize(Object[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            final int len = value.length;
            if (len > 0) {
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
                            currSerializer = provider.findTypedValueSerializer(cc, cc, true);
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
            jgen.writeEndArray();
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
                    JsonSerializer<Object> ser = provider.findNonTypedValueSerializer(componentType);
                    JsonNode schemaNode = (ser instanceof SchemaAware) ?
                            ((SchemaAware) ser).getSchema(provider, null) :
                            JsonSchema.getDefaultSchemaNode();
                    o.put("items", schemaNode);
                }
            }
            return o;
        }
    }

    public final static class StringArraySerializer
        extends SerializerBase<String[]>
    {
        public StringArraySerializer() { super(String[].class); }

        @Override
        public void serialize(String[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
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
            jgen.writeEndArray();
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
        extends SerializerBase<boolean[]>
    {
        public BooleanArraySerializer() { super(boolean[].class); }
        
        @Override
	public void serialize(boolean[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeBoolean(value[i]);
            }
            jgen.writeEndArray();
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
        extends SerializerBase<short[]>
    {
        public ShortArraySerializer() { super(short[].class); }

        @SuppressWarnings("cast")
        @Override
        public void serialize(short[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber((int)value[i]);
            }
            jgen.writeEndArray();
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
        extends SerializerBase<int[]>
    {
        public IntArraySerializer() { super(int[].class); }

        @Override
        public void serialize(int[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
            jgen.writeEndArray();
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
        extends SerializerBase<long[]>
    {
        public LongArraySerializer() { super(long[].class); }

        @Override
        public void serialize(long[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
            jgen.writeEndArray();
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
        extends SerializerBase<float[]>
    {
        public FloatArraySerializer() { super(float[].class); }

        @Override
        public void serialize(float[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
            jgen.writeEndArray();
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
        extends SerializerBase<double[]>
    {
        public DoubleArraySerializer() { super(double[].class); }
        
        @Override
        public void serialize(double[] value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (int i = 0, len = value.length; i < len; ++i) {
                jgen.writeNumber(value[i]);
            }
            jgen.writeEndArray();
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
