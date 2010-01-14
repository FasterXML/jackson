package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.CollectionType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.EnumValues;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.schema.SchemaAware;
import org.codehaus.jackson.type.JavaType;

/**
 * Dummy container class to group standard container serializers: serializers
 * that can serialize things like {@link java.util.List}s,
 * {@link java.util.Map}s and such.
 *<p>
 * TODO: as per [JACKSON-55], should try to add path info for all serializers;
 * is still missing those for some container types.
 */
public final class ContainerSerializers
{
    private ContainerSerializers() { }

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, Lists/collections
    ////////////////////////////////////////////////////////////
     */

    /**
     * This is an optimizied serializer for Lists that can be efficiently
     * traversed by index (as opposed to others, such as {@link LinkedList}
     * that can not}.
     */
    public final static class IndexedListSerializer
        extends SerializerBase<List<?>>
    {
        public final static IndexedListSerializer instance = new IndexedListSerializer();

        public IndexedListSerializer() { super(List.class, false); }
        
        @Override
        public void serialize(List<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();

            final int len = value.size();

            if (len > 0) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                int i = 0;

                try {
                    for (; i < len; ++i) {
                        Object elem = value.get(i);
                        if (elem == null) {
                            provider.getNullValueSerializer().serialize(null, jgen, provider);
                        } else {
                            // Minor optimization to avoid most lookups:
                            Class<?> cc = elem.getClass();
                            JsonSerializer<Object> currSerializer;
                            if (cc == prevClass) {
                                currSerializer = prevSerializer;
                            } else {
                                currSerializer = provider.findTypedValueSerializer(cc, cc, true);
                                prevSerializer = currSerializer;
                                prevClass = cc;
                            }
                            currSerializer.serialize(elem, jgen, provider);
                        }
                    }
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    wrapAndThrow(e, value, i);
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
                if (javaType instanceof CollectionType) {
                    Class<?> componentType = ((CollectionType) javaType).getContentType().getRawClass();
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

    /**
     * Fallback serializer for cases where Collection is not known to be
     * of type for which more specializer serializer exists (such as
     * index-accessible List).
     * If so, we will just construct an {@link java.util.Iterator}
     * to iterate over elements.
     */
    public final static class CollectionSerializer
        extends SerializerBase<Collection<?>>
    {
        public final static CollectionSerializer instance = new CollectionSerializer();

        public CollectionSerializer() { super(Collection.class, false); }
        
        @Override
        public void serialize(Collection<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();

            Iterator<?> it = value.iterator();
            if (it.hasNext()) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;

                int i = 0;

                try {
                    do {
                        Object elem = it.next();
                        if (elem == null) {
                            provider.getNullValueSerializer().serialize(null, jgen, provider);
                        } else {
                            // Minor optimization to avoid most lookups:
                            Class<?> cc = elem.getClass();
                            JsonSerializer<Object> currSerializer;
                            if (cc == prevClass) {
                                currSerializer = prevSerializer;
                            } else {
                                currSerializer = provider.findTypedValueSerializer(cc, cc, true);
                                prevSerializer = currSerializer;
                                prevClass = cc;
                            }
                            currSerializer.serialize(elem, jgen, provider);
                        }
                        ++i;
                    } while (it.hasNext());
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    wrapAndThrow(e, value, i);
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
                if (javaType instanceof CollectionType) {
                    Class<?> componentType = ((CollectionType) javaType).getContentType().getRawClass();
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

    public final static class IteratorSerializer
        extends SerializerBase<Iterator<?>>
    {
        public final static IteratorSerializer instance = new IteratorSerializer();

        public IteratorSerializer() { super(Iterator.class, false); }
        
        @Override
        public void serialize(Iterator<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            if (value.hasNext()) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                do {
                    Object elem = value.next();
                    if (elem == null) {
                        provider.getNullValueSerializer().serialize(null, jgen, provider);
                    } else {
                        // Minor optimization to avoid most lookups:
                        Class<?> cc = elem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevClass) {
                            currSerializer = prevSerializer;
                        } else {
                            currSerializer = provider.findTypedValueSerializer(cc, cc, true);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                } while (value.hasNext());
            }
            jgen.writeEndArray();
        }

        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            ObjectNode o = createSchemaNode("array", true);
            if (typeHint instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) typeHint).getActualTypeArguments();
                if (typeArgs.length == 1) {
                    JavaType javaType = TypeFactory.type(typeArgs[0]);
                    JsonSerializer<Object> ser = provider.findNonTypedValueSerializer(javaType.getRawClass());
                    JsonNode schemaNode = (ser instanceof SchemaAware) ?
                            ((SchemaAware) ser).getSchema(provider, null) :
                            JsonSchema.getDefaultSchemaNode();
                    o.put("items", schemaNode);
                }
            }
            return o;
        }
    }

    public final static class IterableSerializer
        extends SerializerBase<Iterable<?>>
    {
        public final static IterableSerializer instance = new IterableSerializer();

        public IterableSerializer() { super(Iterable.class, false); }
        
        @Override
        public void serialize(Iterable<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            Iterator<?> it = value.iterator();
            if (it.hasNext()) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                do {
                    Object elem = it.next();
                    if (elem == null) {
                        provider.getNullValueSerializer().serialize(null, jgen, provider);
                    } else {
                        // Minor optimization to avoid most lookups:
                        Class<?> cc = elem.getClass();
                        JsonSerializer<Object> currSerializer;
                        if (cc == prevClass) {
                            currSerializer = prevSerializer;
                        } else {
                            currSerializer = provider.findTypedValueSerializer(cc, cc, true);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                } while (it.hasNext());
            }
            jgen.writeEndArray();
        }

        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            ObjectNode o = createSchemaNode("array", true);
            if (typeHint instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) typeHint).getActualTypeArguments();
                if (typeArgs.length == 1) {
                    JavaType javaType = TypeFactory.type(typeArgs[0]);
                    JsonSerializer<Object> ser = provider.findNonTypedValueSerializer(javaType.getRawClass());
                    JsonNode schemaNode = (ser instanceof SchemaAware) ?
                            ((SchemaAware) ser).getSchema(provider, null) :
                            JsonSchema.getDefaultSchemaNode();
                    o.put("items", schemaNode);
                }
            }
            return o;
        }
    }

    public final static class EnumSetSerializer
        extends SerializerBase<EnumSet<? extends Enum<?>>>
    {
        public EnumSetSerializer() { super(EnumSet.class, false); }

        @Override
        public void serialize(EnumSet<? extends Enum<?>> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            JsonSerializer<Object> enumSer = null;
            /* Need to dynamically find instance serializer; unfortunately
             * that seems to be the only way to figure out type (no accessors
             * to the enum class that set knows)
             */
            for (Enum<?> en : value) {
                if (enumSer == null) {
                    /* 12-Jan-2010, tatu: Since enums can not be polymorphic, let's
                     *   not bother with typed serializer variant here
                     */
                    enumSer = provider.findNonTypedValueSerializer(en.getDeclaringClass());
                }
                enumSer.serialize(en, jgen, provider);
            }
            jgen.writeEndArray();
        }

        
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            ObjectNode o = createSchemaNode("array", true);
            if (typeHint instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) typeHint).getActualTypeArguments();
                if (typeArgs.length == 1) {
                    JavaType javaType = TypeFactory.type(typeArgs[0]);
                    JsonSerializer<Object> ser = provider.findNonTypedValueSerializer(javaType.getRawClass());
                    JsonNode schemaNode = (ser instanceof SchemaAware) ?
                            ((SchemaAware) ser).getSchema(provider, null) :
                            JsonSchema.getDefaultSchemaNode();
                    o.put("items", schemaNode);
                }
            }
            return o;
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, Maps
    ////////////////////////////////////////////////////////////
     */

    /**
     * Deprecated map serializer; starting with version 1.4, there
     * is non-inner class version
     * 
     * @deprecated Since 1.4, please use the non-inner class
     *    {@link org.codehaus.jackson.map.ser.MapSerializer} instead.
s     */
    @Deprecated
    public final static class MapSerializer
        extends org.codehaus.jackson.map.ser.MapSerializer { }

    public final static class EnumMapSerializer
        extends SerializerBase<EnumMap<? extends Enum<?>, ?>>
    {
        public EnumMapSerializer() { super(EnumMap.class, false); }

        @Override
        public void serialize(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartObject();
            if (!value.isEmpty()) {
                serializeFields(value, jgen, provider);
            }        
            jgen.writeEndObject();
        }

        @Override
        public void serializeWithType(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            typeSer.writeTypePrefixForValue(value, jgen);
            if (!value.isEmpty()) {
                serializeFields(value, jgen, provider);
            }
            typeSer.writeTypeSuffixForValue(value, jgen);
        }
        
        protected void serializeFields(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            JsonSerializer<Object> prevSerializer = null;
            Class<?> prevClass = null;

            // for efficient key serialization, we need this:
            EnumValues enumValues = null;

            for (Map.Entry<? extends Enum<?>,?> entry : value.entrySet()) {
                // First, serialize key
                Enum<?> key = entry.getKey();
                if (enumValues == null) {
                    /* 15-Oct-2009, tatu: This is bit clumsy, but still the
                     * simplest efficient way to do it currently,
                     * as Serializers get cached.
                     * (it does assume we'll always use default serializer
                     * tho -- so ideally code should be rewritten)
                     */
                    // ... and lovely two-step casting process too...
                    // and as earlier, enums can not be polymorphic, can use non-typed variants
                    SerializerBase<?> ser = (SerializerBase<?>) provider.findNonTypedValueSerializer(key.getDeclaringClass());
                    enumValues = ((EnumSerializer) ser).getEnumValues();
                }
                jgen.writeFieldName(enumValues.valueFor(key));
                // And then value
                Object valueElem = entry.getValue();
                if (valueElem == null) {
                    provider.getNullValueSerializer().serialize(null, jgen, provider);
                } else {
                    Class<?> cc = valueElem.getClass();
                    JsonSerializer<Object> currSerializer;
                    if (cc == prevClass) {
                        currSerializer = prevSerializer;
                    } else {
                        currSerializer = provider.findTypedValueSerializer(cc, cc, true);
                        prevSerializer = currSerializer;
                        prevClass = cc;
                    }
                    try {
                        currSerializer.serialize(valueElem, jgen, provider);
                    } catch (Exception e) {
                        // [JACKSON-55] Need to add reference information
                        wrapAndThrow(e, value, entry.getKey().name());
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        //@Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            ObjectNode o = createSchemaNode("object", true);
            if (typeHint instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) typeHint).getActualTypeArguments();
                if (typeArgs.length == 2) {
                    JavaType enumType = TypeFactory.type(typeArgs[0]);
                    JavaType valueType = TypeFactory.type(typeArgs[1]);
                    ObjectNode propsNode = JsonNodeFactory.instance.objectNode();
                    Class<Enum<?>> enumClass = (Class<Enum<?>>) enumType.getRawClass();
                    for (Enum<?> enumValue : enumClass.getEnumConstants()) {
                        JsonSerializer<Object> ser = provider.findNonTypedValueSerializer(valueType.getRawClass());
                        JsonNode schemaNode = (ser instanceof SchemaAware) ?
                                ((SchemaAware) ser).getSchema(provider, null) :
                                JsonSchema.getDefaultSchemaNode();
                        propsNode.put(provider.getConfig().getAnnotationIntrospector().findEnumValue((Enum<?>)enumValue), schemaNode);
                    }
                    o.put("properties", propsNode);
                }
            }
            return o;
        }
    }
}
