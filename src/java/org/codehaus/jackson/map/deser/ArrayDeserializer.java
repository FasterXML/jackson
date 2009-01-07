package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.ArrayType;
import org.codehaus.jackson.map.type.JavaType;

/**
 * Basic serializer that can serializer non-primitive arrays.
 */
public class ArrayDeserializer
    extends StdDeserializer<Object>
{
    // // Configuration

    final Class<?> _arrayClass;

    /**
     * Type of contained elements: needed for constructing actual
     * result array
     */
    final Class<?> _elementClass;

    /**
     * Element deserializer
     */
    final JsonDeserializer<Object> _elementDeserializer;

    @SuppressWarnings("unchecked") 
        public ArrayDeserializer(ArrayType arrayType, JsonDeserializer<Object> elemDeser)
    {
        _arrayClass = arrayType.getRawClass();
        _elementClass = arrayType.getComponentType().getRawClass();
        _elementDeserializer = elemDeser;
    }

    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_ARRAY
        if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
            throw ctxt.mappingException(_arrayClass);
        }

        // !!! TODO: optimize, reuse array or ArrayList
        ArrayList<Object> elems = new ArrayList<Object>();
        JsonToken t;
        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            // Note: must handle null explicitly here; value deserializers won't
            Object value = (t == JsonToken.VALUE_NULL) ? null : _elementDeserializer.deserialize(jp, ctxt);
            elems.add(value);
        }

        // Done: must construct the array
        int len = elems.size();
        Object[] result = (Object[]) Array.newInstance(_elementClass, len);

        // !!! TBI
        /*
        if (len > 0) {
            System.arra
        }
        */
        return result;
    }
}
