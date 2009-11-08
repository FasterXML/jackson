package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Standard serializer implementation for serializing {link java.util.Map} types.
 *<p>
 * Note: about the only configurable setting currently is ability to filter out
 * entries with specified names.
 */
public class MapSerializer
    extends SerializerBase<Map<?,?>>
{
    /**
     * Default instance that can be used for Map types that have
     * no specific custom annotations.
     */
    public final static MapSerializer instance = new MapSerializer();

    /**
     * Set of entries to omit during serialization, if any
     */
    protected final HashSet<String> _ignoredEntries;
    
    protected MapSerializer() {
        this(null);
    }
    
    protected MapSerializer(String[] ignoredEntries)
    {
        if (ignoredEntries == null || ignoredEntries.length == 0) {
            _ignoredEntries = null;
        } else {
            _ignoredEntries = new HashSet<String>(ignoredEntries.length);
            for (String prop : ignoredEntries) {
                _ignoredEntries.add(prop);
            }
        }
    }

    /**
     * @param ignoredEntries Array of entry names that are to be filtered on
     *    serialization; null if none
     */
    public static MapSerializer construct(String[] ignoredEntries)
    {
        if (ignoredEntries == null || ignoredEntries.length == 0) {
            return instance;
        }
        return new MapSerializer(ignoredEntries);
    }
    
    /*
     ***********************************************************************
     * JsonSerializer implementation
     ***********************************************************************
     */

    @Override
    public void serialize(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeStartObject();
        if (!value.isEmpty()) {
            if (_ignoredEntries == null) {
                serializeEntries(value, jgen, provider);
            } else {
                serializeSomeEntries(value, jgen, provider, _ignoredEntries);
            }
        }        
        jgen.writeEndObject();
    }

    //@Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        ObjectNode o = createSchemaNode("object", true);
        //(ryan) even though it's possible to statically determine the "value" type of the map,
        // there's no way to statically determine the keys, so the "Entries" can't be determined.
        return o;
    }
    
    /*
     ***********************************************************************
     * Internal helper methods
     ***********************************************************************
     */
    
    /**
     * Helper method that will write all entries of given non-empty Map
     */
    protected final void serializeEntries(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final JsonSerializer<Object> keySerializer = provider.getKeySerializer();
        JsonSerializer<Object> prevValueSerializer = null;
        Class<?> prevValueClass = null;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            // First, serialize key
            Object keyElem = entry.getKey();
            if (keyElem == null) {
                provider.getNullKeySerializer().serialize(null, jgen, provider);
            } else {
                keySerializer.serialize(keyElem, jgen, provider);
            }

            // And then value
            Object valueElem = entry.getValue();
            if (valueElem == null) {
                provider.getNullValueSerializer().serialize(null, jgen, provider);
            } else {
                Class<?> cc = valueElem.getClass();
                JsonSerializer<Object> currSerializer;
                if (cc == prevValueClass) {
                    currSerializer = prevValueSerializer;
                } else {
                    currSerializer = provider.findValueSerializer(cc);
                    prevValueSerializer = currSerializer;
                    prevValueClass = cc;
                }
                try {
                    currSerializer.serialize(valueElem, jgen, provider);
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    String keyDesc = ""+keyElem;
                    wrapAndThrow(e, value, keyDesc);
                }
            }
        }
    }

    /**
     * Helper method that will write all entries of the given non-empty map, except
     * for specified set of ignorable entries, filtered based on entry key.
     */
    protected final void serializeSomeEntries(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider,
            HashSet<String> ignored)
        throws IOException, JsonGenerationException
    {
        final JsonSerializer<Object> keySerializer = provider.getKeySerializer();
        JsonSerializer<Object> prevValueSerializer = null;
        Class<?> prevValueClass = null;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            // First, serialize key
            Object keyElem = entry.getKey();
            if (keyElem == null) {
                provider.getNullKeySerializer().serialize(null, jgen, provider);
            } else {
                // One twist: is it ignorable? If so, skip
                if (ignored.contains(keyElem)) continue;
                keySerializer.serialize(keyElem, jgen, provider);
            }

            // And then value
            Object valueElem = entry.getValue();
            if (valueElem == null) {
                provider.getNullValueSerializer().serialize(null, jgen, provider);
            } else {
                Class<?> cc = valueElem.getClass();
                JsonSerializer<Object> currSerializer;
                if (cc == prevValueClass) {
                    currSerializer = prevValueSerializer;
                } else {
                    currSerializer = provider.findValueSerializer(cc);
                    prevValueSerializer = currSerializer;
                    prevValueClass = cc;
                }
                try {
                    currSerializer.serialize(valueElem, jgen, provider);
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    String keyDesc = ""+keyElem;
                    wrapAndThrow(e, value, keyDesc);
                }
            }
        }
    }
}
