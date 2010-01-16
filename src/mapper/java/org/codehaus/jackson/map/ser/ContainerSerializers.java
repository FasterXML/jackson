package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
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
     ****************************************************************
     * Factory methods
     ****************************************************************
     */
    
    public static JsonSerializer<?> indexedListSerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer typeSer)
    {
        return new IndexedListSerializer(elemType, staticTyping, typeSer);
    }

    public static JsonSerializer<?> collectionSerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer typeSer)
    {
        return new CollectionSerializer(elemType, staticTyping, typeSer);
    }

    public static JsonSerializer<?> iteratorSerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer typeSer)
    {
        return new IteratorSerializer(elemType, staticTyping, typeSer);
    }

    public static JsonSerializer<?> iterableSerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer typeSer)
    {
        return new IterableSerializer(elemType, staticTyping, typeSer);
    }

    public static JsonSerializer<?> enumSetSerializer(JavaType enumType)
    {
        return new EnumSetSerializer(enumType);
    }

    public static JsonSerializer<?> enumMapSerializer(JavaType valueType, boolean staticTyping,
            TypeSerializer valueTypeSer, EnumValues keyEnums)
    {
        return new EnumMapSerializer(valueType, staticTyping, valueTypeSer, keyEnums);
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
        implements ResolvableSerializer
    {
        protected final boolean _staticTyping;

        protected final JavaType _elementType;
        
        /**
         * Type serializer used for values, if any.
         * 
         * @since 1.5
         */
        protected final TypeSerializer _elementTypeSerializer;

        /**
         * Value serializer to use, if it can be statically determined
         * 
         * @since 1.5
         */
        protected JsonSerializer<Object> _elementSerializer;

        protected AsArraySerializer(Class<?> cls, JavaType et, boolean staticTyping,
                TypeSerializer typeSer)
        {
            // typing with generics is messy... have to resort to this:
            super(cls, false);
            _elementType = et;
            // static if explicitly requested, or we got final type
            _staticTyping = staticTyping || (et != null && et.isFinal());
            _elementTypeSerializer = typeSer;
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

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            /* 15-Jan-2010, tatu: This should probably be rewritten, given that
             *    more information about content type is actually being explicitly
             *    passed. So there should be less need to try to re-process that
             *    information.
             */
            ObjectNode o = createSchemaNode("array", true);
            JavaType contentType = null;
            if (typeHint != null) {
                JavaType javaType = TypeFactory.type(typeHint);
                contentType = javaType.getContentType();
                if (contentType == null) { // could still be parametrized (Iterators)
                    if (typeHint instanceof ParameterizedType) {
                        Type[] typeArgs = ((ParameterizedType) typeHint).getActualTypeArguments();
                        if (typeArgs.length == 1) {
                            contentType = TypeFactory.type(typeArgs[0]);
                        }
                    }
                }
            }
            if (contentType == null && _elementType != null) {
                contentType = _elementType;
            }
            if (contentType != null) {
                JsonSerializer<Object> ser = provider.findValueSerializer(contentType.getRawClass());
                JsonNode schemaNode = (ser instanceof SchemaAware) ?
                        ((SchemaAware) ser).getSchema(provider, null) :
                        JsonSchema.getDefaultSchemaNode();
                o.put("items", schemaNode);
            }
            return o;
        }

        /**
         * Need to get callback to resolve value serializer, if static typing
         * is used (either being forced, or because value type is final)
         */
        @Override
        public void resolve(SerializerProvider provider)
            throws JsonMappingException
        {
            if (_staticTyping && _elementType != null) {
                _elementSerializer = provider.findValueSerializer(_elementType.getRawClass());
            }
        }
    }
    
    /*
    ************************************************************
    * Concrete serializers, Lists/collections
    ************************************************************
     */

    /**
     * This is an optimizied serializer for Lists that can be efficiently
     * traversed by index (as opposed to others, such as {@link LinkedList}
     * that can not}.
     */
    public final static class IndexedListSerializer
        extends AsArraySerializer<List<?>>
    {
        public final static IndexedListSerializer instance = new IndexedListSerializer(null, false, null);

        public IndexedListSerializer(JavaType elemType, boolean staticTyping, TypeSerializer typeSer)
        {
            super(List.class, elemType, staticTyping, typeSer);
        }
        
        @Override
        public void serializeContents(List<?> value, JsonGenerator jgen, SerializerProvider provider)
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
            final int len = value.size();
            if (len > 0) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                for (int i = 0; i < len; ++i) {
                    Object elem = value.get(i);
                    try {
                        if (elem == null) {
                            provider.getNullValueSerializer().serialize(null, jgen, provider);
                        } else {
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
                            currSerializer.serialize(elem, jgen, provider);
                        }
                    } catch (Exception e) {
                        // [JACKSON-55] Need to add reference information
                        wrapAndThrow(e, value, i);
                    }
                }
             }
        }

        public void serializeContentsUsing(List<?> value, JsonGenerator jgen, SerializerProvider provider,
                JsonSerializer<Object> ser)
            throws IOException, JsonGenerationException
        {
            final int len = value.size();
            if (len > 0) {
                final TypeSerializer typeSer = _elementTypeSerializer;
                for (int i = 0; i < len; ++i) {
                    Object elem = value.get(i);
                    try {
                        if (elem == null) {
                            provider.getNullValueSerializer().serialize(null, jgen, provider);
                        } else if (typeSer == null) {
                            ser.serialize(elem, jgen, provider);
                        } else {
                            ser.serializeWithType(elem, jgen, provider, typeSer);
                        }
                    } catch (Exception e) {
                        // [JACKSON-55] Need to add reference information
                        wrapAndThrow(e, value, i);
                    }
                }
             }
        }

        public void serializeTypedContents(List<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            final int len = value.size();
            if (len > 0) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                final TypeSerializer typeSer = _elementTypeSerializer;
                for (int i = 0; i < len; ++i) {
                    Object elem = value.get(i);
                    try {
                        if (elem == null) {
                            provider.getNullValueSerializer().serialize(null, jgen, provider);
                        } else {
                            Class<?> cc = elem.getClass();
                            JsonSerializer<Object> currSerializer;
                            if (cc == prevClass) {
                                currSerializer = prevSerializer;
                            } else {
                                currSerializer = provider.findValueSerializer(cc);
                                prevSerializer = currSerializer;
                                prevClass = cc;
                            }
                            currSerializer.serializeWithType(elem, jgen, provider, typeSer);
                        }
                    } catch (Exception e) {
                        // [JACKSON-55] Need to add reference information
                        wrapAndThrow(e, value, i);
                    }
                }
             }
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
        extends AsArraySerializer<Collection<?>>
    {
        public final static CollectionSerializer instance = new CollectionSerializer(null, false, null);

        public CollectionSerializer(JavaType elemType, boolean staticTyping, TypeSerializer typeSer)
        {
            super(Collection.class, elemType, staticTyping, typeSer);
        }
        
        @Override
        public void serializeContents(Collection<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            if (_elementSerializer != null) {
                serializeContentsUsing(value, jgen, provider, _elementSerializer);
                return;
            }
            Iterator<?> it = value.iterator();
            if (it.hasNext()) {
                JsonSerializer<Object> prevSerializer = null;
                Class<?> prevClass = null;
                TypeSerializer typeSer = _elementTypeSerializer;
    
                int i = 0;
                do {
                    Object elem = it.next();
                    try {
                        if (elem == null) {
                            provider.getNullValueSerializer().serialize(null, jgen, provider);
                        } else {
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
                            if (typeSer == null) {
                                currSerializer.serialize(elem, jgen, provider);
                            } else {
                                currSerializer.serializeWithType(elem, jgen, provider, typeSer);
                            }
                        }
                    } catch (Exception e) {
                        // [JACKSON-55] Need to add reference information
                        wrapAndThrow(e, value, i);
                    }
                    ++i;
                } while (it.hasNext());
            }
        }

        public void serializeContentsUsing(Collection<?> value, JsonGenerator jgen, SerializerProvider provider,
                JsonSerializer<Object> ser)
            throws IOException, JsonGenerationException
        {
            Iterator<?> it = value.iterator();
            if (it.hasNext()) {
                TypeSerializer typeSer = _elementTypeSerializer;
                int i = 0;
                do {
                    Object elem = it.next();
                    try {
                        if (elem == null) {
                            provider.getNullValueSerializer().serialize(null, jgen, provider);
                        } else {
                            if (typeSer == null) {
                                ser.serialize(elem, jgen, provider);
                            } else {
                                ser.serializeWithType(elem, jgen, provider, typeSer);
                            }
                        }
                        ++i;
                    } catch (Exception e) {
                        // [JACKSON-55] Need to add reference information
                        wrapAndThrow(e, value, i);
                    }
                } while (it.hasNext());
            }
        }
    }

    public final static class IteratorSerializer
        extends AsArraySerializer<Iterator<?>>
    {
        public final static IteratorSerializer instance = new IteratorSerializer();

        public IteratorSerializer() { this(null, false, null); }

        public IteratorSerializer(JavaType elemType, boolean staticTyping, TypeSerializer typeSer)
        {
            super(Iterator.class, elemType, staticTyping, typeSer);
        }
        
        @Override
        public void serializeContents(Iterator<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
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
                            currSerializer = provider.findValueSerializer(cc);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                } while (value.hasNext());
            }
        }
    }

    public final static class IterableSerializer
        extends AsArraySerializer<Iterable<?>>
    {
        public final static IterableSerializer instance = new IterableSerializer(null, false, null);

        public IterableSerializer(JavaType elemType, boolean staticTyping, TypeSerializer typeSer)
        {
            super(Iterable.class, elemType, staticTyping, typeSer);
        }
        
        @Override
        public void serializeContents(Iterable<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
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
                            currSerializer = provider.findValueSerializer(cc);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                } while (it.hasNext());
            }
        }
    }

    public final static class EnumSetSerializer
        extends AsArraySerializer<EnumSet<? extends Enum<?>>>
    {
        public EnumSetSerializer(JavaType elemType)
        {
            super(EnumSet.class, elemType, true, null);
        }

        @Override
        public void serializeContents(EnumSet<? extends Enum<?>> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            JsonSerializer<Object> enumSer = _elementSerializer;
            /* Need to dynamically find instance serializer; unfortunately
             * that seems to be the only way to figure out type (no accessors
             * to the enum class that set knows)
             */
            for (Enum<?> en : value) {
                if (enumSer == null) {
                    /* 12-Jan-2010, tatu: Since enums can not be polymorphic, let's
                     *   not bother with typed serializer variant here
                     */
                    enumSer = provider.findValueSerializer(en.getDeclaringClass());
                }
                enumSer.serialize(en, jgen, provider);
            }
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, Maps
    ////////////////////////////////////////////////////////////
     */

    /**
     * Deprecated map serializer; starting with version 1.4, there
     * is a non-inner class version.
     * 
     * @deprecated Since 1.4, please use the non-inner class
     *    {@link org.codehaus.jackson.map.ser.MapSerializer} instead.
s     */
    @Deprecated
    public final static class MapSerializer
        extends org.codehaus.jackson.map.ser.MapSerializer { }

    /**
     * Specialized serializer for {@link EnumMap}s. Somewhat tricky to
     * implement because actual Enum value type may not be available;
     * and if not, it can only be gotten from actual instance.
     */
    public final static class EnumMapSerializer
        extends SerializerBase<EnumMap<? extends Enum<?>, ?>>
        implements ResolvableSerializer
    {
        protected final boolean _staticTyping;

        /**
         * If we know enumeration used as key, this will contain
         * value set to use for serialization
         */
        protected final EnumValues _keyEnums;

        protected final JavaType _valueType;
        
        /**
         * Type serializer used for values, if any.
         * 
         * @since 1.5
         */
        protected final TypeSerializer _valueTypeSerializer;

        /**
         * Value serializer to use, if it can be statically determined
         * 
         * @since 1.5
         */
        protected JsonSerializer<Object> _valueSerializer;

        public EnumMapSerializer(JavaType valueType, boolean staticTyping, TypeSerializer typeSer,
                EnumValues keyEnums)
        {
            super(EnumMap.class, false);
            _staticTyping = staticTyping || (valueType != null && valueType.isFinal());
            _valueType = valueType;
            _valueTypeSerializer = typeSer;
            _keyEnums = keyEnums;
        }

        @Override
        public void serialize(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartObject();
            if (!value.isEmpty()) {
                serializeContents(value, jgen, provider);
            }        
            jgen.writeEndObject();
        }

        @Override
        public void serializeWithType(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            typeSer.writeTypePrefixForObject(value, jgen);
            if (!value.isEmpty()) {
                serializeContents(value, jgen, provider);
            }
            typeSer.writeTypeSuffixForObject(value, jgen);
        }
        
        protected void serializeContents(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            if (_valueSerializer != null) {
                serializeContentsUsing(value, jgen, provider, _valueSerializer);
                return;
            }
            JsonSerializer<Object> prevSerializer = null;
            Class<?> prevClass = null;
            EnumValues keyEnums = _keyEnums;

            for (Map.Entry<? extends Enum<?>,?> entry : value.entrySet()) {
                // First, serialize key
                Enum<?> key = entry.getKey();
                if (keyEnums == null) {
                    /* 15-Oct-2009, tatu: This is clumsy, but still the simplest efficient
                     * way to do it currently, as Serializers get cached. (it does assume we'll always use
                     * default serializer tho -- so ideally code should be rewritten)
                     */
                    // ... and lovely two-step casting process too...
                    SerializerBase<?> ser = (SerializerBase<?>) provider.findValueSerializer(key.getDeclaringClass());
                    keyEnums = ((EnumSerializer) ser).getEnumValues();
                }
                jgen.writeFieldName(keyEnums.valueFor(key));
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
                        currSerializer = provider.findValueSerializer(cc);
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

        protected void serializeContentsUsing(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider,
                JsonSerializer<Object> valueSer)
            throws IOException, JsonGenerationException
        {
            EnumValues keyEnums = _keyEnums;
            for (Map.Entry<? extends Enum<?>,?> entry : value.entrySet()) {
                Enum<?> key = entry.getKey();
                if (keyEnums == null) {
                    // clumsy, but has to do for now:
                    SerializerBase<?> ser = (SerializerBase<?>) provider.findValueSerializer(key.getDeclaringClass());
                    keyEnums = ((EnumSerializer) ser).getEnumValues();
                }
                jgen.writeFieldName(keyEnums.valueFor(key));
                Object valueElem = entry.getValue();
                if (valueElem == null) {
                    provider.getNullValueSerializer().serialize(null, jgen, provider);
                } else {
                    try {
                        valueSer.serialize(valueElem, jgen, provider);
                    } catch (Exception e) {
                        wrapAndThrow(e, value, entry.getKey().name());
                    }
                }
            }
        }

        @Override
        public void resolve(SerializerProvider provider)
            throws JsonMappingException
        {
            if (_staticTyping) {
                _valueSerializer = provider.findValueSerializer(_valueType.getRawClass());
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
                        JsonSerializer<Object> ser = provider.findValueSerializer(valueType.getRawClass());
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
