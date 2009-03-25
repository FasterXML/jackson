package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationContext;

/**
 * Base class for simple standard deserializers
 */
public abstract class StdDeserializer<T>
    extends JsonDeserializer<T>
{
    final Class<?> _valueClass;

    protected StdDeserializer(Class<?> vc) {
        _valueClass = vc;
    }

    public Class<?> getValueClass() { return _valueClass; }

    /*
    /////////////////////////////////////////////////////////////
    // Helper methods for sub-classes
    /////////////////////////////////////////////////////////////
    */

    protected int _parseInt(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return jp.getIntValue();
        }
        if (t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
            return jp.getIntValue();
        }
        if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
            // !!! 05-Jan-2009, tatu: Should we try to limit value space, JDK is too lenient?
            String text = jp.getText().trim();
            try {
                return Integer.parseInt(text);
            } catch (IllegalArgumentException iae) {
                throw ctxt.weirdStringException(_valueClass, "not a valid representation of integral number value");
            }
        }
        // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass);
    }

    protected double _parseDouble(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
            // We accept couple of different types; obvious ones first:
            JsonToken t = jp.getCurrentToken();

            if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                return jp.getDoubleValue();
            }
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return jp.getDoubleValue();
            }
            // And finally, let's allow Strings to be converted too
            if (t == JsonToken.VALUE_STRING) {
                // !!! 05-Jan-2009, tatu: Should we try to limit value space, JDK is too lenient?
                String text = jp.getText().trim();
                try {
                    return Double.parseDouble(text);
                } catch (IllegalArgumentException iae) { }
                throw ctxt.weirdStringException(_valueClass, "not a valid double value");
            }
            // Otherwise, no can do:
            throw ctxt.mappingException(_valueClass);
    }

    protected java.util.Date _parseDate(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        try {
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return new java.util.Date(jp.getLongValue());
            }
            if (t == JsonToken.VALUE_STRING) {
                return ctxt.parseDate(jp.getText());
            }
            throw ctxt.mappingException(_valueClass);
        } catch (IllegalArgumentException iae) {
            throw ctxt.weirdStringException(_valueClass, "not a valid representation (error: "+iae.getMessage()+")");
        }
    }

    /*
    /////////////////////////////////////////////////////////////
    // Then one intermediate base class for things that have
    // both primitive and wrapper types
    /////////////////////////////////////////////////////////////
    */

    protected abstract static class PrimitiveOrWrapperDeserializer<T>
        extends StdDeserializer<T>
    {
        final T _nullValue;
        
        protected PrimitiveOrWrapperDeserializer(Class<T> vc, T nvl)
        {
            super(vc);
            _nullValue = nvl;
        }
        
        public final T getNullValue() { return _nullValue; }
    }

    /*
    /////////////////////////////////////////////////////////////
    // First, generic (Object, String, String-like, Class) deserializers
    /////////////////////////////////////////////////////////////
    */

    public final static class StringDeserializer
        extends StdDeserializer<String>
    {
        public StringDeserializer() { super(String.class); }

        @Override
            public String deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken curr = jp.getCurrentToken();
            // Usually should just get string value:
            if (curr == JsonToken.VALUE_STRING) {
                return jp.getText();
            }
            // Can deserialize any scaler value, but not markers
            if (curr.isScalarValue()) {
                return jp.getText();
            }
            throw ctxt.mappingException(_valueClass);
        }
    }

    public final static class ClassDeserializer
        extends StdDeserializer<Class<?>>
    {
        public ClassDeserializer() { super(Class.class); }

        @Override
            public Class<?> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken curr = jp.getCurrentToken();
            // Currently will only accept if given simple class name
            if (curr == JsonToken.VALUE_STRING) {
                try {
                    return Class.forName(jp.getText());
                } catch (ClassNotFoundException e) {
                    throw ctxt.instantiationException(_valueClass, e);
                }
            }
            throw ctxt.mappingException(_valueClass);
        }
    }

    /*
    /////////////////////////////////////////////////////////////
    // Then primitive/wrapper types
    /////////////////////////////////////////////////////////////
    */

    public final static class BooleanDeserializer
        extends PrimitiveOrWrapperDeserializer<Boolean>
    {
        public BooleanDeserializer(Class<Boolean> cls, Boolean nvl)
        {
            super(cls, nvl);
        }
        
        @Override
	public Boolean deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // We accept couple of different types; obvious ones first:
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_TRUE) {
                return Boolean.TRUE;
            }
            if (t == JsonToken.VALUE_FALSE) {
                return Boolean.FALSE;
            }
            
            // [JACKSON-78]: should accept ints too, (0 == false, otherwise true)
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return (jp.getIntValue() == 0) ? Boolean.FALSE : Boolean.TRUE; 
            }

            // And finally, let's allow Strings to be converted too
            if (t == JsonToken.VALUE_STRING) {
                String text = jp.getText();
                if ("true".equals(text)) {
                    return Boolean.TRUE;
                }
                if ("false".equals(text)) {
                    return Boolean.FALSE;
                }
                throw ctxt.weirdStringException(_valueClass, "only \"true\" or \"false\" recognized");
            }
            
            // Otherwise, no can do:
            throw ctxt.mappingException(_valueClass);
        }
    }

    public final static class ByteDeserializer
        extends PrimitiveOrWrapperDeserializer<Byte>
    {
        public ByteDeserializer(Class<Byte> cls, Byte nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Byte deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            int value = _parseInt(jp, ctxt);
            // So far so good: but does it fit?
            if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                throw ctxt.weirdStringException(_valueClass, "overflow, value can not be represented as 8-bit value");
            }
            return Byte.valueOf((byte) value);
        }
    }

    public final static class ShortDeserializer
        extends PrimitiveOrWrapperDeserializer<Short>
    {
        public ShortDeserializer(Class<Short> cls, Short nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Short deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            int value = _parseInt(jp, ctxt);
            // So far so good: but does it fit?
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                throw ctxt.weirdStringException(_valueClass, "overflow, value can not be represented as 16-bit value");
            }
            return Short.valueOf((short) value);
        }
    }

    public final static class CharacterDeserializer
        extends PrimitiveOrWrapperDeserializer<Character>
    {
        public CharacterDeserializer(Class<Character> cls, Character nvl)
        {
            super(cls, nvl);
        }

        @Override
		public Character deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            int value;

            if (t == JsonToken.VALUE_NUMBER_INT) { // ok iff ascii value
                value = jp.getIntValue();
                if (value >= 0 && value <= 0xFFFF) {
                    return Character.valueOf((char) value);
                }
            } else if (t == JsonToken.VALUE_STRING) { // this is the usual type
                // But does it have to be exactly one char?
                String text = jp.getText();
                if (text.length() == 1) {
                    return Character.valueOf(text.charAt(0));
                }
            }
            throw ctxt.mappingException(_valueClass);
        }
    }

    public final static class IntegerDeserializer
        extends PrimitiveOrWrapperDeserializer<Integer>
    {
        public IntegerDeserializer(Class<Integer> cls, Integer nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Integer deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return _parseInt(jp, ctxt);
        }
    }

    public final static class LongDeserializer
        extends PrimitiveOrWrapperDeserializer<Long>
    {
        public LongDeserializer(Class<Long> cls, Long nvl)
        {
            super(cls, nvl);
        }

        @Override
        public Long deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // We accept couple of different types; obvious ones first:
            JsonToken t = jp.getCurrentToken();

            if (t == JsonToken.VALUE_NUMBER_INT) {
                return jp.getLongValue();
            }
            // it should be ok to coerce (although may fail, too)
            if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                return jp.getLongValue();
            }
            // And finally, let's allow Strings to be converted too
            if (t == JsonToken.VALUE_STRING) {
                // !!! 05-Jan-2009, tatu: Should we try to limit value space, JDK is too lenient?
                String text = jp.getText().trim();
                try {
                    return Long.parseLong(text);
                } catch (IllegalArgumentException iae) { }
                throw ctxt.weirdStringException(_valueClass, "not a valid long value");
            }
            // Otherwise, no can do:
            throw ctxt.mappingException(_valueClass);
        }
    }

    public final static class FloatDeserializer
        extends PrimitiveOrWrapperDeserializer<Float>
    {
        public FloatDeserializer(Class<Float> cls, Float nvl)
        {
            super(cls, nvl);
        }

        @Override
		public Float deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            /* 22-Jan-2009, tatu: Bounds/range checks would be tricky
             *   here, so let's not bother even trying...
             */
            return Float.valueOf((float) _parseDouble(jp, ctxt));
        }
    }

    public final static class DoubleDeserializer
        extends PrimitiveOrWrapperDeserializer<Double>
    {
        public DoubleDeserializer(Class<Double> cls, Double nvl)
        {
            super(cls, nvl);
        }

        @Override
		public Double deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return _parseDouble(jp, ctxt);
        }
    }

    /**
     * For type <code>Number.class</code>, we can just rely on type
     * mappings that plain {@link JsonParser#getNumberValue} returns.
     */
    public final static class NumberDeserializer
        extends StdDeserializer<Number>
    {
        public NumberDeserializer() { super(Number.class); }

        @Override
        public Number deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return jp.getNumberValue();
            } else if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                /* [JACKSON-72]: need to allow overriding the behavior regarding
                 *   which type to use
                 */
                if (ctxt.isEnabled(DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    return jp.getDecimalValue();
                }
                return Double.valueOf(jp.getDoubleValue());
            }

            /* Textual values are more difficult... not parsing itself, but figuring
             * out 'minimal' type to use 
             */
            if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
                String text = jp.getText().trim();
                try {
                    if (text.indexOf('.') >= 0) { // floating point
                        return new Double(text);
                    }
                    long value = Long.parseLong(text);
                    if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                        return Integer.valueOf((int) value);
                    }
                    return Long.valueOf(value);
                } catch (IllegalArgumentException iae) {
                    throw ctxt.weirdStringException(_valueClass, "not a valid number");
                }
            }
            // Otherwise, no can do:
            throw ctxt.mappingException(_valueClass);
        }
    }

    /*
    /////////////////////////////////////////////////////////////
    // And then bit more complicated (but non-structured) number
    // types
    /////////////////////////////////////////////////////////////
    */

    public final static class BigDecimalDeserializer
        extends StdDeserializer<BigDecimal>
    {
        public BigDecimalDeserializer() { super(BigDecimal.class); }

        @Override
		public BigDecimal deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_INT
                || t == JsonToken.VALUE_NUMBER_FLOAT) {
                return jp.getDecimalValue();
            }
            // String is ok too, can easily convert
            if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
                String text = jp.getText().trim();
                try {
                    return new BigDecimal(text);
                } catch (IllegalArgumentException iae) {
                    throw ctxt.weirdStringException(_valueClass, "not a valid representation");
                }
            }
            // Otherwise, no can do:
            throw ctxt.mappingException(_valueClass);
        }
    }

    /**
     * This is bit trickier to implement efficiently, while avoiding
     * overflow problems.
     */
    public final static class BigIntegerDeserializer
        extends StdDeserializer<BigInteger>
    {
        public BigIntegerDeserializer() { super(BigInteger.class); }

        @Override
		public BigInteger deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            String text;

            if (t == JsonToken.VALUE_NUMBER_INT) {
                switch (jp.getNumberType()) {
                case INT:
                case LONG:
                    return BigInteger.valueOf(jp.getLongValue());
                }
            } else if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                /* Whether to fail if there's non-integer part?
                 * Could do by calling BigDecimal.toBigIntegerExact()
                 */
                return jp.getDecimalValue().toBigInteger();
            } else if (t != JsonToken.VALUE_STRING) { // let's do implicit re-parse
                // String is ok too, can easily convert; otherwise, no can do:
                throw ctxt.mappingException(_valueClass);
            }
            text = jp.getText().trim();
            try {
                return new BigInteger(text);
            } catch (IllegalArgumentException iae) {
                throw ctxt.weirdStringException(_valueClass, "not a valid representation");
            }
        }
    }

    /*
    /////////////////////////////////////////////////////////////
    // Then trickier things: Date/Calendar types
    /////////////////////////////////////////////////////////////
    */

    public final static class CalendarDeserializer
        extends StdDeserializer<Calendar>
    {
        public CalendarDeserializer() { super(Calendar.class); }

        @Override
		public Calendar deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return ctxt.constructCalendar(_parseDate(jp, ctxt));
        }
    }

    public final static class UtilDateDeserializer
        extends StdDeserializer<java.util.Date>
    {
        public UtilDateDeserializer() { super(java.util.Date.class); }

        @Override
		public java.util.Date deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return _parseDate(jp, ctxt);
        }
    }

    /**
     * Compared to plain old {@link java.util.Date}, SQL version is easier
     * to deal with: mostly because it is more limited.
     */
    public final static class SqlDateDeserializer
        extends StdDeserializer<java.sql.Date>
    {
        public SqlDateDeserializer() { super(java.sql.Date.class); }

        @Override
		public java.sql.Date deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();

            try {
                if (t == JsonToken.VALUE_NUMBER_INT) {
                    return new java.sql.Date(jp.getLongValue());
                }
                if (t == JsonToken.VALUE_STRING) {
                    return java.sql.Date.valueOf(jp.getText().trim());
                }
                throw ctxt.mappingException(_valueClass);
            } catch (IllegalArgumentException iae) {
                throw ctxt.weirdStringException(_valueClass, "not a valid representation (error: "+iae.getMessage()+")");
            }
        }
    }

}
