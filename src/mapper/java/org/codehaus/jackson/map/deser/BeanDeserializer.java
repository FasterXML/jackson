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
    protected StringCreator _stringCreator;

    /**
     * If the "bean" class can be instantiated using just a single
     * numeric (int, long) value  (via constructor, static method etc),
     * this object
     * knows how to invoke method/constructor in question.
     * If so, no setters will be used.
     */
    protected NumberCreator _numberCreator;

    /**
     * If the bean class can be instantiated using a creator
     * (an annotated single arg constructor or static method),
     * this object is used for handling details of how delegate-based
     * deserialization and instance construction works
     */
    protected DelegatingCreator _delegatingCreator;

    /**
     * If the beans need to be instantiated using constructor
     * or factory method
     * that takes one or more named properties as argument(s),
     * this creator is used for serialization.
     */
    protected PropertyBasedCreator _propertyBasedCreator;

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

    /**
     * Method called by factory after it has introspected all available
     * Creators (constructors, static factory methods).
     */
    public void setCreators(CreatorContainer creators)
     {
        _stringCreator = creators.stringCreator();
        _numberCreator = creators.numberCreator();
        /**
         * Delegating constructor means that
         * the JSON Object is first deserialized into delegated type, and
         * then resulting value is passed as the argument to delegating
         * constructor.
         *
         * Note that delegating constructors have precedence over default
         * and property-based constructors.
         */
        _delegatingCreator = creators.delegatingCreator();
        _propertyBasedCreator = creators.propertyBasedCreator();

	/* important: ensure we do not hold on to default constructor,
         * if delegating OR property-based creator is found
         */
	if (_delegatingCreator != null || _propertyBasedCreator != null) {
            _defaultConstructor = null;
	}
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
            && (_numberCreator == null)
            && (_stringCreator == null)
            && (_delegatingCreator == null)
            && (_propertyBasedCreator == null)) {
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
	if (_delegatingCreator != null) {
            JsonDeserializer<Object> deser = findDeserializer(config, provider, _delegatingCreator.getValueType(), "[constructor-arg[0]]", seen);
	    _delegatingCreator.setDeserializer(deser);
	}
        // or property-based one
        if (_propertyBasedCreator != null) {
            for (SettableBeanProperty prop : _propertyBasedCreator.properties()) {
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
            // 25-Jul-2009, tatu: finally, can also use "non-default" constructor (or factory method)
            if (_propertyBasedCreator != null) {
                return _deserializeUsingPropertyBased(jp, ctxt);
            }
	    // 07-Jul-2009, tatu: let's allow delegate-based approach too
	    if (_delegatingCreator != null) {
		return _delegatingCreator.deserialize(jp, ctxt);
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
	if (_stringCreator != null) {
	    return _stringCreator.construct(jp.getText());
        }
	if (_delegatingCreator != null) {
	    return _delegatingCreator.deserialize(jp, ctxt);
	}
        throw ctxt.mappingException(getBeanClass());
    }

    public Object deserializeFromNumber(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
	if (_numberCreator != null) {
            switch (jp.getNumberType()) {
            case INT:
		return _numberCreator.construct(jp.getIntValue());
            case LONG:
		return _numberCreator.construct(jp.getLongValue());
            }
	}
	if (_delegatingCreator != null) {
	    return _delegatingCreator.deserialize(jp, ctxt);
	}
	throw ctxt.mappingException(getBeanClass());
    }

    /**
     * Method called to deserialize bean using "property-based creator":
     * this means that a non-default constructor or factory method is
     * called, and then possibly other setters. The trick is that
     * values for creator method need to be buffered, first; and 
     * due to non-guaranteed ordering possibly some other properties
     * as well.
     *
     * @since 1.2
     */
    protected final Object _deserializeUsingPropertyBased(final JsonParser jp, final DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    { 
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyBuffer buffer = creator.startBuilding(jp, ctxt);

        while (true) {
            // end of JSON object?
            if (jp.nextToken() == JsonToken.END_OBJECT) {
                // if so, can just construct and leave...
                return creator.build(buffer);
            }
            String propName = jp.getCurrentName();
            // creator property?
            SettableBeanProperty prop = creator.findCreatorProperty(propName);
            if (prop != null) {
                // Last property to set?
                if (buffer.assignParameter(prop.getCreatorIndex(), prop.deserialize(jp, ctxt))) {
                    return _deserializeProperties(jp, ctxt, creator.build(buffer));
                }
                continue;
            }
            // regular property? needs buffering
            prop = _props.get(propName);
            if (prop != null) {
                buffer.bufferProperty(prop, prop.deserialize(jp, ctxt));
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(jp, ctxt));
                continue;
            }
            // Unknown? This is trickiest to deal with...
            /* !!! 25-Jul-2009, tatu: This should be improved, once we
             *   have a better way dealing with unknown properties
             */
            // need to process or skip the following token
            /*JsonToken t =*/ jp.nextToken();
            // Unknown: let's call handler method
            handleUnknownProperty(ctxt, getBeanClass(), propName);
        }
    }

    /**
     * Method that will process "extra" properties that follow
     * Creator-bound properties (if any).
     */
    protected Object _deserializeProperties(final JsonParser jp, final DeserializationContext ctxt,
                                            Object bean)
        throws IOException, JsonProcessingException
    {
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
     * @param resultBean Bean that is being populated by this deserializer;
     *   or, if not known, Class that would be instantiated to get bean
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
    // Helper classes: containers
    /////////////////////////////////////////////////////////
     */

    /**
     * Container for set of Creators (constructors, factory methods)
     */
    public final static class CreatorContainer
    {
        /// Type of bean being created
        final Class<?> _beanClass;
        final boolean _canFixAccess;

        AnnotatedMethod _strFactory, _intFactory, _longFactory;
        AnnotatedMethod _delegatingFactory;
        AnnotatedMethod _propertyBasedFactory;
        SettableBeanProperty[] _propertyBasedFactoryProperties = null;

        AnnotatedConstructor _strConstructor, _intConstructor, _longConstructor;
        AnnotatedConstructor _delegatingConstructor;
        AnnotatedConstructor _propertyBasedConstructor;
        SettableBeanProperty[] _propertyBasedConstructorProperties = null;

        public CreatorContainer(Class<?> beanClass, boolean canFixAccess) {
            _canFixAccess = canFixAccess;
            _beanClass = beanClass;
        }

        /*
        /////////////////////////////////////////////////////////
        // Setters
        /////////////////////////////////////////////////////////
        */

        public void addStringConstructor(AnnotatedConstructor ctor) {
            _strConstructor = verifyNonDup(ctor, _strConstructor, "String");
        }
        public void addIntConstructor(AnnotatedConstructor ctor) {
            _intConstructor = verifyNonDup(ctor, _intConstructor, "int");
        }
        public void addLongConstructor(AnnotatedConstructor ctor) {
            _longConstructor = verifyNonDup(ctor, _longConstructor, "long");
        }

        public void addDelegatingConstructor(AnnotatedConstructor ctor) {
            _delegatingConstructor = verifyNonDup(ctor, _delegatingConstructor, "long");
        }

        public void addPropertyConstructor(AnnotatedConstructor ctor, SettableBeanProperty[] properties)
        {
            _propertyBasedConstructor = verifyNonDup(ctor, _propertyBasedConstructor, "property-based");
            _propertyBasedConstructorProperties = properties;
        }

        public void addStringFactory(AnnotatedMethod factory) {
            _strFactory = verifyNonDup(factory, _strFactory, "String");
        }
        public void addIntFactory(AnnotatedMethod factory) {
            _intFactory = verifyNonDup(factory, _intFactory, "int");
        }
        public void addLongFactory(AnnotatedMethod factory) {
            _longFactory = verifyNonDup(factory, _longFactory, "long");
        }

        public void addDelegatingFactory(AnnotatedMethod factory) {
            _delegatingFactory = verifyNonDup(factory, _delegatingFactory, "long");
        }

        public void addPropertyFactory(AnnotatedMethod factory, SettableBeanProperty[] properties)
        {
            _propertyBasedFactory = verifyNonDup(factory, _propertyBasedFactory, "property-based");
            _propertyBasedFactoryProperties = properties;
        }

        /*
        /////////////////////////////////////////////////////////
        // Accessors
        /////////////////////////////////////////////////////////
        */

        public StringCreator stringCreator()
        {
            if (_strConstructor == null &&  _strFactory == null) {
                return null;
            }
            return new StringCreator(_beanClass, _strConstructor, _strFactory);
        }

        public NumberCreator numberCreator()
        {
            if (_intConstructor == null && _intFactory == null
                || _longConstructor == null && _longFactory == null) {
                return null;
            }
            return new NumberCreator(_beanClass, _intConstructor, _intFactory,
                                     _longConstructor, _longFactory);
        }

        public DelegatingCreator delegatingCreator()
        {
            if (_delegatingConstructor == null && _delegatingFactory == null) {
                return null;
            }
            return new DelegatingCreator(_delegatingConstructor, _delegatingFactory);
        }

        public PropertyBasedCreator propertyBasedCreator()
        {
            if (_propertyBasedConstructor == null && _propertyBasedFactory == null) {
                return null;
            }
            return new PropertyBasedCreator(_propertyBasedConstructor, _propertyBasedConstructorProperties,
                                            _propertyBasedFactory, _propertyBasedFactoryProperties);
        }

        /*
        /////////////////////////////////////////////////////////
        // Helper methods
        /////////////////////////////////////////////////////////
        */

        protected AnnotatedConstructor verifyNonDup(AnnotatedConstructor newOne, AnnotatedConstructor oldOne,
                                                    String type)
        {
            if (oldOne != null) {
                throw new IllegalArgumentException("Conflicting "+type+" constructors: already had "+oldOne+", encountered "+newOne);
            }
            if (_canFixAccess) {
                ClassUtil.checkAndFixAccess(newOne.getAnnotated());
            }
            return newOne;
        }
        
        protected AnnotatedMethod verifyNonDup(AnnotatedMethod newOne, AnnotatedMethod oldOne,
                                               String type)
        {
            if (oldOne != null) {
                throw new IllegalArgumentException("Conflicting "+type+" factory methods: already had "+oldOne+", encountered "+newOne);
            }
            if (_canFixAccess) {
                ClassUtil.checkAndFixAccess(newOne.getAnnotated());
            }
            return newOne;
        }
    }

    /*
    /////////////////////////////////////////////////////////
    // Helper classes: concreate Creators
    /////////////////////////////////////////////////////////
     */

    /**
     * Helper class that can handle simple deserialization from
     * Json String values.
     */
    final static class StringCreator
    {
        protected final Class<?> _valueClass;
        protected final Method _factoryMethod;
        protected final Constructor<?> _ctor;

        public StringCreator(Class<?> valueClass, AnnotatedConstructor ctor,
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

    final static class NumberCreator
    {
        protected final Class<?> _valueClass;

        protected final Constructor<?> _intCtor;
        protected final Constructor<?> _longCtor;

        protected final Method _intFactoryMethod;
        protected final Method _longFactoryMethod;

        public NumberCreator(Class<?> valueClass,
                             AnnotatedConstructor intCtor, AnnotatedMethod ifm,
                             AnnotatedConstructor longCtor, AnnotatedMethod lfm)
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
    final static class DelegatingCreator
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

        public DelegatingCreator(AnnotatedConstructor ctor,
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

    /**
     * Helper class used to handle details of using a "non-default"
     * creator (constructor or factory that takes one or more arguments
     * that represent logical bean properties)
     */
    final static class PropertyBasedCreator
    {
        protected final Constructor<?> _ctor;
        protected final Method _factoryMethod;

        /**
         * Map that contains property objects for either constructor or factory
         * method (whichever one is null: one property for each
         * parameter for that one), keyed by logical property name
         */
        protected final HashMap<String, SettableBeanProperty> _properties;

        public PropertyBasedCreator(AnnotatedConstructor ctor, SettableBeanProperty[] ctorProps,
                                    AnnotatedMethod factory, SettableBeanProperty[] factoryProps)
        {
            // We will only use one: and constructor has precedence over factory
            SettableBeanProperty[] props;
            if (ctor != null) {
                _ctor = ctor.getAnnotated();
                _factoryMethod = null;
                props = ctorProps;
            } else if (factory != null) {
                _ctor = null;
                _factoryMethod = factory.getAnnotated();
                props = factoryProps;
            } else {
                throw new IllegalArgumentException("Internal error: neither delegating constructor nor factory method passed");
            }
            _properties = new HashMap<String, SettableBeanProperty>();
            for (SettableBeanProperty prop : props) {
                _properties.put(prop.getPropertyName(), prop);
            }
        }

        public Collection<SettableBeanProperty> properties() {
            return _properties.values();
        }

        public SettableBeanProperty findCreatorProperty(String name) {
            return _properties.get(name);
        }

        /**
         * Method called when starting to build a bean instance.
         */
        public PropertyBuffer startBuilding(JsonParser jp, DeserializationContext ctxt)
        {
            return new PropertyBuffer(jp, ctxt, _properties.size());
        }

        public Object build(PropertyBuffer buffer)
            throws IOException, JsonProcessingException
        {
            Object bean;
            try {
                if (_ctor != null) {
                    bean = _ctor.newInstance(buffer.getParameters());
                } else {
                    bean =  _factoryMethod.invoke(null, buffer.getParameters());
                }
            } catch (Exception e) {
                ClassUtil.unwrapAndThrowAsIAE(e);
                return null; // never gets here
            }
            // Anything buffered?
            for (PropValue pv = buffer.buffered(); pv != null; pv = pv.next) {
                pv.assign(bean);
            }
            return bean;
        }
    }

    /*
    /////////////////////////////////////////////////////////
    // Helper classes: builder used with property-based Creator
    /////////////////////////////////////////////////////////
     */

    /**
     * Stateful object used to store state during construction
     * of beans that use Creators, and hence need buffering
     * before instance is constructed.
     */
    final static class PropertyBuffer
    {
        final JsonParser _parser;
        final DeserializationContext _context;

        /**
         * Buffer for storing creator parameters before constructing
         * instance
         */
        final Object[] _creatorParameters;

        /**
         * Number of creator parameters we are still missing.
         *<p>
         * NOTE: assumes there are no duplicates, for now.
         */
        private int _paramsNeeded;

        /**
         * If we get non-creator parameters before or between
         * creator parameters, those need to be buffered. Buffer
         * is just a simple linked list
         */
        private PropValue _buffered;

        public PropertyBuffer(JsonParser jp, DeserializationContext ctxt,
                              int paramCount)
        {
            _parser = jp;
            _context = ctxt;
            _paramsNeeded = paramCount;
            _creatorParameters = new Object[paramCount];
        }

        protected final Object[] getParameters() { return _creatorParameters; }
        protected PropValue buffered() { return _buffered; }

        /**
         * @return True if we have received all creator parameters
         */
        public boolean assignParameter(int index, Object value) {
            _creatorParameters[index] = value;
            return --_paramsNeeded <= 0;
        }

        public void bufferProperty(SettableBeanProperty prop, Object value) {
            _buffered = new RegularPropValue(_buffered, value, prop);
        }

        public void bufferAnyProperty(SettableAnyProperty prop, String propName, Object value) {
            _buffered = new AnyPropValue(_buffered, value, prop, propName);
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Helper classes for buffering property values
    /////////////////////////////////////////////////////
    */

    abstract static class PropValue
    {
        public final PropValue next;
        public final Object value;
        
        protected PropValue(PropValue next, Object value)
        {
            this.next = next;
            this.value = value;
        }

        public abstract void assign(Object bean)
            throws IOException, JsonProcessingException;
    }
    
    final static class RegularPropValue
        extends PropValue
    {
        final SettableBeanProperty _property;
        
        public RegularPropValue(PropValue next, Object value,
                                SettableBeanProperty prop)
        {
            super(next, value);
            _property = prop;
        }

        public void assign(Object bean)
            throws IOException, JsonProcessingException
        {
            _property.set(bean, value);
        }
    }
    
    final static class AnyPropValue
        extends PropValue
    {
        final SettableAnyProperty _property;
        final String _propertyName;
        
        public AnyPropValue(PropValue next, Object value,
                            SettableAnyProperty prop,
                            String propName)
        {
            super(next, value);
            _property = prop;
            _propertyName = propName;
        }

        public void assign(Object bean)
            throws IOException, JsonProcessingException
        {
            _property.set(bean, _propertyName, value);
        }
    }
}
