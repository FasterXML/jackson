package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.map.util.ArrayBuilders;
import org.codehaus.jackson.map.util.ObjectBuffer;
import org.codehaus.jackson.type.JavaType;

/**
 * Container for deserializers used for instantiating "primitive arrays",
 * arrays that contain non-object java primitive types.
 */
public class ArrayDeserializers
{
    HashMap<JavaType,JsonDeserializer<Object>> _allDeserializers;

    final static ArrayDeserializers instance = new ArrayDeserializers();

    private ArrayDeserializers()
    {
        _allDeserializers = new HashMap<JavaType,JsonDeserializer<Object>>();
        // note: we'll use component type as key, not array type
        add(boolean.class, new BooleanDeser());

        /* ByteDeser is bit special, as it has 2 separate modes of operation;
         * one for String input (-> base64 input), the other for
         * numeric input
         */
        add(byte.class, new ByteDeser());
        add(short.class, new ShortDeser());
        add(int.class, new IntDeser());
        add(long.class, new LongDeser());

        add(float.class, new FloatDeser());
        add(double.class, new DoubleDeser());

        add(String.class, new StringDeser());
        /* also: char[] is most likely only used with Strings; doesn't
         * seem to make sense to transfer as numbers
         */
        add(char.class, new CharDeser());
    }

    public static HashMap<JavaType,JsonDeserializer<Object>> getAll()
    {
        return instance._allDeserializers;
    }

