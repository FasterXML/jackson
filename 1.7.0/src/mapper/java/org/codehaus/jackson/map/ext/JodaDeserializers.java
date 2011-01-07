package org.codehaus.jackson.map.ext;

import java.io.IOException;
import java.util.*;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.StdDeserializer;
import org.codehaus.jackson.map.deser.StdScalarDeserializer;
import org.codehaus.jackson.map.util.Provider;

/**
 * Provider for deserializers that handle some basic data types
 * for <a href="http://joda-time.sourceforge.net/">Joda</a> date/time library.
 *
 * @since 1.4
 */
public class JodaDeserializers
    implements Provider<StdDeserializer<?>>
{
    public Collection<StdDeserializer<?>> provide() {
        return Arrays.asList(new StdDeserializer<?>[] {
                new DateTimeDeserializer<DateTime>(DateTime.class)
                ,new DateTimeDeserializer<ReadableDateTime>(ReadableDateTime.class)
                ,new DateTimeDeserializer<ReadableInstant>(ReadableInstant.class)
                ,new LocalDateDeserializer()
                ,new LocalDateTimeDeserializer()
                ,new DateMidnightDeserializer()
        });
    }

    /*
    /*********************************************************************
    /* Intermediate base classes
    /*********************************************************************
    */

    abstract static class JodaDeserializer<T> extends StdScalarDeserializer<T>
    {
        final static DateTimeFormatter _localDateTimeFormat = ISODateTimeFormat.localDateOptionalTimeParser();

        protected JodaDeserializer(Class<T> cls) { super(cls); }

        protected DateTime parseLocal(JsonParser jp)
            throws IOException, JsonProcessingException
        {
            String str = jp.getText().trim();
            if (str.length() == 0) { // [JACKSON-360]
                return null;
            }
            return _localDateTimeFormat.parseDateTime(str);
        }
    }
    
    /*
    /*********************************************************************
    /* Concrete deserializers
    /*********************************************************************
    */

    /**
     * Basic deserializer for {@link DateTime}. Accepts JSON String and Number
     * values and passes those to single-argument constructor.
     * Does not (yet?) support JSON object; support can be added if desired.
     *<p>
     * Since 1.6 this has been generic, to handle multiple related types,
     * including super types of {@link DateTime}
     */
    public static class DateTimeDeserializer<T extends ReadableInstant>
        extends JodaDeserializer<T>
    {
        public DateTimeDeserializer(Class<T> cls) { super(cls); }

        @SuppressWarnings("unchecked")
        @Override
        public T deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return (T) new DateTime(jp.getLongValue(), DateTimeZone.UTC);
            }
            if (t == JsonToken.VALUE_STRING) {
                String str = jp.getText().trim();
                if (str.length() == 0) { // [JACKSON-360]
                    return null;
                }
                return (T) new DateTime(str, DateTimeZone.UTC);
            }
            throw ctxt.mappingException(getValueClass());
        }
    }

    /**
     * @since 1.5
     */
    public static class LocalDateDeserializer
        extends JodaDeserializer<LocalDate>
    {
        public LocalDateDeserializer() { super(LocalDate.class); }
    
        @Override
        public LocalDate deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // We'll accept either long (timestamp) or array:
            if (jp.isExpectedStartArrayToken()) {
                jp.nextToken(); // VALUE_NUMBER_INT 
                int year = jp.getIntValue(); 
                jp.nextToken(); // VALUE_NUMBER_INT
                int month = jp.getIntValue();
                jp.nextToken(); // VALUE_NUMBER_INT
                int day = jp.getIntValue();
                if (jp.nextToken() != JsonToken.END_ARRAY) {
                    ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, "after LocalDate ints");
                }
                return new LocalDate(year, month, day);
            }
            switch (jp.getCurrentToken()) {
            case VALUE_NUMBER_INT:
                return new LocalDate(jp.getLongValue());            
            case VALUE_STRING:
                DateTime local = parseLocal(jp);
                if (local == null) {
                    return null;
                }
                return local.toLocalDate();
            }
            ctxt.wrongTokenException(jp, JsonToken.START_ARRAY, "expected JSON Array, String or Number");
            return null;
        }
    }

    /**
     * @since 1.5
     */
    public static class LocalDateTimeDeserializer
        extends JodaDeserializer<LocalDateTime>
    {
        public LocalDateTimeDeserializer() { super(LocalDateTime.class); }
    
        @Override
        public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // We'll accept either long (timestamp) or array:
            if (jp.isExpectedStartArrayToken()) {
                jp.nextToken(); // VALUE_NUMBER_INT
                int year = jp.getIntValue();
                jp.nextToken(); // VALUE_NUMBER_INT
                int month = jp.getIntValue();
                jp.nextToken(); // VALUE_NUMBER_INT
                int day = jp.getIntValue();
                jp.nextToken(); // VALUE_NUMBER_INT
                int hour = jp.getIntValue();
                jp.nextToken(); // VALUE_NUMBER_INT
                int minute = jp.getIntValue();
                jp.nextToken(); // VALUE_NUMBER_INT
                int second = jp.getIntValue();
                // let's leave milliseconds optional?
                int millisecond = 0;
                if (jp.nextToken() != JsonToken.END_ARRAY) { // VALUE_NUMBER_INT           
                    millisecond = jp.getIntValue();
                    jp.nextToken(); // END_ARRAY?
                }
                if (jp.getCurrentToken() != JsonToken.END_ARRAY) {
                    ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, "after LocalDateTime ints");
                }
                return new LocalDateTime(year, month, day, hour, minute, second, millisecond);                 
            }

            switch (jp.getCurrentToken()) {
            case VALUE_NUMBER_INT:
                return new LocalDateTime(jp.getLongValue());            
            case VALUE_STRING:
                DateTime local = parseLocal(jp);
                if (local == null) {
                    return null;
                }
                return local.toLocalDateTime();
            }
            ctxt.wrongTokenException(jp, JsonToken.START_ARRAY, "expected JSON Array or Number");
            return null;
        }
    }

    /**
     * @since 1.5
     */
    public static class DateMidnightDeserializer
        extends JodaDeserializer<DateMidnight>
    {
        public DateMidnightDeserializer() { super(DateMidnight.class); }
    
        @Override
        public DateMidnight deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // We'll accept either long (timestamp) or array:
            if (jp.isExpectedStartArrayToken()) {
                jp.nextToken(); // VALUE_NUMBER_INT 
                int year = jp.getIntValue(); 
                jp.nextToken(); // VALUE_NUMBER_INT
                int month = jp.getIntValue();
                jp.nextToken(); // VALUE_NUMBER_INT
                int day = jp.getIntValue();
                if (jp.nextToken() != JsonToken.END_ARRAY) {
                    ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, "after DateMidnight ints");
                }
                return new DateMidnight(year, month, day);
            }
            switch (jp.getCurrentToken()) {
            case VALUE_NUMBER_INT:
                return new DateMidnight(jp.getLongValue());            
            case VALUE_STRING:
                DateTime local = parseLocal(jp);
                if (local == null) {
                    return null;
                }
                return local.toDateMidnight();
            }
            ctxt.wrongTokenException(jp, JsonToken.START_ARRAY, "expected JSON Array, Number or String");
            return null;
        }
    }
}
