package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.util.LinkedNode;
import org.codehaus.jackson.type.JavaType;

/**
 * Deserializer class that can deserialize instances of
 * arbitrary bean objects, usually from Json Object structs,
 * but possibly also from simple types like String values.
 */
public class BeanDeserializer
    extends JsonDeserializer<Object>
    implements ResolvableDeserializer
{
    /*
    ///////////////////////////////////////////////
    // Information regarding type being deserialized
    ///////////////////////////////////////////////
     */

    final protected JavaType _beanType;

    /*
    ///////////////////////////////////////////////
    // Construction configuration
    ///////////////////////////////////////////////
     */

    /**
     * Default constructor used to instantiate the bean when mapping
     * from Json object.
     */
    protected Constructor<?> _defaultConstructor;

    /**
     * If the "bean" class can be instantiated using just a single
     * String (via constructor, static method etc), this object
     * knows how to invoke method/constructor in question.
     * If so, no setters will be used.
     */
    protected StringConstructor _stringConstructor;

    /**
     * If the "bean" class can be instantiated using just a single
     * numeric (int, long) value  (via constructor, static method etc),
     * this object
     * knows how to invoke method/constructor in question.
     * If so, no setters will be used.
     */
    protected NumberConstructor _numberConstructor;

    /*
    ///////////////////////////////////////////////
    // Property information, setters
    ///////////////////////////////////////////////
     */

    /**
     * Things set via setters (modifiers) are included in this
     * Map.
     */
    final protected HashMap<String, SettableBeanProperty> _props;

    /**
     * Fallback setter used for handling any properties that are not
     * mapped to regular setters. If setter is not null, it will be
     * called once for each such property.
     */
    protected SettableAnyProperty _anySetter;

    /**
     * In addition to properties that are set, we will also keep
     * track of recognized but ignorable properties: these will
     * be skipped without errors or warnings.
     */
    protected HashSet<String> _ignorableProps;

    /*
    /////////////////////////////////////////////////////////
    // Life-cycle, construction, initialization
    /////////////////////////////////////////////////////////
     */

    public BeanDeserializer(JavaType type) 
    {
        _beanType = type;
        _props = new HashMap<String, SettableBeanProperty>();
        _ignorableProps = null;
    }

    public void setDefaultConstructor(Constructor<?> ctor) {
        _defaultConstructor = ctor;
    }

    public void setConstructor(StringConstructor ctor) {
        _stringConstructor = ctor;
    }

    public void setConstructor(NumberConstructor ctor) {
        _numberConstructor = ctor;
    }
    
    /**
     * Method to add a property setter. Will ensure that there is no
     * unexpected override; if one is found will throw a
     * {@link IllegalArgumentException}.
     */
    public void addProperty(SettableBeanProperty prop)
    {
        SettableBeanProperty old =  _props.put(prop.getPropertyName(), prop);
        if (old != null && old != prop) { // should never occur...
            throw new IllegalArgumentException("Duplicate property '"+prop.getPropertyName()+"' for "+_beanType);
        }
    }

    public SettableBeanProperty removeProperty(String name)
    {
        return _props.remove(name);
    }

    public void setAnySetter(SettableAnyProperty s)
    {
        if (_anySetter != null && s != null) {
            throw new IllegalStateException("_anySetter already set to non-null");
        }
        _anySetter = s;
    }

    /**
     * Method that will add property name as one of properties that can
     * be ignored if not recognized.
     */
    public void addIgnorable(String propName)
    {
        if (_ignorableProps == null) {
            _ignorableProps = new HashSet<String>();
        }
        _ignorableProps.add(propName);
    }

    /*
    /////////////////////////////////////////////////////////
    // Validation, post-processing
    /////////////////////////////////////////////////////////
     */

    /**
     * Method called to ensure that there is at least one constructor
     * that could be used to construct an instance.
     */
    public void validateConstructors()
    {
        // sanity check: must have a constructor of one type or another
        if ((_defaultConstructor == null) && (_numberConstructor == null)
            && (_stringConstructor == null)) {
            throw new IllegalArgumentException("Can not create Bean deserializer for ("+_beanType+"): neither default constructor nor factory methods found");
        }
    }

    /**
     * Method called to finalize setup of this deserializer,
     * after deserializer itself has been registered. This
     * is needed to handle recursive dependencies.
     */
    public void resolve(DeserializationConfig config, DeserializerProvider provider)
        throws JsonMappingException
    {
        // let's reuse same instances, not all are cached by provider
        /* 04-Feb-2009, tatu: This is tricky now that we are to pass referrer
         *   information, as there is no easy+reliable+efficient way to do
         *   it. But we can use a quick heuristic: only cache "expensive"
         *   BeanDeserializers; for them it is unlikely that different
         *   references should lead to different deserializers, and for other
         *   types cost is much lower so we can drop caching
         */
        HashMap<JavaType, JsonDeserializer<Object>> seen = null;

        for (SettableBeanProperty prop : _props.values()) {
            // May already have deserializer from annotations, if so, skip:
            if (prop.hasValueDeserializer()) {
                continue;
            }

            JavaType type = prop.getType();
            JsonDeserializer<Object> deser = null;

            if (seen != null) {
                deser = seen.get(type);
            }
            if (deser == null) {
                deser = provider.findValueDeserializer(config, type, _beanType, prop.getPropertyName());
                if (deser instanceof BeanDeserializer) {
                    if (seen == null) {
                        seen = new HashMap<JavaType, JsonDeserializer<Object>>();
                    }
                    seen.put(type, deser);
                }
            }
            prop.setValueDeserializer(deser);
        }

        // Finally, "any setter" may also need to be resolved now
        if (_anySetter != null && !_anySetter.hasValueDeserializer()) {
            JavaType type = _anySetter.getType();
            JsonDeserializer<Object> deser = null;
            if (seen != null) {
                deser = seen.get(type);
            }
            if (deser == null) {
                deser = provider.findValueDeserializer(config, type, _beanType, null);
            }
            _anySetter.setValueDeserializer(deser);
        }
    }

    /*
    /////////////////////////////////////////////////////////
    // JsonDeserializer implementation
    /////////////////////////////////////////////////////////
     */

    /**
     * Because of costs associated with constructing bean deserializers,
     * they usually should be cached unlike other deserializer types.
     * Additionally it is important to be able to cache bean serializers
     * to handle cyclic references.
     */
    @Override
    public boolean shouldBeCached() { return true; }

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
        throw ctxt.mappingException(getBeanClass());
    }

    /*
    /////////////////////////////////////////////////////////
    // Other public accessors
    /////////////////////////////////////////////////////////
     */

    public final Class<?> getBeanClass() { return _beanType.getRawClass(); }

    /*
    /////////////////////////////////////////////////////////
    // Concrete deserialization methods
    /////////////////////////////////////////////////////////
     */

    public Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // !!! TODO: alternative constructors (with annotated params)

        // But for now, must have the default constructor:
        if (_defaultConstructor == null) {
            throw JsonMappingException.from(jp, "No default constructor found for type "+_beanType+": can not instantiate from Json object");
        }

        Object bean;
        try {
            bean = _defaultConstructor.newInstance();
        } catch (Exception e) {
            _rethrow(e);
            return null; // never gets here
        }

        while (jp.nextToken() != JsonToken.END_OBJECT) { // otherwise field name
            String propName = jp.getCurrentName();
            SettableBeanProperty prop = _props.get(propName);

            if (prop != null) { // normal case
                prop.deserializeAndSet(jp, ctxt, bean);
                continue;
            }
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                continue;
            }
            // need to process or skip the following token
            JsonToken t = jp.nextToken();
            // Unknown: let's call handler method
            handleUnknownProperty(ctxt, bean, propName);
        }
        return bean;
    }

    /*
    /////////////////////////////////////////////////////////
    // Overridable helper methods
    /////////////////////////////////////////////////////////
     */

    /**
     * Method called to deal with a property that did not map to a known
     * Bean property. Method can deal with the problem as it sees fit (ignore,
     * throw exception); but if it does return, it has to skip the matching
     * Json content parser has.
     *
     * @param ctxt Context for deserialization; allows access to the parser,
     *    error reporting functionality
     * @param resultBean Bean that is being populated by this deserializer
     * @param propName Name of the property that can not be mapped
     */
    protected void handleUnknownProperty(DeserializationContext ctxt, Object resultBean, String propName)
        throws IOException, JsonProcessingException
    {
        // otherwise, what to do with it? Ignore?
        if (_ignorableProps != null && _ignorableProps.contains(propName)) {
            ; // fine, ignore as is
        } else {
            LinkedNode<DeserializationProblemHandler> h = ctxt.getConfig().getProblemHandlers();
            while (h != null) {
                // Can bail out if it's handled
                if (h.value().handleUnknownProperty(ctxt, this, resultBean, propName)) {
                    return;
                }
            }
            // Nope, not handled. Still a problem:
            reportUnknownField(ctxt, resultBean, propName);
        }
        /* either way, need to skip now; we point to first token of value
         * (START_xxx for structured, or the value token for others)
         */
        ctxt.getParser().skipChildren();
    }
        
    protected void reportUnknownField(DeserializationContext ctxt,
                                      Object resultBean, String fieldName)
        throws IOException, JsonProcessingException
    {
        throw ctxt.unknownFieldException(resultBean, fieldName);
    }

    /*
    /////////////////////////////////////////////////////////
    // Other helper methods
    /////////////////////////////////////////////////////////
     */

    protected static void _rethrow(Exception e)
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
    }

    /**
     * Helper class that can handle simple deserialization from
     * Json String values.
     */
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
