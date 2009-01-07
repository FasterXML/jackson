package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.type.*;

/**
 * Helper class used to contain simple/well-known deserializers for JDK types
 */
class StdDeserializers
{
    final HashMap<JavaType, JsonDeserializer<Object>> _deserializers = new HashMap<JavaType, JsonDeserializer<Object>>();

    private StdDeserializers()
    {
        // First, add the fall-back "untyped" deserializer:
        add(new UntypedObjectDeserializer());

        // Then String and String-like converters:
        add(new StdDeserializer.StringDeserializer());

        // Then primitives/wrappers
        add(new StdDeserializer.BooleanDeserializer());
        add(new StdDeserializer.ByteDeserializer());
        add(new StdDeserializer.ShortDeserializer());
        add(new StdDeserializer.CharacterDeserializer());
        add(new StdDeserializer.IntegerDeserializer());
        add(new StdDeserializer.LongDeserializer());
        add(new StdDeserializer.FloatDeserializer());
        add(new StdDeserializer.DoubleDeserializer());
    }

    private HashMap<JavaType, JsonDeserializer<Object>> getDeserializers() {
        return _deserializers;
    }

    public static HashMap<JavaType, JsonDeserializer<Object>> constructAll()
    {
        return new StdDeserializers().getDeserializers();
    }

    void add(StdDeserializer<?> stdDeser)
    {
        // must do some unfortunate casting here...
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> deser = (JsonDeserializer<Object>) stdDeser;

        Class<?> valueClass = stdDeser.getValueClass();
        _deserializers.put(TypeFactory.instance.fromClass(valueClass), deser);
    }
}
