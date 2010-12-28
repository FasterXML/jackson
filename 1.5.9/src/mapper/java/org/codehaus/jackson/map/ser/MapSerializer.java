package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.JavaType;

/**
 * Standard serializer implementation for serializing {link java.util.Map} types.
 *<p>
 * Note: about the only configurable setting currently is ability to filter out
 * entries with specified names.
 */
public class MapSerializer
    extends ContainerSerializerBase<Map<?,?>>
    implements ResolvableSerializer
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

    /**
     * Whether static types should be used for serialization of values
     * or not (if not, dynamic runtime type is used)
     */
    protected final boolean _valueTypeIsStatic;

    /**
     * Declared type of contained values
     */
    protected final JavaType _valueType;

    /**
     * Value serializer to use, if it can be statically determined
     * 
     * @since 1.5
     */
    protected JsonSerializer<Object> _valueSerializer;

    /**
     * Type serializer used for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;
    
    protected MapSerializer() {
        this((HashSet<String>)null, null, false, null);
    }
    
    protected MapSerializer(HashSet<String> ignoredEntries, JavaType valueType, boolean valueTypeIsStatic,
            TypeSerializer vts)
    {
        super(Map.class, false);
        _ignoredEntries = ignoredEntries;
        _valueType = valueType;
        _valueTypeIsStatic = valueTypeIsStatic;
        _valueTypeSerializer = vts;
    }
    
    @Override
    public ContainerSerializerBase<?> _withValueTypeSerializer(TypeSerializer vts)
    {
        MapSerializer ms = new MapSerializer(_ignoredEntries, _valueType, _valueTypeIsStatic, vts);
        if (_valueSerializer != null) {
            ms._valueSerializer = _valueSerializer;
        }
        return ms;
    }
    
    /**
     * Factory method used to construct Map serializers.
     * 
     * @param ignoredList Array of entry names that are to be filtered on
     *    serialization; null if none
     * @param mapType Declared type information (needed for static typing)
     * @param staticValueType Whether static typing should be used for the
     *    Map (which includes its contents)
     * @param vts Type serializer to use for map entry values, if any
     */
    public static MapSerializer construct(String[] ignoredList, JavaType mapType,
            boolean staticValueType, TypeSerializer vts)
    {
        HashSet<String> ignoredEntries = toSet(ignoredList);
        JavaType valueType = (mapType == null) ? TypeFactory.type(Object.class) : mapType.getContentType();
        // If value type is final, it's same as forcing static value typing:
        if (!staticValueType) {
            staticValueType = (valueType != null && valueType.isFinal());
        }
        /* for plain vanilla case can return singleton; plain meaning that there are
         * no ignored entries (no view), non-static type (can not statically determine
         * value serializer) and no assigned value type serializer.
         */
        if (!staticValueType && (ignoredEntries == null) && (vts == null)) {
            return instance;
        }
        return new MapSerializer(ignoredEntries, valueType, staticValueType, vts);
    }

    private static HashSet<String> toSet(String[] ignoredEntries) {
        if (ignoredEntries == null || ignoredEntries.length == 0) {
            return null;
        }
        HashSet<String> result = new HashSet<String>(ignoredEntries.length);
        for (String prop : ignoredEntries) {
            result.add(prop);
        }
        return result;
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
            if (_valueSerializer != null) {
                serializeFieldsUsing(value, jgen, provider, _valueSerializer);
            } else {
                serializeFields(value, jgen, provider);
            }
        }        
        jgen.writeEndObject();
    }

    @Override
    public void serializeWithType(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        typeSer.writeTypePrefixForObject(value, jgen);
        if (!value.isEmpty()) {
            if (_valueSerializer != null) {
                serializeFieldsUsing(value, jgen, provider, _valueSerializer);
            } else {
                serializeFields(value, jgen, provider);
            }
        }
        typeSer.writeTypeSuffixForObject(value, jgen);
    }

    /**
     * Method called to serialize fields, when the value type is not statically known.
     */
    protected void serializeFields(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_valueTypeSerializer != null) {
            serializeTypedFields(value, jgen, provider);
            return;
        }
        
        final JsonSerializer<Object> keySerializer = provider.getKeySerializer();
        JsonSerializer<Object> prevValueSerializer = null;
        Class<?> prevValueClass = null;
        final HashSet<String> ignored = _ignoredEntries;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            // First, serialize key
            Object keyElem = entry.getKey();
            if (keyElem == null) {
                provider.getNullKeySerializer().serialize(null, jgen, provider);
            } else {
                // One twist: is entry ignorable? If so, skip
                if (ignored != null && ignored.contains(keyElem)) continue;
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
     * Method called to serialize fields, when the value type is statically known,
     * so that value serializer is passed and does not need to be fetched from
     * provider.
     */
    protected void serializeFieldsUsing(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser)
            throws IOException, JsonGenerationException
    {
        final JsonSerializer<Object> keySerializer = provider.getKeySerializer();
        final HashSet<String> ignored = _ignoredEntries;
        final TypeSerializer typeSer = _valueTypeSerializer;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            Object keyElem = entry.getKey();
            if (keyElem == null) {
                provider.getNullKeySerializer().serialize(null, jgen, provider);
            } else {
                if (ignored != null && ignored.contains(keyElem)) continue;
                keySerializer.serialize(keyElem, jgen, provider);
            }
            Object valueElem = entry.getValue();
            if (valueElem == null) {
                provider.getNullValueSerializer().serialize(null, jgen, provider);
            } else {
                try {
                    if (typeSer == null) {
                        ser.serialize(valueElem, jgen, provider);
                    } else {
                        ser.serializeWithType(valueElem, jgen, provider, typeSer);
                    }
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    String keyDesc = ""+keyElem;
                    wrapAndThrow(e, value, keyDesc);
                }
            }
        }
    }

    protected void serializeTypedFields(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final JsonSerializer<Object> keySerializer = provider.getKeySerializer();
        JsonSerializer<Object> prevValueSerializer = null;
        Class<?> prevValueClass = null;
        final HashSet<String> ignored = _ignoredEntries;
    
        for (Map.Entry<?,?> entry : value.entrySet()) {
            // First, serialize key
            Object keyElem = entry.getKey();
            if (keyElem == null) {
                provider.getNullKeySerializer().serialize(null, jgen, provider);
            } else {
                // One twist: is entry ignorable? If so, skip
                if (ignored != null && ignored.contains(keyElem)) continue;
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
                    currSerializer.serializeWithType(valueElem, jgen, provider, _valueTypeSerializer);
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    String keyDesc = ""+keyElem;
                    wrapAndThrow(e, value, keyDesc);
                }
            }
        }
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        ObjectNode o = createSchemaNode("object", true);
        //(ryan) even though it's possible to statically determine the "value" type of the map,
        // there's no way to statically determine the keys, so the "Entries" can't be determined.
        return o;
    }

    /**
     * Need to get callback to resolve value serializer, if static typing
     * is used (either being forced, or because value type is final)
     */
    public void resolve(SerializerProvider provider)
        throws JsonMappingException
    {
        if (_valueTypeIsStatic) {
            _valueSerializer = provider.findValueSerializer(_valueType);
        }
    }
}

