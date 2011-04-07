package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonCachable;
import org.codehaus.jackson.map.deser.impl.BeanPropertyMap;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.TokenBuffer;

/**
 * Deserializer class that can deserialize instances of
 * arbitrary bean objects, usually from JSON Object structs,
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
    /**********************************************************
    /* Information regarding type being deserialized
    /**********************************************************
     */

    /**
     * Class for which deserializer is built; used for accessing
     * annotations during resolution phase (see {@link #resolve}).
     */
    final protected AnnotatedClass _forClass;
    
    /**
     * Declared type of the bean this deserializer handles.
     */
    final protected JavaType _beanType;

    /**
     * Property that contains value to be deserialized using
     * deserializer
     * 
     * @since 1.7
     */
    final protected BeanProperty _property;
    
    /*
    /**********************************************************
    /* Construction configuration
    /**********************************************************
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
    protected final Constructor<?> _defaultConstructor;

    /**
     * If the "bean" class can be instantiated using just a single
     * String (via constructor, static method etc), this object
     * knows how to invoke method/constructor in question.
     * If so, no setters will be used.
     */
    protected final Creator.StringBased _stringCreator;

    /**
     * If the "bean" class can be instantiated using just a single
     * numeric (int, long) value  (via constructor, static method etc),
     * this object
     * knows how to invoke method/constructor in question.
     * If so, no setters will be used.
     */
    protected final Creator.NumberBased _numberCreator;

    /**
     * If the bean class can be instantiated using a creator
     * (an annotated single arg constructor or static method),
     * this object is used for handling details of how delegate-based
     * deserialization and instance construction works
     */
    protected final Creator.Delegating _delegatingCreator;

    /**
     * If the bean needs to be instantiated using constructor
     * or factory method
     * that takes one or more named properties as argument(s),
     * this creator is used for instantiation.
     */
    protected final Creator.PropertyBased _propertyBasedCreator;

    /*
    /**********************************************************
    /* Property information, setters
    /**********************************************************
     */

    /**
     * Mapping of property names to properties, built when all properties
     * to use have been succesfully resolved.
     * 
     * @since 1.7
     */
    final protected BeanPropertyMap _beanProperties;
    
    /**
     * Fallback setter used for handling any properties that are not
     * mapped to regular setters. If setter is not null, it will be
     * called once for each such property.
     */
    final protected SettableAnyProperty _anySetter;

    /**
     * In addition to properties that are set, we will also keep
     * track of recognized but ignorable properties: these will
     * be skipped without errors or warnings.
     */
    final protected HashSet<String> _ignorableProps;

    /**
     * Flag that can be set to ignore and skip unknown properties.
     * If set, will not throw an exception for unknown properties.
     */
    final protected boolean _ignoreAllUnknown;

    /**
     * We may also have one or more back reference fields (usually
     * zero or one).
     */
    final protected Map<String, SettableBeanProperty> _backRefs;
    
    /*
    /**********************************************************
    /* Special deserializers needed for sub-types
    /**********************************************************
     */

    /**
     * Lazily constructed map used to contain deserializers needed
     * for polymorphic subtypes.
     */
    protected HashMap<ClassKey, JsonDeserializer<Object>> _subDeserializers;
    
    /*
    /**********************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************
     */

    public BeanDeserializer(AnnotatedClass forClass, JavaType type, BeanProperty property,
            CreatorContainer creators,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
            HashSet<String> ignorableProps, boolean ignoreAllUnknown,
            SettableAnyProperty anySetter)
    {
        super(type);
        _forClass = forClass;
        _beanType = type;
        _property = property;
        _beanProperties = properties;
        _backRefs = backRefs;
        _ignorableProps = ignorableProps;
        _ignoreAllUnknown = ignoreAllUnknown;
        _anySetter = anySetter;

        // And then creator stuff:
        _stringCreator = creators.stringCreator();
        _numberCreator = creators.numberCreator();
        /* Delegating constructor means that the JSON Object is first deserialized
         * into delegated type, and then resulting value is passed as the argument
         * to delegating constructor.
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
        } else {
            _defaultConstructor = creators.getDefaultConstructor();
        }

    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write styling copying of settings of an existing instance.
     * 
     * @since 1.7
     */
    protected BeanDeserializer(BeanDeserializer src)
    {
        super(src._beanType);
        _forClass = src._forClass;
        _beanType = src._beanType;
        _property = src._property;
        _beanProperties = src._beanProperties;
        _backRefs = src._backRefs;
        _ignorableProps = src._ignorableProps;
        _ignoreAllUnknown = src._ignoreAllUnknown;
        _anySetter = src._anySetter;

        // and plethora of creators...
        _defaultConstructor = src._defaultConstructor;
        _stringCreator = src._stringCreator;
        _numberCreator = src._numberCreator;
        _delegatingCreator = src._delegatingCreator;
        _propertyBasedCreator = src._propertyBasedCreator;
    }

    /*
    /**********************************************************
    /* Public accessors
    /**********************************************************
     */

    public boolean hasProperty(String propertyName) {
        return _beanProperties.find(propertyName) != null;
    }

    /**
     * Accessor for checking number of deserialized properties.
     * 
     * @since 1.7
     */
    public int getPropertyCount() { 
        return _beanProperties.size();
    }
    
    /*
    /**********************************************************
    /* Validation, post-processing
    /**********************************************************
     */

    /**
     * Method called to finalize setup of this deserializer,
     * after deserializer itself has been registered. This
     * is needed to handle recursive and transitive dependencies.
     */
    public void resolve(DeserializationConfig config, DeserializerProvider provider)
        throws JsonMappingException
    {
        Iterator<SettableBeanProperty> it = _beanProperties.allProperties();
        while (it.hasNext()) {
            SettableBeanProperty prop = it.next();
            // May already have deserializer from annotations, if so, skip:
            if (!prop.hasValueDeserializer()) {
                prop.setValueDeserializer(findDeserializer(config, provider, prop.getType(), prop));
            }
            // and for [JACKSON-235] need to finally link managed references with matching back references
            String refName = prop.getManagedReferenceName();
            if (refName != null) {
                JsonDeserializer<?> valueDeser = prop._valueDeserializer;
                SettableBeanProperty backProp = null;
                boolean isContainer = false;
                if (valueDeser instanceof BeanDeserializer) {
                    backProp = ((BeanDeserializer) valueDeser).findBackReference(refName);
                } else if (valueDeser instanceof ContainerDeserializer<?>) {
                    JsonDeserializer<?> contentDeser = ((ContainerDeserializer<?>) valueDeser).getContentDeserializer();
                    if (!(contentDeser instanceof BeanDeserializer)) {
                        throw new IllegalArgumentException("Can not handle managed/back reference '"+refName
                                +"': value deserializer is of type ContainerDeserializer, but content type is not handled by a BeanDeserializer "
                                +" (instead it's of type "+contentDeser.getClass().getName()+")");
                    }
                    backProp = ((BeanDeserializer) contentDeser).findBackReference(refName);
                    isContainer = true;
                } else if (valueDeser instanceof AbstractDeserializer) { // [JACKSON-368]: not easy to fix, alas  
                    throw new IllegalArgumentException("Can not handle managed/back reference for abstract types (property "+_beanType.getRawClass().getName()+"."+prop.getName()+")");
                } else {
                    throw new IllegalArgumentException("Can not handle managed/back reference '"+refName
                            +"': type for value deserializer is not BeanDeserializer or ContainerDeserializer, but "
                            +valueDeser.getClass().getName());
                }
                if (backProp == null) {
                    throw new IllegalArgumentException("Can not handle managed/back reference '"+refName+"': no back reference property found from type "
                            +prop.getType());
                }
                // also: verify that type is compatible
                JavaType referredType = _beanType;
                JavaType backRefType = backProp.getType();
                if (!backRefType.getRawClass().isAssignableFrom(referredType.getRawClass())) {
                    throw new IllegalArgumentException("Can not handle managed/back reference '"+refName+"': back reference type ("
                            +backRefType.getRawClass().getName()+") not compatible with managed type ("
                            +referredType.getRawClass().getName()+")");
                }
                _beanProperties.replace(new SettableBeanProperty.ManagedReferenceProperty(refName, prop, backProp,
                        _forClass.getAnnotations(), isContainer));
            }
        }

        // Finally, "any setter" may also need to be resolved now
        if (_anySetter != null && !_anySetter.hasValueDeserializer()) {
            _anySetter.setValueDeserializer(findDeserializer(config, provider, _anySetter.getType(), _anySetter.getProperty()));
        }

        // as well as delegate-based constructor:
        if (_delegatingCreator != null) {
            // Need to create a temporary property to allow contextual deserializers:
            BeanProperty.Std property = new BeanProperty.Std(null, _delegatingCreator.getValueType(),
                    _forClass.getAnnotations(), _delegatingCreator.getCreator());
            JsonDeserializer<Object> deser = findDeserializer(config, provider, _delegatingCreator.getValueType(), property);
            _delegatingCreator.setDeserializer(deser);
        }
        // or property-based one
        if (_propertyBasedCreator != null) {
            for (SettableBeanProperty prop : _propertyBasedCreator.properties()) {
                if (!prop.hasValueDeserializer()) {
                    prop.setValueDeserializer(findDeserializer(config, provider, prop.getType(), prop));
                }
            }
        }
    }
    /*
    /**********************************************************
    /* JsonDeserializer implementation
    /**********************************************************
     */

    /**
     * Main deserialization method for bean-based objects (POJOs).
     */
    @Override
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
        case END_OBJECT: // added to resolve [JACKSON-319], possible related issues
            return deserializeFromObject(jp, ctxt);
	}
        throw ctxt.mappingException(getBeanClass());
    }

    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt, Object bean)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        // 23-Mar-2010, tatu: In some cases, we start with full JSON object too...
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            SettableBeanProperty prop = _beanProperties.find(propName);
            jp.nextToken(); // skip field, returns value token
            
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
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
    /**********************************************************
    /* Other public accessors
    /**********************************************************
     */

    public final Class<?> getBeanClass() { return _beanType.getRawClass(); }

    @Override public JavaType getValueType() { return _beanType; }

    /**
     * 
     * @since 1.6
     */
    public Iterator<SettableBeanProperty> properties()
    {
        if (_beanProperties == null) { // since 1.7
            throw new IllegalStateException("Can only call before BeanDeserializer has been resolved");
        }
        return _beanProperties.allProperties();
    }

    /**
     * Method needed by {@link BeanDeserializerFactory} to properly link
     * managed- and back-reference pairs.
     */
    public SettableBeanProperty findBackReference(String logicalName)
    {
        if (_backRefs == null) {
            return null;
        }
        return _backRefs.get(logicalName);
    }
    
    /*
    /**********************************************************
    /* Concrete deserialization methods
    /**********************************************************
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
    	    if (_beanType.isAbstract()) {
                throw JsonMappingException.from(jp, "Can not instantiate abstract type "+_beanType
                        +" (need to add/enable type information?)");
    	    }
            throw JsonMappingException.from(jp, "No suitable constructor found for type "+_beanType+": can not instantiate from JSON object (need to add/enable type information?)");
        }

        final Object bean = constructDefaultInstance();
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
            } else if (_anySetter != null) {
                try {
                    _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            } else {
                // Unknown: let's call handler method
                handleUnknownProperty(jp, ctxt, bean, propName);         
            }
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
    	// [JACKSON-204]: allow "" as null equivalent for POJOs...
    	if (ctxt.isEnabled(DeserializationConfig.Feature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
    	    if (jp.getTextLength() == 0) {
    	        return null;
    	    }
    	}
        throw ctxt.instantiationException(getBeanClass(), "no suitable creator method found to deserialize from JSON String");
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
        throw ctxt.instantiationException(getBeanClass(), "no suitable creator method found to deserialize from JSON Number");
    }

    public Object deserializeUsingCreator(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
    	if (_delegatingCreator != null) {
    	    try {
    	        return _delegatingCreator.deserialize(jp, ctxt);
            } catch (Exception e) {
                wrapInstantiationProblem(e, ctxt);
            }
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
                    Object bean;
                    try {
                        bean = creator.build(buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        continue; // never gets here
                    }
                //  polymorphic?
		    if (bean.getClass() != _beanType.getRawClass()) {
			return handlePolymorphic(jp, ctxt, bean, unknown);
		    }
                    if (unknown != null) { // nope, just extra unknown stuff...
                        bean = handleUnknownProperties(ctxt, bean, unknown);
		    }
		    // or just clean?
                    return deserialize(jp, ctxt, bean);
                }
                continue;
            }
            // regular property? needs buffering
            prop = _beanProperties.find(propName);
            if (prop != null) {
                buffer.bufferProperty(prop, prop.deserialize(jp, ctxt));
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                jp.skipChildren();
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
        Object bean;
        try {
            bean =  creator.build(buffer);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null; // never gets here
        }
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
    /**********************************************************
    /* Overridable helper methods
    /**********************************************************
     */

    /**
     * Method called when a JSON property is encountered that has not matching
     * setter, any-setter or field, and thus can not be assigned.
     */
    @Override
    protected void handleUnknownProperty(JsonParser jp, DeserializationContext ctxt, Object beanOrClass, String propName)
        throws IOException, JsonProcessingException
    {
        /* 22-Aug-2010, tatu: Caller now mostly checks for ignorable properties, so
         *    following should not be necessary. However, "handleUnknownProperties()" seems
         *    to still possibly need it so it is left for now.
         */
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
            JavaType type = ctxt.constructType(bean.getClass());
            /* 09-Dec-2010, tatu: Would be nice to know which property pointed to this
             *    bean... but, alas, no such information is retained, so:
             */
            subDeser = deserProv.findValueDeserializer(ctxt.getConfig(), type, _property);
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

    /**
     * Method that is called to instantiate Object of type this deserializer
     * produces, using the default (no-argument) constructor.
     * 
     * @return Instance of type this deserializer handles
     * 
     * @since 1.7
     */
    protected Object constructDefaultInstance()
    {
        try {
            return _defaultConstructor.newInstance();
        } catch (Exception e) {
            ClassUtil.unwrapAndThrowAsIAE(e);
            return null; // never gets here
        }
    }

    /*
    /**********************************************************
    /* Helper methods for error reporting
    /**********************************************************
     */

    /**
     * Method that will modify caught exception (passed in as argument)
     * as necessary to include reference information, and to ensure it
     * is a subtype of {@link IOException}, or an unchecked exception.
     *<p>
     * Rules for wrapping and unwrapping are bit complicated; essentially:
     *<ul>
     * <li>Errors are to be passed as is (if uncovered via unwrapping)
     * <li>"Plain" IOExceptions (ones that are not of type
     *   {@link JsonMappingException} are to be passed as is
     *</ul>
     */
    public void wrapAndThrow(Throwable t, Object bean, String fieldName,
            DeserializationContext ctxt)
        throws IOException
    {
        /* 05-Mar-2009, tatu: But one nasty edge is when we get
         *   StackOverflow: usually due to infinite loop. But that
         *   usually gets hidden within an InvocationTargetException...
         */
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationConfig.Feature.WRAP_EXCEPTIONS);
        // Ditto for IOExceptions; except we may want to wrap mapping exceptions
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JsonMappingException)) {
                throw (IOException) t;
            }
        } else if (!wrap) { // [JACKSON-407] -- allow disabling wrapping for unchecked exceptions
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
        // [JACKSON-55] Need to add reference information
        throw JsonMappingException.wrapWithPath(t, bean, fieldName);
    }

    public void wrapAndThrow(Throwable t, Object bean, int index, DeserializationContext ctxt)
        throws IOException
    {
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationConfig.Feature.WRAP_EXCEPTIONS);
        // Ditto for IOExceptions; except we may want to wrap mapping exceptions
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JsonMappingException)) {
                throw (IOException) t;
            }
        } else if (!wrap) { // [JACKSON-407] -- allow disabling wrapping for unchecked exceptions
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
        // [JACKSON-55] Need to add reference information
        throw JsonMappingException.wrapWithPath(t, bean, index);
    }

    protected void wrapInstantiationProblem(Throwable t, DeserializationContext ctxt)
        throws IOException
    {
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationConfig.Feature.WRAP_EXCEPTIONS);
        if (t instanceof IOException) {
            // Since we have no more information to add, let's not actually wrap..
            throw (IOException) t;
        } else if (!wrap) { // [JACKSON-407] -- allow disabling wrapping for unchecked exceptions
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
        throw ctxt.instantiationException(_beanType.getRawClass(), t);
    }
    
    /**
     * @deprecated Since 1.7 use variant that takes {@link DeserializationContext}
     */
    @Deprecated
    public void wrapAndThrow(Throwable t, Object bean, String fieldName)
        throws IOException
    {
        wrapAndThrow(t, bean, fieldName, null);
    }
    
    /**
     * @deprecated Since 1.7 use variant that takes {@link DeserializationContext}
     */
    @Deprecated
    public void wrapAndThrow(Throwable t, Object bean, int index)
        throws IOException
    {
        wrapAndThrow(t, bean, index, null);
    }    
}
