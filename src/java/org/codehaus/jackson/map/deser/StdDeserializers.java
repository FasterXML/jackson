package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.type.JavaType;

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
        add(new StdDeserializer.ClassDeserializer());

        // Then primitives/wrappers
        add(new StdDeserializer.BooleanDeserializer(), Boolean.class, Boolean.TYPE);
        add(new StdDeserializer.ByteDeserializer(), Byte.class, Byte.TYPE);
        add(new StdDeserializer.ShortDeserializer(), Short.class, Short.TYPE);
        add(new StdDeserializer.CharacterDeserializer(), Character.class, Character.TYPE);
        add(new StdDeserializer.IntegerDeserializer(), Integer.class, Integer.TYPE);
        add(new StdDeserializer.LongDeserializer(), Long.class, Long.TYPE);
        add(new StdDeserializer.FloatDeserializer(), Float.class, Float.TYPE);
        add(new StdDeserializer.DoubleDeserializer(), Double.class, Double.TYPE);

        // and related
        add(new StdDeserializer.BigDecimalDeserializer());
        add(new StdDeserializer.BigIntegerDeserializer());

        add(new StdDeserializer.UtilDateDeserializer());
        add(new StdDeserializer.SqlDateDeserializer());
        add(new StdDeserializer.CalendarDeserializer());

        // Then other simple types:
        add(new FromStringDeserializer.UUIDDeserializer());
        add(new FromStringDeserializer.URLDeserializer());
        add(new FromStringDeserializer.URIDeserializer());
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
        add(stdDeser, stdDeser.getValueClass());
    }

    void add(StdDeserializer<?> stdDeser, Class<?>... valueClasses)
    {
        // must do some unfortunate casting here...
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> deser = (JsonDeserializer<Object>) stdDeser;

        for (Class<?> valueClass : valueClasses) {
            _deserializers.put(TypeFactory.fromClass(valueClass), deser);
        }
    }
}
