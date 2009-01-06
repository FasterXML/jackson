package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonDeserializationContext;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * Base class for simple standard deserializers
 */
public abstract class StdDeserializer<T>
    extends JsonDeserializer<T>
{
    /**
     * Let's limit length of error messages, for cases where underlying data
     * may be very large -- no point in spamming logs with megs of meaningless
     * data.
     */
    final static int MAX_ERROR_STR_LEN = 500;

    protected StdDeserializer() { }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for sub-classes to use; exception handling
    ////////////////////////////////////////////////////////////
    */

    protected JsonMappingException mappingException(JsonParser jp, Class<?> targetClass)
    {
        return JsonMappingException.from(jp, "Can not deserialize "+targetClass.getName()+" out of "+jp.getCurrentToken()+" token");
    }

    protected JsonMappingException instantiationException(JsonParser jp, Class<?> instClass, Exception e)
    {
        return JsonMappingException.from(jp, "Can not construct instance of "+instClass.getName()+", problem: "+e.getMessage());
    }

    /**
     * Method that will construct an exception suitable for throwing when
     * some String values are acceptable, but the one encountered is not
     */
    protected JsonMappingException weirdStringException(JsonParser jp, Class<?> instClass, String msg)
    {
        String text;

        try {
            text = jp.getText();
        } catch (Exception e) {
            text = "[N/A]";
        }

        // !!! should we quote it?
        if (text.length() > MAX_ERROR_STR_LEN) {
            text = text.substring(0, MAX_ERROR_STR_LEN) + "]...[" + text.substring(text.length() - MAX_ERROR_STR_LEN);
        }
        return JsonMappingException.from(jp, "Can not construct instance of "+instClass.getName()+" from String value '"+text+"': "+msg);
    }

    /*
    /////////////////////////////////////////////////////////////
    // First, generic (Object, String, String-like) deserializers
    /////////////////////////////////////////////////////////////
    */

    public final static class StringDeserializer
        extends StdDeserializer<String>
    {
        public StringDeserializer() { }

        public String deserialize(JsonParser jp, JsonDeserializationContext ctxt)
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
            throw mappingException(jp, String.class);
        }
    }
    
    /*
    /////////////////////////////////////////////////////////////
    // Then primitive/wrapper types
    /////////////////////////////////////////////////////////////
    */

    public final static class BooleanDeserializer
        extends StdDeserializer<Boolean>
    {
        public BooleanDeserializer() { }
        
        public Boolean deserialize(JsonParser jp, JsonDeserializationContext ctxt)
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
            // Hmmh. Is it to get or expect null...?
            if (t == JsonToken.VALUE_NULL) {
                return null;
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
                throw weirdStringException(jp, Boolean.class, "only \"true\" or \"false\" recognized");
            }
            
            // Otherwise, no can do:
            throw mappingException(jp, Boolean.class);
        }
    }

    public final static class ByteDeserializer
        extends StdDeserializer<Byte>
    {
        public ByteDeserializer() { }

        public Byte deserialize(JsonParser jp, JsonDeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            int value;

            if (t == JsonToken.VALUE_NUMBER_INT) {
                value = jp.getIntValue();
            } else if (t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
                value = jp.getIntValue();
            } else if (t == JsonToken.VALUE_NULL) { // ok to get null?
                return null;
            } else if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
                // !!! 05-Jan-2009, tatu: Should we try to limit value space, JDK is too lenient?
                String text = jp.getText().trim();
                try {
                    value = Integer.parseInt(text);
                } catch (IllegalArgumentException iae) {
                    throw weirdStringException(jp, Byte.class, "not a valid Byte value");
                }
            } else { // Otherwise, no can do:
                throw mappingException(jp, Byte.class);
            }
            // So far so good: but does it fit?
            if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                throw weirdStringException(jp, Byte.class, "overflow, value can not be represent as 8-bit value");
            }
            return Byte.valueOf((byte) value);
        }
    }

    public final static class ShortDeserializer
        extends StdDeserializer<Short>
    {
        public ShortDeserializer() { }

        public Short deserialize(JsonParser jp, JsonDeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            int value;

            if (t == JsonToken.VALUE_NUMBER_INT) {
                value = jp.getIntValue();
            } else if (t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
                value = jp.getIntValue();
            } else if (t == JsonToken.VALUE_NULL) { // ok to get null?
                return null;
            } else if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
                // !!! 05-Jan-2009, tatu: Should we try to limit value space, JDK is too lenient?
                String text = jp.getText().trim();
                try {
                    value = Integer.parseInt(text);
                } catch (IllegalArgumentException iae) {
                    throw weirdStringException(jp, Short.class, "not a valid Short value");
                }
            } else { // Otherwise, no can do:
                throw mappingException(jp, Short.class);
            }
            // So far so good: but does it fit?
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                throw weirdStringException(jp, Short.class, "overflow, value can not be represent as 8-bit value");
            }
            return Short.valueOf((short) value);
        }
    }

    public final static class CharacterDeserializer
        extends StdDeserializer<Character>
    {
        public CharacterDeserializer() { }

        public Character deserialize(JsonParser jp, JsonDeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            int value;

            if (t == JsonToken.VALUE_NUMBER_INT) { // ok iff ascii value
                value = jp.getIntValue();
                if (value >= 0 && value <= 0xFFFF) {
                    return Character.valueOf((char) value);
                }
            } else if (t == JsonToken.VALUE_NULL) { // ok to get null?
                return null;
            } else if (t == JsonToken.VALUE_STRING) { // this is the usual type
                // But does it have to be exactly one char?
                String text = jp.getText();
                if (text.length() == 1) {
                    return Character.valueOf(text.charAt(0));
                }
            }
            throw mappingException(jp, Character.class);
        }
    }

    public final static class IntegerDeserializer
        extends StdDeserializer<Integer>
    {
        public IntegerDeserializer() { }

        public Integer deserialize(JsonParser jp, JsonDeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // We accept couple of different types; obvious ones first:
            JsonToken t = jp.getCurrentToken();

            if (t == JsonToken.VALUE_NUMBER_INT) {
                return jp.getIntValue();
            }
            // it should be ok to coerce (although may fail, too)
            if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                return jp.getIntValue();
            }
            // Hmmh. Is it to get or expect null...?
            if (t == JsonToken.VALUE_NULL) {
                return null;
            }
            // And finally, let's allow Strings to be converted too
            if (t == JsonToken.VALUE_STRING) {
                // !!! 05-Jan-2009, tatu: Should we try to limit value space, JDK is too lenient?
                String text = jp.getText().trim();
                try {
                    return Integer.parseInt(text);
                } catch (IllegalArgumentException iae) { }
                throw weirdStringException(jp, Integer.class, "not a valid integer value");
            }
            // Otherwise, no can do:
            throw mappingException(jp, Integer.class);
        }
    }

    public final static class LongDeserializer
        extends StdDeserializer<Long>
    {
        public LongDeserializer() { }

        public Long deserialize(JsonParser jp, JsonDeserializationContext ctxt)
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
            // Hmmh. Is it to get or expect null...?
            if (t == JsonToken.VALUE_NULL) {
                return null;
            }
            // And finally, let's allow Strings to be converted too
            if (t == JsonToken.VALUE_STRING) {
                // !!! 05-Jan-2009, tatu: Should we try to limit value space, JDK is too lenient?
                String text = jp.getText().trim();
                try {
                    return Long.parseLong(text);
                } catch (IllegalArgumentException iae) { }
                throw weirdStringException(jp, Long.class, "not a valid long value");
            }
            // Otherwise, no can do:
            throw mappingException(jp, Long.class);
        }
    }

    public final static class FloatDeserializer
        extends StdDeserializer<Float>
    {
        public FloatDeserializer() { }

        public Float deserialize(JsonParser jp, JsonDeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // We accept couple of different types; obvious ones first:
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                // should we worry about over/underflow?
                return (float) jp.getDoubleValue();
            }
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return (float) jp.getDoubleValue();
            }
            if (t == JsonToken.VALUE_NULL) { // null ok?
                return null;
            }
            // And finally, let's allow Strings to be converted too
            if (t == JsonToken.VALUE_STRING) {
                String text = jp.getText().trim();
                try {
                    return Float.parseFloat(text);
                } catch (IllegalArgumentException iae) { }
                throw weirdStringException(jp, Float.class, "not a valid float value");
            }
            // Otherwise, no can do:
            throw mappingException(jp, Float.class);
        }
    }

    public final static class DoubleDeserializer
        extends StdDeserializer<Double>
    {
        public DoubleDeserializer() { }

        public Double deserialize(JsonParser jp, JsonDeserializationContext ctxt)
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
            // Hmmh. Is it to get or expect null...?
            if (t == JsonToken.VALUE_NULL) {
                return null;
            }
            // And finally, let's allow Strings to be converted too
            if (t == JsonToken.VALUE_STRING) {
                // !!! 05-Jan-2009, tatu: Should we try to limit value space, JDK is too lenient?
                String text = jp.getText().trim();
                try {
                    return Double.parseDouble(text);
                } catch (IllegalArgumentException iae) { }
                throw weirdStringException(jp, Double.class, "not a valid double value");
            }
            // Otherwise, no can do:
            throw mappingException(jp, Double.class);
        }
    }

}
