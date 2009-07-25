package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonCachable;
import org.codehaus.jackson.map.introspect.AnnotatedConstructor;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.map.util.LinkedNode;
import org.codehaus.jackson.type.JavaType;

/**
 * Deserializer class that can deserialize instances of
 * arbitrary bean objects, usually from Json Object structs,
 * but possibly also from simple types like String values.
 */
@JsonCachable
/* Because of costs associated with constructing bean deserializers,
 * they usually should be cached unlike other deserializer types.
 * But more importantly, it is important to be able to cache
 * bean serializers to handle cyclic references.
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
     * from Json object, and only using setters for initialization
     * (not specific constructors)
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

    /**
     * If the bean class can be instantiated using a creator
     * (an annotated single arg constructor or static method),
     * this object is used for handling details of how delegate-based
     * deserialization and instance construction works
     */
    protected DelegatingConstructor _delegatingConstructor;

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

    public void setStringCreators(Class<?> valueClass,
                                  AnnotatedConstructor ctor, AnnotatedMethod factoryMethod)
    {
	_stringConstructor = new StringConstructor(valueClass, ctor, factoryMethod);
    }
    
    public void setNumberCreators(Class<?> valueClass,
                                  AnnotatedConstructor intCtor,  AnnotatedConstructor longCtor,
                                  AnnotatedMethod intFactory, AnnotatedMethod longFactory)
    {
	_numberConstructor = new NumberConstructor(valueClass, intCtor, longCtor, intFactory, longFactory);
    }

    /**
     * Method called to define a "delegating" constructor to use for
     * deserializing from JSON Object structs. Delegation here means that
     * the JSON Object is first deserialized into delegated type, and
     * then resulting value is passed as the argument to delegating
     * constructor.
     *<p>
     * Note that delegating constructors have precedence over default
     * and property-based constructors.
     */
    public void setDelegatingCreators(AnnotatedConstructor ctor, AnnotatedMethod factory)
    {
	// important: ensure we do not hold on to default constructor...
	_defaultConstructor = null;
	_delegatingConstructor = new DelegatingConstructor(ctor, factory);
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
    public void validateCreators()
        throws JsonMappingException
    {
        // sanity check: must have a constructor of one type or another
        if ((_defaultConstructor == null)
            && (_numberConstructor == null)
            && (_stringConstructor == null)
            && (_delegatingConstructor == null)) {
            throw new JsonMappingException("Can not create Bean deserializer for ("+_beanType+"): neither default/delegating constructor nor factory methods found");
        }
    }

    /**
     * Method called to finalize setup of this deserializer,
     * after deserializer itself has been registered. This
     * is needed to handle recursive and transitive dependencies.
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
                deser = findDeserializer(config, provider, type, prop.getPropertyName(), seen);
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
                deser = findDeserializer(config, provider, type, "[any]", seen);
            }
            _anySetter.setValueDeserializer(deser);
        }

	// as well as delegate-based constructor:
	if (_delegatingConstructor != null) {
            JsonDeserializer<Object> deser = findDeserializer(config, provider, _delegatingConstructor.getValueType(), "[constructor-arg[0]]", seen);
	    _delegatingConstructor.setDeserializer(deser);
	}
    }

    /*
    /////////////////////////////////////////////////////////
    // JsonDeserializer implementation
    /////////////////////////////////////////////////////////
     */

    public final Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            return deserializeFromObject(jp, ctxt);
        }
        if (t == JsonToken.VALUE_STRING) {
	    return deserializeFromString(jp, ctxt);
	}
        if (t.isNumeric()) {
	    return deserializeFromNumber(jp, ctxt);
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
        if (_defaultConstructor == null) {
	    // 07-Jul-2009, tatu: let's allow delegate-based approach too
	    if (_delegatingConstructor != null) {
		return _delegatingConstructor.deserialize(jp, ctxt);
	    }
            throw JsonMappingException.from(jp, "No default constructor found for type "+_beanType+": can not instantiate from Json object");
        }

        Object bean;
        try {
            bean = _defaultConstructor.newInstance();
        } catch (Exception e) {
            ClassUtil.unwrapAndThrowAsIAE(e);
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
            /*JsonToken t =*/ jp.nextToken();
            // Unknown: let's call handler method
            handleUnknownProperty(ctxt, bean, propName);
        }
        return bean;
    }

    public Object deserializeFromString(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
	if (_stringConstructor != null) {
	    return _stringConstructor.construct(jp.getText());
        }
	if (_delegatingConstructor != null) {
	    return _delegatingConstructor.deserialize(jp, ctxt);
	}
        throw ctxt.mappingException(getBeanClass());
    }

    public Object deserializeFromNumber(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
	if (_numberConstructor != null) {
            switch (jp.getNumberType()) {
            case INT:
		return _numberConstructor.construct(jp.getIntValue());
            case LONG:
		return _numberConstructor.construct(jp.getLongValue());
            }
	}
	if (_delegatingConstructor != null) {
	    return _delegatingConstructor.deserialize(jp, ctxt);
	}
	throw ctxt.mappingException(getBeanClass());
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

    /**
     * Helper method used to locate deserializers for properties the
     * bean itself contains.
     */
    protected JsonDeserializer<Object> findDeserializer(DeserializationConfig config, DeserializerProvider provider,
                                                        JavaType type, String propertyName,
                                                        HashMap<JavaType, JsonDeserializer<Object>> seen)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser = provider.findValueDeserializer(config, type, _beanType, propertyName);
        if (deser instanceof BeanDeserializer) {
            if (seen == null) {
                seen = new HashMap<JavaType, JsonDeserializer<Object>>();
            }
            seen.put(type, deser);
        }
        return deser;
    }

    /*
    /////////////////////////////////////////////////////////
    // Helper classes
    /////////////////////////////////////////////////////////
     */

    /**
     * Helper class that can handle simple deserialization from
     * Json String values.
     */
    final static class StringConstructor
    {
        protected final Class<?> _valueClass;
        protected final Method _factoryMethod;
        protected final Constructor<?> _ctor;

        public StringConstructor(Class<?> valueClass, AnnotatedConstructor ctor,
                                 AnnotatedMethod factoryMethod)
        {
            _valueClass = valueClass;
            _ctor = (ctor == null) ? null : ctor.getAnnotated();
            _factoryMethod = (factoryMethod == null) ? null : factoryMethod.getAnnotated();
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
                ClassUtil.unwrapAndThrowAsIAE(e);
            }
            return null;
        }
    }

    final static class NumberConstructor
    {
        protected final Class<?> _valueClass;

        protected final Constructor<?> _intCtor;
        protected final Constructor<?> _longCtor;

        protected final Method _intFactoryMethod;
        protected final Method _longFactoryMethod;

        public NumberConstructor(Class<?> valueClass,
                                 AnnotatedConstructor intCtor,
                                 AnnotatedConstructor longCtor,
                                 AnnotatedMethod ifm, AnnotatedMethod lfm)
        {
            _valueClass = valueClass;
            _intCtor = (intCtor == null) ? null : intCtor.getAnnotated(); 
            _longCtor = (longCtor == null) ? null : longCtor.getAnnotated();
            _intFactoryMethod = (ifm == null) ? null : ifm.getAnnotated();
            _longFactoryMethod = (lfm == null) ? null : lfm.getAnnotated();
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
                ClassUtil.unwrapAndThrowAsIAE(e);
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
                ClassUtil.unwrapAndThrowAsIAE(e);
            }
            return null;
        }
    }

    /**
     * Helper class used to delegate parts of deserialization into
     * another serializer, and then construct instance with deserialized
     * results (either via constructor or factory method)
     */
    final static class DelegatingConstructor
    {
	/**
	 * Type to deserialize JSON to, as well as the type to pass to
	 * creator (constructor, factory method)
	 */
	protected final JavaType _valueType;

        protected final Constructor<?> _ctor;
        protected final Method _factoryMethod;

	/**
	 * Delegate deserializer to use for actual deserialization, before
	 * instantiating value
	 */
	protected JsonDeserializer<Object> _deserializer;

        public DelegatingConstructor(AnnotatedConstructor ctor,
				     AnnotatedMethod factory)
	{
            if (ctor != null) {
                _ctor = ctor.getAnnotated();
                _factoryMethod = null;
                _valueType = TypeFactory.fromType(ctor.getParameterType(0));
            } else if (factory != null) {
                _ctor = null;
                _factoryMethod = factory.getAnnotated();
                _valueType = TypeFactory.fromType(factory.getParameterType(0));
            } else {
                throw new IllegalArgumentException("Internal error: neither delegating constructor nor factory method passed");
            }
	}

	public JavaType getValueType() { return _valueType; }

	public void setDeserializer(JsonDeserializer<Object> deser)
	{
	    _deserializer = deser;
	}

	public Object deserialize(JsonParser jp, DeserializationContext ctxt)
	    throws IOException, JsonProcessingException
	{
	    Object value = _deserializer.deserialize(jp, ctxt);
            try {
                if (_ctor != null) {
                    return _ctor.newInstance(value);
                }
                // static method, 'obj' can be null
		return _factoryMethod.invoke(null, value);
            } catch (Exception e) {
                ClassUtil.unwrapAndThrowAsIAE(e);
		return null;
            }
	}
    }
}
