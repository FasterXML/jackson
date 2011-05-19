package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Helper class used to contain simple/well-known deserializers for core JDK types
 */
class StdDeserializers
{
    final HashMap<JavaType, JsonDeserializer<Object>> _deserializers = new HashMap<JavaType, JsonDeserializer<Object>>();

    private StdDeserializers()
    {
        // First, add the fall-back "untyped" deserializer:
        add(new UntypedObjectDeserializer());

        // Then String and String-like converters:
        StdDeserializer<?> strDeser = new StdDeserializer.StringDeserializer();
        add(strDeser, String.class);
        add(strDeser, CharSequence.class);
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
        add(new TimestampDeserializer());
        add(new StdDeserializer.CalendarDeserializer());
        /* 24-Jan-2010, tatu: When including type information, we may
         *    know that we specifically need GregorianCalendar...
         */
        add(new StdDeserializer.CalendarDeserializer(GregorianCalendar.class),
                GregorianCalendar.class);

        // From-string deserializers:
        for (StdDeserializer<?> deser : FromStringDeserializer.all()) {
            add(deser);
        }

        // And finally some odds and ends

        // to deserialize Throwable, need stack trace elements:
        add(new StdDeserializer.StackTraceElementDeserializer());

        // Plus TokenBuffer is a core type since 1.5
        add(new StdDeserializer.TokenBufferDeserializer());
        // [JACKSON-283] need to support atomic types, too
        add(new StdDeserializer.AtomicBooleanDeserializer());
    }

    /**
     * Public accessor to deserializers for core types.
     */
    public static HashMap<JavaType, JsonDeserializer<Object>> constructAll()
    {
        return new StdDeserializers()._deserializers;
    }

    private void add(StdDeserializer<?> stdDeser)
    {
        add(stdDeser, stdDeser.getValueClass());
    }

    private void add(StdDeserializer<?> stdDeser, Class<?> valueClass)
    {
        // must do some unfortunate casting here...
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> deser = (JsonDeserializer<Object>) stdDeser;
        // Not super clean, but default TypeFactory does work here:
        _deserializers.put(TypeFactory.defaultInstance().constructType(valueClass), deser);
    }
}
