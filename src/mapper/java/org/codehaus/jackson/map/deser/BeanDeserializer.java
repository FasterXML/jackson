package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonCachable;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.TokenBuffer;

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
    /*************************************************
    /* Information regarding type being deserialized
    /*************************************************
     */

    final protected JavaType _beanType;

    /*
    /*************************************************
    /* Construction configuration
    /*************************************************
     */

    /**
     * Default constructor used to instantiate the bean when mapping
     * from Json object, and only using setters for initialization
     * (not specific constructors).
     *<p>
     * Note: may be null, if deserializer is constructed for abstract
     * types (which is only useful if additional type information will
     * allow construction of concrete subtype).
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
    /********************************************************
    /* Property information, setters
    /********************************************************
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

    /**
     * Flag that can be set to ignore and skip unknown properties.
     * If set, will not throw an exception for unknown properties.
     */
    protected boolean _ignoreAllUnknown;

    /*
    /********************************************************
    /* Special deserializers needed for sub-types
    /********************************************************
     */

    /**
     * Lazily constructed map used to contain deserializers needed
     * for polymorphic subtypes.
     */
    protected HashMap<ClassKey, JsonDeserializer<Object>> _subDeserializers;
    
    /*
    /********************************************************
    /* Life-cycle, construction, initialization
    /********************************************************
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
        /* Delegating constructor means that
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

    public void setIgnoreUnknownProperties(boolean ignore) {
        _ignoreAllUnknown = ignore;
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
    /********************************************************
    /* Validation, post-processing
    /********************************************************
     */

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
    /********************************************************
    /* JsonDeserializer implementation
    /********************************************************
     */

    /**
     * Main deserialization method for bean-based objects (POJOs).
     */
    public final Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        // common case first:
        if (t == JsonToken.START_OBJECT) {
            jp.nextToken();
            return deserializeFromObject(jp, ctxt);
        }
        // and then others, generally requiring use of @JsonCreator
        switch (t) {
        case VALUE_STRING:
	    return deserializeFromString(jp, ctxt);
        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
	    return deserializeFromNumber(jp, ctxt);
        case VALUE_EMBEDDED_OBJECT:
            return jp.getEmbeddedObject();
        case VALUE_TRUE:
        case VALUE_FALSE:
        case START_ARRAY:
            // these only work if there's a (delegating) creator...
            return deserializeUsingCreator(jp, ctxt);
        case FIELD_NAME:
            return deserializeFromObject(jp, ctxt);
	}
        throw ctxt.mappingException(getBeanClass());
    }

    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    public Object deserialize(JsonParser jp, DeserializationContext ctxt, Object bean)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            SettableBeanProperty prop = _props.get(propName);
            jp.nextToken(); // skip field, returns value token
            
            if (prop != null) { // normal case
                prop.deserializeAndSet(jp, ctxt, bean);
                continue;
            }
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                continue;
                }
            // Unknown: let's call handler method
            handleUnknownProperty(jp, ctxt, bean, propName);
        }
        return bean;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromObject(jp, ctxt);
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
    	    // should only occur for abstract types...
            throw JsonMappingException.from(jp, "No default constructor found for type "+_beanType+": can not instantiate from JSON object (need to add/enable type information?)");
        }

        Object bean;
        try {
            bean = _defaultConstructor.newInstance();
        } catch (Exception e) {
            ClassUtil.unwrapAndThrowAsIAE(e);
            return null; // never gets here
        }
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();
            SettableBeanProperty prop = _props.get(propName);
            if (prop != null) { // normal case
                prop.deserializeAndSet(jp, ctxt, bean);
                continue;
            }
            /* 08-Nov-2009, tatus: Should we first check ignorable properties,
             *    and skip? For now, we'll just call any setter.
             */
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                continue;
            }
            // Unknown: let's call handler method
            handleUnknownProperty(jp, ctxt, bean, propName);         
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
        throw ctxt.instantiationException(getBeanClass(), "no suitable creator method found");
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
        throw ctxt.instantiationException(getBeanClass(), "no suitable creator method found");
    }

    public Object deserializeUsingCreator(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
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

        // 04-Jan-2010, tatu: May need to collect unknown properties for polymorphic cases
        TokenBuffer unknown = null;

        JsonToken t = jp.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty prop = creator.findCreatorProperty(propName);
            if (prop != null) {
                // Last creator property to set?
                Object value = prop.deserialize(jp, ctxt);
                if (buffer.assignParameter(prop.getCreatorIndex(), value)) {
                    jp.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object bean = creator.build(buffer);
		    //  polymorphic?
		    if (bean.getClass() != _beanType.getRawClass()) {
			return handlePolymorphic(jp, ctxt, bean, unknown);
		    }
                    if (unknown != null) { // nope, just extra unknown stuff...
                        return handleUnknownProperties(ctxt, bean, unknown);
		    }
		    // or just clean?
                    return deserialize(jp, ctxt, bean);
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
            // Ok then, let's collect the whole field; name and value
            if (unknown == null) {
                unknown = new TokenBuffer(jp.getCodec());
            }
            unknown.writeFieldName(propName);
            unknown.copyCurrentStructure(jp);
        }

        // We hit END_OBJECT, so:
        Object bean =  creator.build(buffer);
        if (unknown != null) {
            // polymorphic?
            if (bean.getClass() != _beanType.getRawClass()) {
                return handlePolymorphic(null, ctxt, bean, unknown);
            }
            // no, just some extra unknown properties
            return handleUnknownProperties(ctxt, bean, unknown);
        }
        return bean;
    }

    /*
    /////////////////////////////////////////////////////////
    // Overridable helper methods
    /////////////////////////////////////////////////////////
     */

    /**
     * Method called when a JSON property is encountered that has not matching
     * setter, any-setter or field, and thus can not be assigned.
     */
    @Override
    protected void handleUnknownProperty(JsonParser jp, DeserializationContext ctxt, Object beanOrClass, String propName)
        throws IOException, JsonProcessingException
    {
        // If registered as ignorable, skip
        if (_ignoreAllUnknown ||
            (_ignorableProps != null && _ignorableProps.contains(propName))) {
            jp.skipChildren();
            return;
        }
        /* Otherwise use default handling (call handler(s); if not
         * handled, throw exception or skip depending on settings)
         */
        super.handleUnknownProperty(jp, ctxt, beanOrClass, propName);
    }

    /**
     * Method called to handle set of one or more unknown properties,
     * stored in their entirety in given {@link TokenBuffer}
     * (as field entries, name and value).
     */
    protected Object handleUnknownProperties(DeserializationContext ctxt, Object bean, TokenBuffer unknownTokens)
        throws IOException, JsonProcessingException
    {
        // First: add closing END_OBJECT as marker
        unknownTokens.writeEndObject();
        
        // note: buffer does NOT have starting START_OBJECT
        JsonParser bufferParser = unknownTokens.asParser();
        while (bufferParser.nextToken() != JsonToken.END_OBJECT) {
            String propName = bufferParser.getCurrentName();
            // Unknown: let's call handler method
            bufferParser.nextToken();
            handleUnknownProperty(bufferParser, ctxt, bean, propName);
        }
        return bean;
    }

    /**
     * Method called in cases where we may have polymorphic deserialization
     * case: that is, type of Creator-constructed bean is not the type
     * of deserializer itself. It should be a sub-class or implementation
     * class; either way, we may have more specific deserializer to use
     * for handling it.
     *
     * @param jp (optional) If not null, parser that has more properties to handle
     *   (in addition to buffered properties); if null, all properties are passed
     *   in buffer
     */
    protected Object handlePolymorphic(JsonParser jp, DeserializationContext ctxt,
				       Object bean, TokenBuffer unknownTokens)
        throws IOException, JsonProcessingException
    {  
        // First things first: maybe there is a more specific deserializer available?
	JsonDeserializer<Object> subDeser = _findSubclassDeserializer(ctxt, bean, unknownTokens);
	if (subDeser != null) {
	    if (unknownTokens != null) {
		// need to add END_OBJECT marker first
		unknownTokens.writeEndObject();
                JsonParser p2 = unknownTokens.asParser();
                p2.nextToken(); // to get to first data field
		bean = subDeser.deserialize(p2, ctxt, bean);
	    }
	    // Original parser may also have some leftovers
	    if (jp != null) {
		bean = subDeser.deserialize(jp, ctxt, bean);
	    }
	    return bean;
	}
	// nope; need to use this deserializer. Unknowns we've seen so far?
	if (unknownTokens != null) {
	    bean = handleUnknownProperties(ctxt, bean, unknownTokens);
	}
	// and/or things left to process via main parser?
	if (jp != null) {
	    bean = deserialize(jp, ctxt, bean);
	}
	return bean;
    }

    /**
     * Helper method called to (try to) locate deserializer for given sub-type of
     * type that this deserializer handles.
     */
    protected JsonDeserializer<Object> _findSubclassDeserializer(DeserializationContext ctxt, Object bean, TokenBuffer unknownTokens)
        throws IOException, JsonProcessingException
    {  
        JsonDeserializer<Object> subDeser;

        // First: maybe we have already created sub-type deserializer?
        synchronized (this) {
            subDeser = (_subDeserializers == null) ? null : _subDeserializers.get(new ClassKey(bean.getClass()));
        }
        if (subDeser != null) {
            return subDeser;
        }
        // If not, maybe we can locate one. First, need provider
        DeserializerProvider deserProv = ctxt.getDeserializerProvider();
        if (deserProv != null) {
            JavaType type = TypeFactory.type(bean.getClass());
            subDeser = deserProv.findValueDeserializer(ctxt.getConfig(), type, null, "*this*");
            // Also, need to cache it
            if (subDeser != null) {
                synchronized (this) {
                    if (_subDeserializers == null) {
                        _subDeserializers = new HashMap<ClassKey,JsonDeserializer<Object>>();;
                    }
                    _subDeserializers.put(new ClassKey(bean.getClass()), subDeser);
                }            
            }
        }
        return subDeser;
    }
}
