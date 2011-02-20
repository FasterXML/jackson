package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.io.NumberInput;
import org.codehaus.jackson.map.*;

/**
 * Base class for simple key deserializers.
 */
public abstract class StdKeyDeserializer
    extends KeyDeserializer
{
    final protected Class<?> _keyClass;

    protected StdKeyDeserializer(Class<?> cls) { _keyClass = cls; }

    @Override
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
        } catch (Exception re) {
            throw ctxt.weirdKeyException(_keyClass, key, "not a valid representation: "+re.getMessage());
        }
        throw ctxt.weirdKeyException(_keyClass, key, "not a valid representation");
    }

    public Class<?> getKeyClass() { return _keyClass; }

    protected abstract Object _parse(String key, DeserializationContext ctxt) throws Exception;

    /*
    /**********************************************************
    /* Helper methods for sub-classes
    /**********************************************************
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
        return NumberInput.parseDouble(key);
    }

    /*
    /**********************************************************
    /* Key deserializer implementations; wrappers
    /**********************************************************
     */

    final static class BoolKD extends StdKeyDeserializer
    {
        BoolKD() { super(Boolean.class); }

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
		public Integer _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            return _parseInt(key);
        }
    }

    final static class LongKD extends StdKeyDeserializer
    {
        LongKD() { super(Long.class); }

        @Override
        public Long _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            return _parseLong(key);
        }
    }

    final static class DoubleKD extends StdKeyDeserializer
    {
        DoubleKD() { super(Double.class); }

        @Override
        public Double _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            return _parseDouble(key);
        }
    }

    final static class FloatKD extends StdKeyDeserializer
    {
        FloatKD() { super(Float.class); }

        @Override
        public Float _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            /* 22-Jan-2009, tatu: Bounds/range checks would be tricky
             *   here, so let's not bother even trying...
             */
            return Float.valueOf((float) _parseDouble(key));
        }
    }

    /*
    /**********************************************************
    /* Key deserializer implementations; other
    /**********************************************************
     */

    final static class EnumKD extends StdKeyDeserializer
    {
        final EnumResolver<?> _resolver;

        EnumKD(EnumResolver<?> er)
        {
            super(er.getEnumClass());
            _resolver = er;
        }

        @Override
        public Enum<?> _parse(String key, DeserializationContext ctxt) throws JsonMappingException
        {
            Enum<?> e = _resolver.findEnum(key);
            if (e == null) {
                throw ctxt.weirdKeyException(_keyClass, key, "not one of values for Enum class");
            }
            return e;
        }
    }

    /**
     * Key deserializer that calls a single-string-arg constructor
     * to instantiate desired key type.
     */
    final static class StringCtorKeyDeserializer extends StdKeyDeserializer
    {
        final Constructor<?> _ctor;

        public StringCtorKeyDeserializer(Constructor<?> ctor) {
            super(ctor.getDeclaringClass());
            _ctor = ctor;
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt) throws Exception
        {
            return _ctor.newInstance(key);
        }
    }

    /**
     * Key deserializer that calls a static no-args factory method
     * to instantiate desired key type.
     */
    final static class StringFactoryKeyDeserializer extends StdKeyDeserializer
    {
        final Method _factoryMethod;

        public StringFactoryKeyDeserializer(Method fm) {
            super(fm.getDeclaringClass());
            _factoryMethod = fm;
        }

        @Override
        public Object _parse(String key, DeserializationContext ctxt) throws Exception
        {
            return _factoryMethod.invoke(null, key);
        }
    }
}

