package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonCachable;
import org.codehaus.jackson.map.util.ClassUtil;
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
    extends StdDeserializer<Object>
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
    protected Creator.StringBased _stringCreator;

    /**
     * If the "bean" class can be instantiated using just a single
     * numeric (int, long) value  (via constructor, static method etc),
     * this object
     * knows how to invoke method/constructor in question.
     * If so, no setters will be used.
     */
    protected Creator.NumberBased _numberCreator;

    /**
     * If the bean class can be instantiated using a creator
     * (an annotated single arg constructor or static method),
     * this object is used for handling details of how delegate-based
     * deserialization and instance construction works
     */
    protected Creator.Delegating _delegatingCreator;

    /**
     * If the bean needs to be instantiated using constructor
     * or factory method
     * that takes one or more named properties as argument(s),
     * this creator is used for instantiation.
     */
    protected Creator.PropertyBased _propertyBasedCreator;

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
        super(type.getRawClass());
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
        HashMap<JavaType, JsonDeserializer<Object>> seen = new HashMap<JavaType, JsonDeserializer<Object>>();

        for (SettableBeanProperty prop : _props.values()) {
            // May already have deserializer from annotations, if so, skip:
            if (prop.hasValueDeserializer()) {
                continue;
            }
            prop.setValueDeserializer(findDeserializer(config, provider, prop.getType(), prop.getPropertyName(), seen));
        }

        // Finally, "any setter" may also need to be resolved now
        if (_anySetter != null && !_anySetter.hasValueDeserializer()) {
            _anySetter.setValueDeserializer(findDeserializer(config, provider, _anySetter.getType(), "[any]", seen));
        }

	// as well as delegate-based constructor:
	if (_delegatingCreator != null) {
            JsonDeserializer<Object> deser = findDeserializer(config, provider, _delegatingCreator.getValueType(), "[constructor-arg[0]]", seen);
	    _delegatingCreator.setDeserializer(deser);
	}
        // or property-based one
        if (_propertyBasedCreator != null) {
            for (SettableBeanProperty prop : _propertyBasedCreator.properties()) {
                prop.setValueDeserializer(findDeserializer(config, provider, prop.getType(), prop.getPropertyName(), seen));
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

    @Override public JavaType getValueType() { return _beanType; }

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
        final Creator.PropertyBased creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt);

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

    @Override
    protected void handleUnknownProperty(DeserializationContext ctxt, Object beanOrClass, String propName)
        throws IOException, JsonProcessingException
    {
        // If registered as ignorable, skip
        if (_ignorableProps != null && _ignorableProps.contains(propName)) {
            ctxt.getParser().skipChildren();
            return;
        }
        /* Otherwise use default handling (call handler(s); if not
         * handled, throw exception or skip depending on settings)
         */
        super.handleUnknownProperty(ctxt, beanOrClass, propName);
    }
}
