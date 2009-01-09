package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.*;

/**
 * Base class for simple key deserializers.
 */
public abstract class StdKeyDeserializer
    extends KeyDeserializer
{
    final static double MIN_FLOAT = (double) Float.MIN_VALUE;
    final static double MAX_FLOAT = (double) Float.MAX_VALUE;

    final Class<?> _keyClass;

    protected StdKeyDeserializer(Class<?> cls) { _keyClass = cls; }

    public final Object deserializeKey(String key, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (key == null) { // is this even legal call?
            return null;
        }
        try {
            Object result = _parse(key, ctxt);
            if (result != null) {
                return result;
            }
        } catch (IllegalArgumentException iae) {
            ;
        }
        throw ctxt.weirdKeyException(_keyClass, key, "not a valid representation");
    }

    public Class<?> getKeyClass() { return _keyClass; }

    protected abstract Object _parse(String key, DeserializationContext ctxt) throws JsonMappingException;

    /*
    ////////////////////////////////////////////////////////////////////////
    // Helper methods for sub-classes
    ////////////////////////////////////////////////////////////////////////
     */

    protected int _parseInt(String key) throws IllegalArgumentException
    {
        return Integer.parseInt(key);
    }

    protected long _parseLong(String key) throws IllegalArgumentException
    {
        return Long.parseLong(key);
    }

    protected double _parseDouble(String key) throws IllegalArgumentException
    {
        return Double.parseDouble(key);
    }

    /*
    ////////////////////////////////////////////////////////////////////////
    // Key deserializer implementations; wrappers
    ////////////////////////////////////////////////////////////////////////
     */

    final static class BoolKD extends StdKeyDeserializer
    {
        BoolKD() { super(Boolean.class); }

        public Boolean _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            if ("true".equals(key)) {
                return Boolean.TRUE;
            }
            if ("false".equals(key)) {
                return Boolean.FALSE;
            }
            throw ctxt.weirdKeyException(_keyClass, key, "value not 'true' or 'false'");
        }
    }

    final static class ByteKD extends StdKeyDeserializer
    {
        ByteKD() { super(Byte.class); }

        public Byte _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            int value = _parseInt(key);
            if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                throw ctxt.weirdKeyException(_keyClass, key, "overflow, value can not be represented as 8-bit value");
            }
            return Byte.valueOf((byte) value);
        }
    }

    final static class ShortKD extends StdKeyDeserializer
    {
        ShortKD() { super(Integer.class); }

        public Short _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            int value = _parseInt(key);
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                throw ctxt.weirdKeyException(_keyClass, key, "overflow, value can not be represented as 16-bit value");
            }
            return Short.valueOf((short) value);
        }
    }

    /**
     * Dealing with Characters is bit trickier: let's assume it must be a String, and that
     * Unicode numeric value is never used.
     */
    final static class CharKD extends StdKeyDeserializer
    {
        CharKD() { super(Character.class); }

        public Character _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            if (key.length() == 1) {
                return Character.valueOf(key.charAt(0));
            }
            throw ctxt.weirdKeyException(_keyClass, key, "can only convert 1-character Strings");
        }
    }

    final static class IntKD extends StdKeyDeserializer
    {
        IntKD() { super(Integer.class); }

        public Integer _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            return _parseInt(key);
        }
    }

    final static class LongKD extends StdKeyDeserializer
    {
        LongKD() { super(Long.class); }

        public Long _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            return _parseLong(key);
        }
    }

    final static class DoubleKD extends StdKeyDeserializer
    {
        DoubleKD() { super(Double.class); }

        public Double _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            return _parseDouble(key);
        }
    }

    final static class FloatKD extends StdKeyDeserializer
    {
        FloatKD() { super(Float.class); }

        public Float _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            double d = _parseDouble(key);
            if (d < MIN_FLOAT || d > MAX_FLOAT) {
                throw ctxt.weirdKeyException(_keyClass, key, "overflow/underflow, value can not be represented as a 32-bit float");
            }
            return Float.valueOf((float) d);
        }
    }

    /*
    ////////////////////////////////////////////////////////////////////////
    // Key deserializer implementations; other
    ////////////////////////////////////////////////////////////////////////
     */

    final static class EnumKD extends StdKeyDeserializer
    {
        final EnumResolver _resolver;

        EnumKD(EnumResolver er)
        {
            super(er.getEnumClass());
            _resolver = er;
        }

        public Enum<?> _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            Enum<?> e = _resolver.findEnum(key);
            if (e == null) {
                throw ctxt.weirdKeyException(_keyClass, key, "not one of values for Enum class");
            }
            return e;
        }
    }
}

