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

        // Then primitive-wrappers (simple):
        add(new StdDeserializer.BooleanDeserializer(Boolean.class, null));
        add(new StdDeserializer.ByteDeserializer(Byte.class, null));
        add(new StdDeserializer.ShortDeserializer(Short.class, null));
        add(new StdDeserializer.CharacterDeserializer(Character.class, null));
        add(new StdDeserializer.IntegerDeserializer(Integer.class, null));
        add(new StdDeserializer.LongDeserializer(Long.class, null));
        add(new StdDeserializer.FloatDeserializer(Float.class, null));
        add(new StdDeserializer.DoubleDeserializer(Double.class, null));
        
        /* And actual primitives: difference is the way nulls are to be
         * handled...
         */
        add(new StdDeserializer.BooleanDeserializer(Boolean.TYPE, Boolean.FALSE));
        add(new StdDeserializer.ByteDeserializer(Byte.TYPE, Byte.valueOf((byte)(0))));
        add(new StdDeserializer.ShortDeserializer(Short.TYPE, Short.valueOf((short)0)));
        add(new StdDeserializer.CharacterDeserializer(Character.TYPE, Character.valueOf('\0')));
        add(new StdDeserializer.IntegerDeserializer(Integer.TYPE, Integer.valueOf(0)));
        add(new StdDeserializer.LongDeserializer(Long.TYPE, Long.valueOf(0L)));
        add(new StdDeserializer.FloatDeserializer(Float.TYPE, Float.valueOf(0.0f)));
        add(new StdDeserializer.DoubleDeserializer(Double.TYPE, Double.valueOf(0.0)));
        
        // and related
        add(new StdDeserializer.NumberDeserializer());
        add(new StdDeserializer.BigDecimalDeserializer());
        add(new StdDeserializer.BigIntegerDeserializer());
        
        add(new DateDeserializer());
        add(new StdDeserializer.SqlDateDeserializer());
        add(new StdDeserializer.CalendarDeserializer());
        
        // Then other simple types:
        add(new FromStringDeserializer.UUIDDeserializer());
        add(new FromStringDeserializer.URLDeserializer());
        add(new FromStringDeserializer.URIDeserializer());

        // And finally some odds and ends

        // to deserialize Throwable, need stack trace elements:
        add(new StdDeserializer.StackTraceElementDeserializer());
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
