package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.ArrayType;
import org.codehaus.jackson.map.util.ObjectBuffer;

/**
 * Basic serializer that can serializer non-primitive arrays.
 */
public class ArrayDeserializer
    extends StdDeserializer<Object>
{
    // // Configuration

    final Class<?> _arrayClass;

    /**
     * Flag that indicates whether the component type is Object or not.
     * Used for minor optimization when constructing result.
     */
    final boolean _untyped;

    /**
     * Type of contained elements: needed for constructing actual
     * result array
     */
    final Class<?> _elementClass;

    /**
     * Element deserializer
     */
    final JsonDeserializer<Object> _elementDeserializer;

    public ArrayDeserializer(ArrayType arrayType, JsonDeserializer<Object> elemDeser)
    {
        super(Object[].class);
        _arrayClass = arrayType.getRawClass();
        _elementClass = arrayType.getComponentType().getRawClass();
        _untyped = (_elementClass == Object.class);
        _elementDeserializer = elemDeser;
    }

    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_ARRAY
        if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
            throw ctxt.mappingException(_arrayClass);
        }

        final ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] chunk = buffer.resetAndStart();
        int ix = 0;
        JsonToken t;

        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            // Note: must handle null explicitly here; value deserializers won't
            Object value = (t == JsonToken.VALUE_NULL) ? null : _elementDeserializer.deserialize(jp, ctxt);
            if (ix >= chunk.length) {
                chunk = buffer.appendCompletedChunk(chunk);
                ix = 0;
            }
            chunk[ix++] = value;
        }

        Object[] result;

        if (_untyped) {
            result = buffer.completeAndClearBuffer(chunk, ix);
        } else {
            result = buffer.completeAndClearBuffer(chunk, ix, _elementClass);
        }
        ctxt.returnObjectBuffer(buffer);
        return result;
    }
}
