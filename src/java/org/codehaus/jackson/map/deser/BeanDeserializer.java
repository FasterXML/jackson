package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Deserializer class that can deserialize instances of
 * arbitrary bean objects, usually from Json Object structs,
 * but possibly also from simple types like String values.
 */
public final class BeanDeserializer
    extends JsonDeserializer<Object>
    implements ResolvableDeserializer
{
    final Class<?> _beanClass;

    /**
     * Things set via setters (modifiers) are included in this
     * Map.
     */
    final HashMap<String, SettableBeanProperty> _props;

    /**
     * In addition to properties that are set, we will also keep
     * track of recognized but ignorable properties: these will
     * be skipped without errors or warnings.
     */
    final HashSet<String> _ignorableProps;

    /**
     * If the "bean" class can be instantiated using just a single
     * String (via constructor, static method etc), this object
     * knows how to invoke method/constructor in question.
     */
    final StringConstructor _stringConstructor;

    /**
     * If the "bean" class can be instantiated using just a single
     * numeric (int, long) value  (via constructor, static method etc),
     * this object
     * knows how to invoke method/constructor in question.
     */
    final NumberConstructor _numberConstructor;

    /*
    /////////////////////////////////////////////////////////
    // Life-cycle
    /////////////////////////////////////////////////////////
     */

    public BeanDeserializer(Class<?> type, 
                            HashMap<String, SettableBeanProperty> props,
                            HashSet<String> ignorableProps,
                            StringConstructor sctor,
                            NumberConstructor nctor)
    {
        _beanClass = type;
        _props = props;
        _ignorableProps = ignorableProps;
        _stringConstructor = sctor;
        _numberConstructor = nctor;
    }

    /**
     * Method called to finalize setup of this deserializer,
     * after deserializer itself has been registered. This
     * is needed to handle recursive dependencies.
     */
    public void resolve(DeserializerProvider provider)
    {
        // !!! TBI
    }

    /*
    /////////////////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////////////////
     */

    public Class<?> getBeanClass() { return _beanClass; }

    public final Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            return deserializeFromObject(jp, ctxt);
        }
        if (t == JsonToken.VALUE_STRING) {
            Object value = _stringConstructor.construct(jp.getText());
            if (value != null) {
                return value;
            }
        }
        if (t.isNumeric()) {
            Object value = null;
            switch (jp.getNumberType()) {
            case INT:
                value = _numberConstructor.construct(jp.getIntValue());
                break;
            case LONG:
                value = _numberConstructor.construct(jp.getLongValue());
                break;
            }
            if (value != null) {
                return value;
            }
        }
        throw ctxt.mappingException(_beanClass);
    }

    /*
    /////////////////////////////////////////////////////////
    // Concrete deserialization methods
    /////////////////////////////////////////////////////////
     */

    public final Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // !!! TBI
        return null;
    }

    /*
    /////////////////////////////////////////////////////////
    // Internal helper methods
    /////////////////////////////////////////////////////////
     */

    /*
    /////////////////////////////////////////////////////////
    // Helper classes
    /////////////////////////////////////////////////////////
     */

    static class ConstructorBase
    {
        protected final Class<?> _valueClass;

        public ConstructorBase(Class<?> valueClass)
        {
            _valueClass = valueClass;
        }

        protected void _rethrow(Exception e)
            throws RuntimeException
        {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            throw new IllegalArgumentException(t.getMessage(), t);
        }
    }

    final static class StringConstructor
        extends ConstructorBase
    {
        protected final Method _factoryMethod;
        protected final Constructor<?> _ctor;

        public StringConstructor(Class<?> valueClass, Constructor<?> ctor,
                                 Method factoryMethod)
        {
            super(valueClass);
            _ctor = ctor;
            _factoryMethod = factoryMethod;
        }

        public Object construct(String value)
        {
            try {
                if (_ctor != null) {
                    return _ctor.newInstance(value);
                }
                if (_factoryMethod != null) {
                    return _factoryMethod.invoke(_valueClass, value);
                }
            } catch (Exception e) {
                _rethrow(e);
            }
            return null;
        }
    }

    final static class NumberConstructor
        extends ConstructorBase
    {
        protected final Constructor<?> _intCtor;
        protected final Constructor<?> _longCtor;

        protected final Method _intFactoryMethod;
        protected final Method _longFactoryMethod;

        public NumberConstructor(Class<?> valueClass,
                                 Constructor<?> intCtor,
                                 Constructor<?> longCtor,
                                 Method ifm, Method lfm)
        {
            super(valueClass);
            _intCtor = intCtor;
            _longCtor = longCtor;
            _intFactoryMethod = ifm;
            _longFactoryMethod = lfm;
        }

        public Object construct(int value)
        {
            // First: "native" int methods work best:
            try {
                if (_intCtor != null) {
                    return _intCtor.newInstance(value);
                }
                if (_intFactoryMethod != null) {
                    return _intFactoryMethod.invoke(_valueClass, Integer.valueOf(value));
                }
            } catch (Exception e) {
                _rethrow(e);
            }
            // but if not, can do widening conversion
            return construct((long) value);
        }

        public Object construct(long value)
        {
            /* For longs we don't even try casting down to ints;
             * theoretically could try if value fits... but let's
             * leave that as a future improvement
             */
            try {
                if (_longCtor != null) {
                    return _longCtor.newInstance(value);
                }
                if (_longFactoryMethod != null) {
                    return _longFactoryMethod.invoke(_valueClass, Long.valueOf(value));
                }
            } catch (Exception e) {
                _rethrow(e);
            }
            return null;
        }
    }
}
