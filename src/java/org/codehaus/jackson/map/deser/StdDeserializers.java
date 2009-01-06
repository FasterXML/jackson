package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.type.*;

/**
 * Helper class used to contain simple/well-known serializers for JDK types
 */
class StdDeserializers
{
    final HashMap<JavaType, JsonDeserializer<Object>> _deserializers = new HashMap<JavaType, JsonDeserializer<Object>>();

    private StdDeserializers()
    {
        // First, add the fall-back "untyped" deserializer:
        add(Object.class, new UntypedObjectDeserializer());

        // Then String and String-like converters:
        add(String.class, new StdDeserializer.StringDeserializer());

        // Then primitives/wrappers
        add(Boolean.class, new StdDeserializer.BooleanDeserializer());
        add(Integer.class, new StdDeserializer.IntegerDeserializer());
    }

    private HashMap<JavaType, JsonDeserializer<Object>> getDeserializers() {
        return _deserializers;
    }

    public static HashMap<JavaType, JsonDeserializer<Object>> constructAll()
    {
        return new StdDeserializers().getDeserializers();
    }

    @SuppressWarnings("unchecked")
	<T> void add(Class<T> clz, JsonDeserializer<T> typedDeser)
    {
        // must do some unfortunate casting here...
        JsonDeserializer<Object> deser = (JsonDeserializer<Object>) typedDeser;
        _deserializers.put(TypeFactory.instance.fromClass(clz), deser);
    }
}