    @SuppressWarnings("unchecked")
	private void add(Class<?> cls, JsonDeserializer<?> deser)
    {
        _allDeserializers.put(TypeFactory.type(cls),
                              (JsonDeserializer<Object>) deser);
    }

    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        /* Should there be separate handling for base64 stuff?
         * for now this should be enough:
         */
        return typeDeserializer.deserializeTypedFromArray(jp, ctxt);
    }

    /*
    /********************************************************
    /* Intermediate base class
    /********************************************************
     */
    
    static abstract class ArrayDeser<T>
        extends StdDeserializer<T>
    {
        protected ArrayDeser(Class<T> cls) {
            super(cls);
        }

        @Override
        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException
        {
            return typeDeserializer.deserializeTypedFromArray(jp, ctxt);
        }
    }
    
    /*
    /////////////////////////////////////////////////////////////
    // Actual deserializers: efficient String[], char[] deserializers
    /////////////////////////////////////////////////////////////
    */

    final static class StringDeser
        extends ArrayDeser<String[]>
    {
        public StringDeser() { super(String[].class); }

        public String[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // Ok: must point to START_ARRAY
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw ctxt.mappingException(_valueClass);
            }
            final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
            Object[] chunk = buffer.resetAndStart();
            int ix = 0;
            JsonToken t;
            
            while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
                // Ok: no need to convert Strings, but must recognize nulls
                String value = (t == JsonToken.VALUE_NULL) ? null : jp.getText();
                if (ix >= chunk.length) {
                    chunk = buffer.appendCompletedChunk(chunk);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
            String[] result = buffer.completeAndClearBuffer(chunk, ix, String.class);
            ctxt.returnObjectBuffer(buffer);
            return result;
        }
    }

    final static class CharDeser
        extends ArrayDeser<char[]>
    {
        public CharDeser() { super(char[].class); }

        public char[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            /* Won't take arrays, must get a String (could also
             * convert other tokens to Strings... but let's not bother
             * yet, doesn't seem to make sense)
             */
            if (jp.getCurrentToken() != JsonToken.VALUE_STRING) {
                throw ctxt.mappingException(_valueClass);
            }
            // note: can NOT return shared internal buffer, must copy:
            char[] buffer = jp.getTextCharacters();
            int offset = jp.getTextOffset();
            int len = jp.getTextLength();

            char[] result = new char[len];
            System.arraycopy(buffer, offset, result, 0, len);
            return result;
        }
    }

    /*
    /////////////////////////////////////////////////////////////
    // Actual deserializers: primivate array desers
    /////////////////////////////////////////////////////////////
    */

    final static class BooleanDeser
        extends ArrayDeser<boolean[]>
    {
        public BooleanDeser() { super(boolean[].class); }

        public boolean[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw ctxt.mappingException(_valueClass);
            }
            ArrayBuilders.BooleanBuilder builder = ctxt.getArrayBuilders().getBooleanBuilder();
            boolean[] chunk = builder.resetAndStart();
            int ix = 0;

            while (jp.nextToken() != JsonToken.END_ARRAY) {
                // whether we should allow truncating conversions?
                boolean value = _parseBoolean(jp, ctxt);
                if (ix >= chunk.length) {
                    chunk = builder.appendCompletedChunk(chunk, ix);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }
    }

    /**
     * When dealing with byte arrays we have one more alternative (compared
     * to int/long/shorts): base64 encoded data.
     */
    final static class ByteDeser
        extends ArrayDeser<byte[]>
    {
        public ByteDeser() { super(byte[].class); }

        public byte[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();

            // Most likely case: base64 encoded String?
            if (t == JsonToken.VALUE_STRING) {
                return jp.getBinaryValue(ctxt.getBase64Variant());
            }
            // 31-Dec-2009, tatu: Also may be hidden as embedded Object
            if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object ob = jp.getEmbeddedObject();
                if (ob == null) return null;
                if (ob instanceof byte[]) {
                    return (byte[]) ob;
                }
            }            
            if (t != JsonToken.START_ARRAY) {
                throw ctxt.mappingException(_valueClass);
            }
            ArrayBuilders.ByteBuilder builder = ctxt.getArrayBuilders().getByteBuilder();
            byte[] chunk = builder.resetAndStart();
            int ix = 0;

            while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
                // whether we should allow truncating conversions?
                byte value;
                if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
                    // should we catch overflow exceptions?
                    value = jp.getByteValue();
                } else {
                    // [JACKSON-79]: should probably accept nulls as 'false'
                    if (t != JsonToken.VALUE_NULL) {
                        throw ctxt.mappingException(_valueClass.getComponentType());
                    }
                    value = (byte) 0;
                }
                if (ix >= chunk.length) {
                    chunk = builder.appendCompletedChunk(chunk, ix);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }
    }

    final static class ShortDeser
        extends ArrayDeser<short[]>
    {
        public ShortDeser() { super(short[].class); }

        public short[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw ctxt.mappingException(_valueClass);
            }
            ArrayBuilders.ShortBuilder builder = ctxt.getArrayBuilders().getShortBuilder();
            short[] chunk = builder.resetAndStart();
            int ix = 0;

            while (jp.nextToken() != JsonToken.END_ARRAY) {
                short value = _parseShort(jp, ctxt);
                if (ix >= chunk.length) {
                    chunk = builder.appendCompletedChunk(chunk, ix);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }
    }

    final static class IntDeser
        extends ArrayDeser<int[]>
    {
        public IntDeser() { super(int[].class); }

        public int[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw ctxt.mappingException(_valueClass);
            }
            ArrayBuilders.IntBuilder builder = ctxt.getArrayBuilders().getIntBuilder();
            int[] chunk = builder.resetAndStart();
            int ix = 0;

            while (jp.nextToken() != JsonToken.END_ARRAY) {
                // whether we should allow truncating conversions?
                int value = _parseInt(jp, ctxt);
                if (ix >= chunk.length) {
                    chunk = builder.appendCompletedChunk(chunk, ix);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }
    }

    final static class LongDeser
        extends ArrayDeser<long[]>
    {
        public LongDeser() { super(long[].class); }

        public long[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw ctxt.mappingException(_valueClass);
            }
            ArrayBuilders.LongBuilder builder = ctxt.getArrayBuilders().getLongBuilder();
            long[] chunk = builder.resetAndStart();
            int ix = 0;

            while (jp.nextToken() != JsonToken.END_ARRAY) {
                long value = _parseLong(jp, ctxt);
                if (ix >= chunk.length) {
                    chunk = builder.appendCompletedChunk(chunk, ix);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }
    }

    final static class FloatDeser
        extends ArrayDeser<float[]>
    {
        public FloatDeser() { super(float[].class); }

        public float[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw ctxt.mappingException(_valueClass);
            }
            ArrayBuilders.FloatBuilder builder = ctxt.getArrayBuilders().getFloatBuilder();
            float[] chunk = builder.resetAndStart();
            int ix = 0;

            while (jp.nextToken() != JsonToken.END_ARRAY) {
                // whether we should allow truncating conversions?
                float value = _parseFloat(jp, ctxt);
                if (ix >= chunk.length) {
                    chunk = builder.appendCompletedChunk(chunk, ix);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }
    }

    final static class DoubleDeser
        extends ArrayDeser<double[]>
    {
        public DoubleDeser() { super(double[].class); }

        public double[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw ctxt.mappingException(_valueClass);
            }
            ArrayBuilders.DoubleBuilder builder = ctxt.getArrayBuilders().getDoubleBuilder();
            double[] chunk = builder.resetAndStart();
            int ix = 0;

            while (jp.nextToken() != JsonToken.END_ARRAY) {
                double value = _parseDouble(jp, ctxt);
                if (ix >= chunk.length) {
                    chunk = builder.appendCompletedChunk(chunk, ix);
                    ix = 0;
                }
                chunk[ix++] = value;
            }
            return builder.completeAndClearBuffer(chunk, ix);
        }
    }
}
