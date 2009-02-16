package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Dummy container class to group standard container serializers: serializers
 * that can serialize things like {@link java.util.List}s,
 * {@link java.util.Maps}s and such.
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
        extends JsonSerializer<List<?>>
    {
        public final static IndexedListSerializer instance = new IndexedListSerializer();

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
                                currSerializer = provider.findValueSerializer(cc);
                                prevSerializer = currSerializer;
                                prevClass = cc;
                            }
                            currSerializer.serialize(elem, jgen, provider);
                        }
                    }
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    throw JsonMappingException.wrapWithPath(e, value, i);
                }
              }

            jgen.writeEndArray();
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
        extends JsonSerializer<Collection<?>>
    {
        public final static CollectionSerializer instance = new CollectionSerializer();

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
                                currSerializer = provider.findValueSerializer(cc);
                                prevSerializer = currSerializer;
                                prevClass = cc;
                            }
                            currSerializer.serialize(elem, jgen, provider);
                        }
                        ++i;
                    } while (it.hasNext());
                } catch (Exception e) {
                    // [JACKSON-55] Need to add reference information
                    throw JsonMappingException.wrapWithPath(e, value, i);
                }
            }
            jgen.writeEndArray();
        }
    }

    public final static class IteratorSerializer
        extends JsonSerializer<Iterator<?>>
    {
        public final static IteratorSerializer instance = new IteratorSerializer();

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
                            currSerializer = provider.findValueSerializer(cc);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                } while (value.hasNext());
            }
            jgen.writeEndArray();
        }
    }

    public final static class IterableSerializer
        extends JsonSerializer<Iterable<?>>
    {
        public final static IterableSerializer instance = new IterableSerializer();

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
                            currSerializer = provider.findValueSerializer(cc);
                            prevSerializer = currSerializer;
                            prevClass = cc;
                        }
                        currSerializer.serialize(elem, jgen, provider);
                    }
                } while (it.hasNext());
            }
            jgen.writeEndArray();
        }
    }

    public final static class EnumSetSerializer
        extends JsonSerializer<EnumSet<? extends Enum<?>>>
    {
        public final static CollectionSerializer instance = new CollectionSerializer();

        @Override
            public void serialize(EnumSet<? extends Enum<?>> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartArray();
            for (Enum<?> en : value) {
                jgen.writeString(en.name());
            }
            jgen.writeEndArray();
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Concrete serializers, Maps
    ////////////////////////////////////////////////////////////
     */

    public final static class MapSerializer
        extends JsonSerializer<Map<?,?>>
    {
        public final static MapSerializer instance = new MapSerializer();

        @Override
            public void serialize(Map<?,?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartObject();

            if (!value.isEmpty()) {
                final JsonSerializer<Object> keySerializer = provider.getKeySerializer();
                JsonSerializer<Object> prevValueSerializer = null;
                Class<?> prevValueClass = null;

                Object keyElem, valueElem;

                for (Map.Entry<?,?> entry : value.entrySet()) {
                    // First, serialize key
                    keyElem = entry.getKey();
                    if (keyElem == null) {
                        provider.getNullKeySerializer().serialize(null, jgen, provider);
                    } else {
                        keySerializer.serialize(keyElem, jgen, provider);
                    }

                    // And then value
                    valueElem = entry.getValue();
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
                            throw JsonMappingException.wrapWithPath(e, value, keyDesc);
                        }
                    }
                }
            }
                
            jgen.writeEndObject();
        }
    }

    public final static class EnumMapSerializer
        extends JsonSerializer<EnumMap<? extends Enum<?>, ?>>
    {
        @Override
            public void serialize(EnumMap<? extends Enum<?>,?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartObject();
            JsonSerializer<Object> prevSerializer = null;
            Class<?> prevClass = null;

            for (Map.Entry<? extends Enum<?>,?> entry : value.entrySet()) {
                // First, serialize key
                jgen.writeFieldName(entry.getKey().name());
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
                        throw JsonMappingException.wrapWithPath(e, value, entry.getKey().name());
                    }
                }
            }
            jgen.writeEndObject();
        }
    }
}
