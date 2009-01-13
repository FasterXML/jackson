package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.map.util.ObjectBuffer;

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
        add(int[].class, new IntDeser());

        add(String[].class, new StringDeser());
    }

    public static HashMap<JavaType,JsonDeserializer<Object>> getAll()
    {
        return instance._allDeserializers;
    }

    @SuppressWarnings("unchecked")
	private void add(Class<?> cls, JsonDeserializer<?> deser)
    {
        _allDeserializers.put(TypeFactory.instance.fromClass(cls),
                              (JsonDeserializer<Object>) deser);
    }

    /*
    /////////////////////////////////////////////////////////////
    // Actual deserializers
    /////////////////////////////////////////////////////////////
    */

    final static class StringDeser
        extends StdDeserializer<String[]>
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

    final static class IntDeser
        extends StdDeserializer<int[]>
    {
        public IntDeser() { super(int[].class); }

        public int[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // Ok: must point to START_ARRAY
            if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                throw ctxt.mappingException(_valueClass);
            }
            return null;
        }
    }
}
