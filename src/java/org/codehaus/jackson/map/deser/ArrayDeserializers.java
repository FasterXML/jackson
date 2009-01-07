package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.*;

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
        extends JsonDeserializer<String[]>
    {
        public String[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // !!! TBI
            return null;
        }
    }

    final static class IntDeser
        extends JsonDeserializer<int[]>
    {
        public int[] deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // !!! TBI
            return null;
        }
    }
}
