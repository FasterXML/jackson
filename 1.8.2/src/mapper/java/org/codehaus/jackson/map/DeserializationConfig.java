package org.codehaus.jackson.map;

import java.text.DateFormat;
import java.util.*;

import org.codehaus.jackson.Base64Variant;
import org.codehaus.jackson.Base64Variants;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.SubtypeResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.map.util.LinkedNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Object that contains baseline configuration for deserialization
 * process. An instance is owned by {@link ObjectMapper}, which makes
 * a copy that is passed during serialization process to
 * {@link DeserializerProvider} and {@link DeserializerFactory}.
 *<p>
 * Note: although configuration settings can be changed at any time
 * (for factories and instances), they are not guaranteed to have
 * effect if called after constructing relevant mapper or deserializer
 * instance. This because some objects may be configured, constructed and
 * cached first time they are needed.
 */
public class DeserializationConfig
    extends MapperConfig<DeserializationConfig>
{
    /**
     * Enumeration that defines togglable features that guide
     * the serialization feature.
     */
    public enum Feature {
        /*
        /******************************************************
         *  Introspection features
        /******************************************************
         */

        /**
         * Feature that determines whether annotation introspection
         * is used for configuration; if enabled, configured
         * {@link AnnotationIntrospector} will be used: if disabled,
         * no annotations are considered.
         *<P>
         * Feature is enabled by default.
         *
         * @since 1.2
         */
        USE_ANNOTATIONS(true),

        /**
         * Feature that determines whether "setter" methods are
         * automatically detected based on standard Bean naming convention
         * or not. If yes, then all public one-argument methods that
         * start with prefix "set"
         * are considered setters. If disabled, only methods explicitly
         * annotated are considered setters.
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_SETTERS(true),

        /**
         * Feature that determines whether "creator" methods are
         * automatically detected by consider public constructors,
         * and static single argument methods with name "valueOf".
         * If disabled, only methods explicitly annotated are considered
         * creator methods (except for the no-arg default constructor which
         * is always considered a factory method).
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_CREATORS(true),

        /**
         * Feature that determines whether non-static fields are recognized as
         * properties.
         * If yes, then all public member fields
         * are considered as properties. If disabled, only fields explicitly
         * annotated are considered property fields.
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         *
         * @since 1.1
         */
        AUTO_DETECT_FIELDS(true),

        /**
         * Feature that determines whether otherwise regular "getter"
         * methods (but only ones that handle Collections and Maps,
         * not getters of other type)
         * can be used for purpose of getting a reference to a Collection
         * and Map to modify the property, without requiring a setter
         * method.
         * This is similar to how JAXB framework sets Collections and
         * Maps: no setter is involved, just setter.
         *<p>
         * Note that such getters-as-setters methods have lower
         * precedence than setters, so they are only used if no
         * setter is found for the Map/Collection property.
         *<p>
         * Feature is enabled by default.
         */
        USE_GETTERS_AS_SETTERS(true),

        /**
         * Feature that determines whether method and field access
         * modifier settings can be overridden when accessing
         * properties. If enabled, method
         * {@link java.lang.reflect.AccessibleObject#setAccessible}
         * may be called to enable access to otherwise unaccessible
         * objects.
         */
        CAN_OVERRIDE_ACCESS_MODIFIERS(true),

        /*
        /******************************************************
         *  Type conversion features
        /******************************************************
         */

        /**
         * Feature that determines whether Json floating point numbers
         * are to be deserialized into {@link java.math.BigDecimal}s
         * if only generic type description (either {@link Object} or
         * {@link Number}, or within untyped {@link java.util.Map}
         * or {@link java.util.Collection} context) is available.
         * If enabled such values will be deserialized as {@link java.math.BigDecimal}s;
         * if disabled, will be deserialized as {@link Double}s.
         * <p>
         * Feature is disabled by default, meaning that "untyped" floating
         * point numbers will by default be deserialized as {@link Double}s
         * (choice is for performance reason -- BigDecimals are slower than
         * Doubles)
         */
        USE_BIG_DECIMAL_FOR_FLOATS(false),

        /**
         * Feature that determines whether Json integral (non-floating-point)
         * numbers are to be deserialized into {@link java.math.BigInteger}s
         * if only generic type description (either {@link Object} or
         * {@link Number}, or within untyped {@link java.util.Map}
         * or {@link java.util.Collection} context) is available.
         * If enabled such values will be deserialized as
         * {@link java.math.BigInteger}s;
         * if disabled, will be deserialized as "smallest" available type,
         * which is either {@link Integer}, {@link Long} or
         * {@link java.math.BigInteger}, depending on number of digits.
         * <p>
         * Feature is disabled by default, meaning that "untyped" floating
         * point numbers will by default be deserialized using whatever
         * is the most compact integral type, to optimize efficiency.
         */
        USE_BIG_INTEGER_FOR_INTS(false),

        /**
         * Feature that determines standard deserialization mechanism used for
         * Enum values: if enabled, Enums are assumed to have been serialized  using
         * return value of <code>Enum.toString()</code>;
         * if disabled, return value of <code>Enum.name()</code> is assumed to have been used.
         * Since pre-1.6 method was to use Enum name, this is the default.
         *<p>
         * Note: this feature should usually have same value
         * as {@link SerializationConfig.Feature#WRITE_ENUMS_USING_TO_STRING}.
         *<p>
         * For further details, check out [JACKSON-212]
         * 
         * @since 1.6
         */
        READ_ENUMS_USING_TO_STRING(false),
        
        /*
        /******************************************************
         *  Error handling features
        /******************************************************
         */

        /**
         * Feature that determines whether encountering of unknown
         * properties (ones that do not map to a property, and there is
         * no "any setter" or handler that can handle it)
         * should result in a failure (by throwing a
         * {@link JsonMappingException}) or not.
         * This setting only takes effect after all other handling
         * methods for unknown properties have been tried, and
         * property remains unhandled.
         *<p>
         * Feature is enabled by default, meaning that 
         * {@link JsonMappingException} is thrown if an unknown property
         * is encountered. This is the implicit default prior to
         * introduction of the feature.
         *
         * @since 1.2
         */
        FAIL_ON_UNKNOWN_PROPERTIES(true),

        /**
         * Feature that determines whether encountering of JSON null
         * is an error when deserializing into Java primitive types
         * (like 'int' or 'double'). If it is, a JsonProcessingException
         * is thrown to indicate this; if not, default value is used
         * (0 for 'int', 0.0 for double, same defaulting as what JVM uses).
         *<p>
         * Feature is disabled by default (to be consistent with behavior
         * of Jackson 1.6),
         * i.e. to allow use of nulls for primitive properties.
         * 
         * @since 1.7
         */
        FAIL_ON_NULL_FOR_PRIMITIVES(false),

        /**
         * Feature that determines whether JSON integer numbers are valid
         * values to be used for deserializing Java enum values.
         * If set to 'false' numbers are acceptable and are used to map to
         * ordinal() of matching enumeration value; if 'true', numbers are
         * not allowed and a {@link JsonMappingException} will be thrown.
         * Latter behavior makes sense if there is concern that accidental
         * mapping from integer values to enums might happen (and when enums
         * are always serialized as JSON Strings)
         *<p>
         * Feature is disabled by default (to be consistent with behavior
         * of Jackson 1.6), 
         * i.e. to allow use of JSON integers for Java enums.
         * 
         * @since 1.7
         */
        FAIL_ON_NUMBERS_FOR_ENUMS(false),

        /**
         * Feature that determines whether Jackson code should catch
         * and wrap {@link Exception}s (but never {@link Error}s!)
         * to add additional information about
         * location (within input) of problem or not. If enabled,
         * most exceptions will be caught and re-thrown (exception
         * specifically being that {@link java.io.IOException}s may be passed
         * as is, since they are declared as throwable); this can be
         * convenient both in that all exceptions will be checked and
         * declared, and so there is more contextual information.
         * However, sometimes calling application may just want "raw"
         * unchecked exceptions passed as is.
         *<p>
         * Feature is enabled by default, and is similar in behavior
         * to default prior to 1.7.
         * 
         * @since 1.7
         */
        WRAP_EXCEPTIONS(true),
        
        /*
        /******************************************************
         *  Structural conversion features
        /******************************************************
         */

        /**
         * Feature that was planned to be enabled to handle "wrapped" values
         * (see {@link SerializationConfig.Feature#WRAP_ROOT_VALUE}
         * for details).
         * <b>NOTE</b>: Not implemented (unlike its counterpart for serialization
         * which was implemented in 1.7)
         * 
         * @deprecated Never implemented; plus, incorrectly named: should be
         *    "UNWRAP_ROOT_VALUE" to be of use. Feature such named may be added in future.
         */
        @Deprecated
        WRAP_ROOT_VALUE(false),
        
        /**
         * Feature that can be enabled to allow JSON empty String
         * value ("") to be bound to POJOs as null.
         * If disabled, standard POJOs can only be bound from JSON null or
         * JSON Object (standard meaning that no custom deserializers or
         * constructors are defined; both of which can add support for other
         * kinds of JSON values); if enable, empty JSON String can be taken
         * to be equivalent of JSON null.
         * 
         * @since 1.8
         */
        ACCEPT_EMPTY_STRING_AS_NULL_OBJECT(false),

        /**
         * Feature that determines whether it is acceptable to coerce non-array
         * (in JSON) values to work with Java collection (arrays, java.util.Collection)
         * types. If enabled, collection deserializers will try to handle non-array
         * values as if they had "implicit" surrounding JSON array.
         * This feature is meant to be used for compatibility/interoperability reasons,
         * to work with packages (such as XML-to-JSON converters) that leave out JSON
         * array in cases where there is just a single element in array.
         * 
         * @since 1.8
         */
        ACCEPT_SINGLE_VALUE_AS_ARRAY(false)
        
        /*
        /******************************************************
         *  Other features
        /******************************************************
         */
        
        ;

        final boolean _defaultState;

        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
	        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
        }
	        
        public boolean enabledByDefault() { return _defaultState; }
    
        public int getMask() { return (1 << ordinal()); }
    }

    /**
     * Bitfield (set of flags) of all Features that are enabled
     * by default.
     */
    protected final static int DEFAULT_FEATURE_FLAGS = Feature.collectDefaults();

    /*
    /**********************************************************
    /* Configuration settings for deserialization
    /**********************************************************
     */

    /**
     * Bit set that contains all enabled features
     */
    protected int _featureFlags = DEFAULT_FEATURE_FLAGS;

    /**
     * Linked list that contains all registered problem handlers.
     * Implementation as front-added linked list allows for sharing
     * of the list (tail) without copying the list.
     */
    protected LinkedNode<DeserializationProblemHandler> _problemHandlers;

    /**
     * To support on-the-fly class generation for interface and abstract classes
     * it is possible to register "abstract type resolver".
     *<p>
     * Non-final to support deprecated legacy methods; should be made final for 2.0
     * 
     * @since 1.6
     */
    protected AbstractTypeResolver _abstractTypeResolver;
    
    /**
     * Factory used for constructing {@link org.codehaus.jackson.JsonNode} instances.
     *<p>
     * Non-final to support deprecated legacy methods; should be made final for 2.0
     * 
     * @since 1.6
     */
    protected JsonNodeFactory _nodeFactory;
    
    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    /**
     * Constructor used by ObjectMapper to create default configuration object instance.
     */
    public DeserializationConfig(ClassIntrospector<? extends BeanDescription> intr,
            AnnotationIntrospector annIntr, VisibilityChecker<?> vc,
            SubtypeResolver subtypeResolver, PropertyNamingStrategy propertyNamingStrategy,
            TypeFactory typeFactory, HandlerInstantiator handlerInstantiator)
    {
        super(intr, annIntr, vc, subtypeResolver, propertyNamingStrategy, typeFactory, handlerInstantiator);
        _nodeFactory = JsonNodeFactory.instance;
    }

    /**
     * @since 1.8
     */
    protected DeserializationConfig(DeserializationConfig src) {
        this(src, src._base);
    }

    /**
     * Copy constructor used to create a non-shared instance with given mix-in
     * annotation definitions and subtype resolver.
     * 
     * @since 1.8
     */
    private DeserializationConfig(DeserializationConfig src,
            HashMap<ClassKey,Class<?>> mixins, SubtypeResolver str)
    {
        this(src, src._base);
        _mixInAnnotations = mixins;
        _subtypeResolver = str;
    }
    
    /**
     * @since 1.8
     */
    protected DeserializationConfig(DeserializationConfig src, MapperConfig.Base base)
    {
        super(src, base, src._subtypeResolver);
        _featureFlags = src._featureFlags;
        _abstractTypeResolver = src._abstractTypeResolver;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
    }
    
    /**
     * @since 1.8
     */
    protected DeserializationConfig(DeserializationConfig src, JsonNodeFactory f)
    {
        super(src);
        _featureFlags = src._featureFlags;
        _abstractTypeResolver = src._abstractTypeResolver;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = f;
    }
    
    /*
    /**********************************************************
    /* Life-cycle, factory methods from MapperConfig
    /**********************************************************
     */

    @Override
    public DeserializationConfig withClassIntrospector(ClassIntrospector<? extends BeanDescription> ci) {
        return new DeserializationConfig(this, _base.withClassIntrospector(ci));
    }

    @Override
    public DeserializationConfig withAnnotationIntrospector(AnnotationIntrospector ai) {
        return new DeserializationConfig(this, _base.withAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig withVisibilityChecker(VisibilityChecker<?> vc) {
        return new DeserializationConfig(this, _base.withVisibilityChecker(vc));
    }

    @Override
    public DeserializationConfig withTypeResolverBuilder(TypeResolverBuilder<?> trb) {
        return new DeserializationConfig(this, _base.withTypeResolverBuilder(trb));
    }

    @Override
    public DeserializationConfig withSubtypeResolver(SubtypeResolver str)
    {
        DeserializationConfig cfg = new DeserializationConfig(this);
        cfg._subtypeResolver = str;
        return cfg;
    }
    
    @Override
    public DeserializationConfig withPropertyNamingStrategy(PropertyNamingStrategy pns) {
        return new DeserializationConfig(this, _base.withPropertyNamingStrategy(pns));
    }
    
    @Override
    public DeserializationConfig withTypeFactory(TypeFactory tf) {
        return (tf == _base.getTypeFactory()) ? this : new DeserializationConfig(this, _base.withTypeFactory(tf));
    }

    @Override
    public DeserializationConfig withDateFormat(DateFormat df) {
        return (df == _base.getDateFormat()) ? this : new DeserializationConfig(this, _base.withDateFormat(df));
    }
    
    @Override
    public DeserializationConfig withHandlerInstantiator(HandlerInstantiator hi) {
        return (hi == _base.getHandlerInstantiator()) ? this : new DeserializationConfig(this, _base.withHandlerInstantiator(hi));
    }
    
    /*
    /**********************************************************
    /* Life-cycle, deserialization-specific factory methods
    /**********************************************************
     */

    /**
     * @since 1.8
     */
    public DeserializationConfig withNodeFactory(JsonNodeFactory f) {
        return new DeserializationConfig(this, f);
    }
    
    /*
    /**********************************************************
    /* Configuration: on/off features
    /**********************************************************
     */

    /**
     * Method for enabling specified  feature.
     */
    public void enable(Feature f) {
        _featureFlags |= f.getMask();
    }

    /**
     * Method for disabling specified feature.
     */
    public void disable(Feature f) {
        _featureFlags &= ~f.getMask();
    }

    /**
     * Method for enabling or disabling specified feature.
     */
    public void set(Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
    }

    /**
     * Method for checking whether given feature is enabled or not
     */
    public final boolean isEnabled(Feature f) {
        return (_featureFlags & f.getMask()) != 0;
    }

    //protected int getFeatures() { return _generatorFeatures; }
    
    /*
    /**********************************************************
    /* MapperConfig implementation
    /**********************************************************
     */

    /**
     * Method that checks class annotations that the argument Object has,
     * and modifies settings of this configuration object accordingly,
     * similar to how those annotations would affect actual value classes
     * annotated with them, but with global scope. Note that not all
     * annotations have global significance, and thus only subset of
     * Jackson annotations will have any effect.
     *<p>
     * Ones that are known to have effect are:
     *<ul>
     * <li>{@link JsonAutoDetect}</li>
     *</ul>
     * 
     * @param cls Class of which class annotations to use
     *   for changing configuration settings
     */
    @Override
    public void fromAnnotations(Class<?> cls)
    {
    	/* no class annotation for:
         *
         * - CAN_OVERRIDE_ACCESS_MODIFIERS
         * - USE_BIG_DECIMAL_FOR_FLOATS
         * - USE_BIG_INTEGER_FOR_INTS
         * - USE_GETTERS_AS_SETTERS
         */

        /* 10-Jul-2009, tatu: Should be able to just pass null as
         *    'MixInResolver'; no mix-ins set at this point
         */
        AnnotationIntrospector ai = getAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.construct(cls, ai, null);
        // visibility checks handled via separate checker object...
        VisibilityChecker<?> prevVc = getDefaultVisibilityChecker();
        _base = _base.withVisibilityChecker(ai.findAutoDetectVisibility(ac, prevVc));
    }

    /**
     * Method that is called to create a non-shared copy of the configuration
     * to be used for a deserialization operation.
     * Note that if sub-classing
     * and sub-class has additional instance methods,
     * this method <b>must</b> be overridden to produce proper sub-class
     * instance.
     */
    @Override
    public DeserializationConfig createUnshared(SubtypeResolver subtypeResolver)
    {
        HashMap<ClassKey,Class<?>> mixins = _mixInAnnotations;
        // ensure that we assume sharing at this point:
        _mixInAnnotationsShared = true;
        return new DeserializationConfig(this, mixins, subtypeResolver);
    }

    /**
     * Method for getting {@link AnnotationIntrospector} configured
     * to introspect annotation values used for configuration.
     */
    @Override
    public AnnotationIntrospector getAnnotationIntrospector()
    {
        /* 29-Jul-2009, tatu: it's now possible to disable use of
         *   annotations; can be done using "no-op" introspector
         */
        if (isEnabled(Feature.USE_ANNOTATIONS)) {
            return super.getAnnotationIntrospector();
        }
        return NopAnnotationIntrospector.instance;
    }
    
    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     *<p>
     * Note: part of {@link MapperConfig} since 1.7
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectClassAnnotations(Class<?> cls) {
        return (T) getClassIntrospector().forClassAnnotations(this, cls, this);
    }

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     *<p>
     * Note: part of {@link MapperConfig} since 1.7
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectDirectClassAnnotations(Class<?> cls) {
        return (T) getClassIntrospector().forDirectClassAnnotations(this, cls, this);
    }

    @Override
    public boolean isAnnotationProcessingEnabled() {
        return isEnabled(Feature.USE_ANNOTATIONS);
    }

    @Override
    public boolean canOverrideAccessModifiers() {
        return isEnabled(Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
    }
    
    /*
    /**********************************************************
    /* Problem handlers
    /**********************************************************
     */

    /**
     * Method for getting head of the problem handler chain. May be null,
     * if no handlers have been added.
     */
    public LinkedNode<DeserializationProblemHandler> getProblemHandlers()
    {
        return _problemHandlers;
    }
    
    /**
     * Method that can be used to add a handler that can (try to)
     * resolve non-fatal deserialization problems.
     */
    public void addHandler(DeserializationProblemHandler h)
    {
        /* Sanity check: let's prevent adding same handler multiple
         * times
         */
        if (!LinkedNode.contains(_problemHandlers, h)) {
            _problemHandlers = new LinkedNode<DeserializationProblemHandler>(h, _problemHandlers);
        }
    }

    /**
     * Method for removing all configuring problem handlers; usually done to replace
     * existing handler(s) with different one(s)
     *
     * @since 1.1
     */
    public void clearHandlers()
    {
        _problemHandlers = null;
    }

    /*
    /**********************************************************
    /* Other configuration
    /**********************************************************
     */

    /**
     * Method called during deserialization if Base64 encoded content
     * needs to be decoded. Default version just returns default Jackson
     * uses, which is modified-mime which does not add linefeeds (because
     * those would have to be escaped in JSON strings).
     */
    public Base64Variant getBase64Variant() {
        return Base64Variants.getDefaultVariant();
    }

    /**
     * @since 1.6
     */
    public final JsonNodeFactory getNodeFactory() {
        return _nodeFactory;
    }
    
    /*
    /**********************************************************
    /* Introspection methods
    /**********************************************************
     */

    /**
     * Method that will introspect full bean properties for the purpose
     * of building a bean deserializer
     *
     * @param type Type of class to be introspected
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspect(JavaType type) {
        return (T) getClassIntrospector().forDeserialization(this, type, this);
    }

    /**
     * Method that will introspect subset of bean properties needed to
     * construct bean instance.
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectForCreation(JavaType type) {
        return (T) getClassIntrospector().forCreation(this, type, this);
    }
    
    /*
    /**********************************************************
    /* Extended API: handler instantiation
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> deserializerInstance(Annotated annotated,
            Class<? extends JsonDeserializer<?>> deserClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            JsonDeserializer<?> deser = hi.deserializerInstance(this, annotated, deserClass);
            if (deser != null) {
                return (JsonDeserializer<Object>) deser;
            }
        }
        return (JsonDeserializer<Object>) ClassUtil.createInstance(deserClass, canOverrideAccessModifiers());
    }

    public KeyDeserializer keyDeserializerInstance(Annotated annotated,
            Class<? extends KeyDeserializer> keyDeserClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            KeyDeserializer keyDeser = hi.keyDeserializerInstance(this, annotated, keyDeserClass);
            if (keyDeser != null) {
                return (KeyDeserializer) keyDeser;
            }
        }
        return (KeyDeserializer) ClassUtil.createInstance(keyDeserClass, canOverrideAccessModifiers());
    }
    
    /*
    /**********************************************************
    /* Deprecated methods
    /**********************************************************
     */

    /**
     * @deprecated Since 1.8 should use variant without arguments
     */
    @Deprecated
    @Override
    public DeserializationConfig createUnshared(TypeResolverBuilder<?> typer,
            VisibilityChecker<?> vc, SubtypeResolver str)
    {
        return createUnshared(str)
            .withTypeResolverBuilder(typer)
            .withVisibilityChecker(vc);
    }
    
    /**
     * @since 1.6
     * 
     * @deprecated Since 1.8 should use {@link #withNodeFactory} instead
     */
    @Deprecated
    public void setNodeFactory(JsonNodeFactory nf) {
        _nodeFactory = nf;
    }


    /**
     * Method for accessing {@link AbstractTypeResolver} configured, if any
     * (no default) used for resolving abstract types into concrete
     * types (either by mapping or materializing new classes).
     * 
     * @deprecated Since 1.8 resolvers should be registered using Module interface
     *    Will be removed from Jackson 2.0
     * 
     * @since 1.6
     */
    @Deprecated
    public AbstractTypeResolver getAbstractTypeResolver() {
        return _abstractTypeResolver;
    }
    
    /**
     * Method for specifying {@link AbstractTypeResolver} to use for resolving
     * references to abstract types into concrete types (if possible).
     * 
     * @since 1.6
     * 
     * @deprecated Since 1.8 resolvers should be registered using Module interface.
     *    Will be removed from Jackson 2.0
     */
    @Deprecated
    public void setAbstractTypeResolver(AbstractTypeResolver atr) {
        _abstractTypeResolver = atr;
    }
    
}
