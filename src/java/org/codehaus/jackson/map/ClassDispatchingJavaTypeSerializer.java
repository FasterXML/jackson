package org.codehaus.jackson.map;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;

/**
 * A {@link JavaTypeSerializer} implementation that dispatches serialization of concrete
 * types to specific registered {@link JavaTypeSerializer}, based on the class of the
 * object ot be serialized.
 *
 * @author Stanislaw Osinski
 */
public class ClassDispatchingJavaTypeSerializer extends JavaTypeSerializerBase<Object>
{
    private final Map<Class<?>, JavaTypeSerializer<?>> serializerByClass;

    /**
     * Creates the serializer with the provided {@link Class} to
     * {@link JavaTypeSerializer} mappings.
     */
    public ClassDispatchingJavaTypeSerializer(Map<Class<?>, JavaTypeSerializer<?>> serializerByClass)
    {
        this.serializerByClass = serializerByClass;
    }

    /**
     * Default constructor that will construct an instance with no
     * configured per-class serializers.
     */
    public ClassDispatchingJavaTypeSerializer()
    {
        this(new HashMap<Class<?>, JavaTypeSerializer<?>>());
    }

    public <T> void addSerializer(Class<T> forClass, JavaTypeSerializer<T> serializer)
    {
        serializerByClass.put(forClass, serializer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean writeAny(JavaTypeSerializer<Object> defaultSerializer,
        JsonGenerator jgen, Object value) throws IOException, JsonParseException
    {
        // Alas, hard to find proper cast here...
        final JavaTypeSerializer javaTypeSerializer = serializerByClass.get(value.getClass());
        if (javaTypeSerializer != null) {
            if (javaTypeSerializer.writeAny(defaultSerializer, jgen, value)) {
                return true;
            }
        }
        return super.writeAny(defaultSerializer, jgen, value);
    }
}
