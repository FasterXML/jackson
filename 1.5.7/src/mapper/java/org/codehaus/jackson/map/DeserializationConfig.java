package org.codehaus.jackson.map;

import java.text.DateFormat;
import java.util.*;

import org.codehaus.jackson.Base64Variant;
import org.codehaus.jackson.Base64Variants;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.util.LinkedNode;
import org.codehaus.jackson.map.util.StdDateFormat;
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
    implements MapperConfig<DeserializationConfig>
{
    /**
     * Enumeration that defines togglable features that guide
     * the serialization feature.
     */
    public enum Feature {
        // // // Introspection configuration

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

        // // // Type conversion configuration

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

        // // // Problem handling

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
         FAIL_ON_UNKNOWN_PROPERTIES(true)

        // // // Structural changes

        /**
         * Feature that can be enabled to handle "wrapped" values
         * (see {@link SerializationConfig.Feature#WRAP_ROOT_VALUE}
         * for details).
         * If enabled, value being deserialized must be a single-property
         * JSON Object where the key name matches expected "root name"
         * (determined using annotation, or if none, fallback which is
         * the unqualified class name of the expected value)
         *<p>
         * Default setting is false, meaning root value is not wrapped.
         *
         * @since 1.3
         */
        ,WRAP_ROOT_VALUE(false)
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

    protected final static DateFormat DEFAULT_DATE_FORMAT = StdDateFormat.instance;

    /*
    /***************************************************
    /* Configuration settings
    /***************************************************
     */

    /**
     * Introspector used to figure out Bean properties needed for bean serialization
     * and deserialization. Overridable so that it is possible to change low-level
     * details of introspection, like adding new annotation types.
     */
    protected ClassIntrospector<? extends BeanDescription> _classIntrospector;

    /**
     * Introspector used for accessing annotation value based configuration.
     */
    protected AnnotationIntrospector _annotationIntrospector;

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
     * Custom date format to use for de-serialization. If specified, will be
     * used instead of {@link org.codehaus.jackson.map.util.StdDateFormat}.
     *<p>
     * Note that the configured format object will be cloned once per
     * deserialization process (first time it is needed)
     */
    protected DateFormat _dateFormat = DEFAULT_DATE_FORMAT;

    /**
     * Mapping that defines how to apply mix-in annotations: key is
     * the type to received additional annotations, and value is the
     * type that has annotations to "mix in".
     *<p>
     * Annotations associated with the value classes will be used to
     * override annotations of the key class, associated with the
     * same field or method. They can be further masked by sub-classes:
     * you can think of it as injecting annotations between the target
     * class and its sub-classes (or interfaces)
     *
     * @since 1.2
     */
    HashMap<ClassKey,Class<?>> _mixInAnnotations;


    /**
     * Flag used to detect when a copy if mix-in annotations is
     * needed: set when current copy is shared, cleared when a
     * fresh copy is maed
     *
     * @since 1.2
     */
    protected boolean _mixInAnnotationsShared;

    /**
     * Type information handler used for "untyped" values (ones declared
     * to have type <code>Object.class</code>)
     * 
     * @since 1.5
     */
    protected final TypeResolverBuilder<?> _typer;

    /**
     * Object used for determining whether specific property elements
     * (method, constructors, fields) can be auto-detected based on
     * their visibility (access modifiers). Can be changed to allow
     * different minimum visibility levels for auto-detection. Note
     * that this is the global handler; individual types (classes)
     * can further override active checker used (using
     * {@link JsonAutoDetect} annotation)
     * 
     * @since 1.5
     */
    protected VisibilityChecker<?> _visibilityChecker;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public DeserializationConfig(ClassIntrospector<? extends BeanDescription> intr,
                               AnnotationIntrospector annIntr, VisibilityChecker<?> vc)
    {
        _classIntrospector = intr;
        _annotationIntrospector = annIntr;
        _typer = null;
        _visibilityChecker = vc;
    }

    protected DeserializationConfig(DeserializationConfig src,
                                    HashMap<ClassKey,Class<?>> mixins,
                                    TypeResolverBuilder<?> typer,
                                    VisibilityChecker<?> vc)
    {
        _classIntrospector = src._classIntrospector;
        _annotationIntrospector = src._annotationIntrospector;
        _featureFlags = src._featureFlags;
        _problemHandlers = src._problemHandlers;
        _dateFormat = src._dateFormat;
        _mixInAnnotations = mixins;
        _typer = typer;
        _visibilityChecker = vc;
    }

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
    //@Override
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
        AnnotatedClass ac = AnnotatedClass.construct(cls, _annotationIntrospector, null);
        // visibility checks handled via separate checker object...
        _visibilityChecker = _annotationIntrospector.findAutoDetectVisibility(ac, _visibilityChecker);
    }

    /**
     * Method that is called to create a non-shared copy of the configuration
     * to be used for a deserialization operation.
     * Note that if sub-classing
     * and sub-class has additional instance methods,
     * this method <b>must</b> be overridden to produce proper sub-class
     * instance.
     */
    //@Override
    public DeserializationConfig createUnshared(TypeResolverBuilder<?> typer,
    		VisibilityChecker<?> vc)

    {
        HashMap<ClassKey,Class<?>> mixins = _mixInAnnotations;
        _mixInAnnotationsShared = true;
    	return new DeserializationConfig(this, mixins, typer, vc);
    }


    //@Override
    public void setIntrospector(ClassIntrospector<? extends BeanDescription> i) {
        _classIntrospector = i;
    }

    /**
     * Method for getting {@link AnnotationIntrospector} configured
     * to introspect annotation values used for configuration.
     */
    //@Override
    public AnnotationIntrospector getAnnotationIntrospector()
    {
        /* 29-Jul-2009, tatu: it's now possible to disable use of
         *   annotations; can be done using "no-op" introspector
         */
        if (isEnabled(Feature.USE_ANNOTATIONS)) {
            return _annotationIntrospector;
        }
        return NopAnnotationIntrospector.instance;
    }

    //@Override
    public void setAnnotationIntrospector(AnnotationIntrospector introspector)
    {
        _annotationIntrospector = introspector;
    }

    /**
     * Method to use for defining mix-in annotations to use for augmenting
     * annotations that deserializable classes have.
     * Mixing in is done when introspecting class annotations and properties.
     * Map passed contains keys that are target classes (ones to augment
     * with new annotation overrides), and values that are source classes
     * (have annotations to use for augmentation).
     * Annotations from source classes (and their supertypes)
     * will <b>override</b>
     * annotations that target classes (and their super-types) have.
     *<p>
     * Note: a copy of argument Map is created; the original Map is
     * not modified or retained by this config object.
     *
     * @since 1.2
     */
    //@Override
    public void setMixInAnnotations(Map<Class<?>, Class<?>> sourceMixins)
    {
        HashMap<ClassKey,Class<?>> mixins = null;
        if (sourceMixins != null && sourceMixins.size() > 0) {
            mixins = new HashMap<ClassKey,Class<?>>(sourceMixins.size());
            for (Map.Entry<Class<?>,Class<?>> en : sourceMixins.entrySet()) {
                mixins.put(new ClassKey(en.getKey()), en.getValue());
            }
        }
        _mixInAnnotationsShared = false;
        _mixInAnnotations = mixins;
    }

    //@Override
    public void addMixInAnnotations(Class<?> target, Class<?> mixinSource)
    {
        if (_mixInAnnotations == null || _mixInAnnotationsShared) {
            _mixInAnnotationsShared = false;
            _mixInAnnotations = new HashMap<ClassKey,Class<?>>();
        }
        _mixInAnnotations.put(new ClassKey(target), mixinSource);
    }

    /**
     * @since 1.2
     */
    //@Override
    public Class<?> findMixInClassFor(Class<?> cls) {
        return (_mixInAnnotations == null) ? null : _mixInAnnotations.get(new ClassKey(cls));
    }

    /*
    /***************************************************
    /* Adding problem handlers
    /***************************************************
     */

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
    /***************************************************
    /* Accessors
    /***************************************************
     */

    /**
     * Method for checking whether given feature is enabled or not
     */
    public final boolean isEnabled(Feature f) {
        return (_featureFlags & f.getMask()) != 0;
    }

    /**
     * Method called during deserialization if Base64 encoded content
     * needs to be decoded. Default version just returns default Jackson
     * uses, which is modified-mime which does not add linefeeds (because
     * those would have to be escaped in Json strings).
     */
    public Base64Variant getBase64Variant() {
        return Base64Variants.getDefaultVariant();
    }

    /**
     * Method for getting head of the problem handler chain. May be null,
     * if no handlers have been added.
     */
    public LinkedNode<DeserializationProblemHandler> getProblemHandlers()
    {
        return _problemHandlers;
    }

    public DateFormat getDateFormat() { return _dateFormat; }

    /**
     * Method that will introspect full bean properties for the purpose
     * of building a bean deserializer
     *
     * @param type Type of class to be introspected
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspect(JavaType type) {
        return (T) _classIntrospector.forDeserialization(this, type, this);
    }

    /**
     * Method that will introspect subset of bean properties needed to
     * construct bean instance.
     */
    @SuppressWarnings("unchecked")
	public <T extends BeanDescription> T introspectForCreation(Class<?> cls) {
        return (T) _classIntrospector.forCreation(this, cls, this);
    }

    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     */
    @SuppressWarnings("unchecked")
	public <T extends BeanDescription> T introspectClassAnnotations(Class<?> cls) {
        return (T) _classIntrospector.forClassAnnotations(this, cls, this);
    }

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectDirectClassAnnotations(Class<?> cls) {
        return (T) _classIntrospector.forDirectClassAnnotations(this, cls, this);
    }
    
    //@Override
    public TypeResolverBuilder<?> getDefaultTyper(JavaType baseType) {
        return _typer;
    }

    //@Override
    public VisibilityChecker<?> getDefaultVisibilityChecker() {
    	return _visibilityChecker;
    }
    
    /*
    /***************************************************
    /* Configuration: on/off features
    /***************************************************
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

    //protected int getFeatures() { return _generatorFeatures; }

    /*
    /***************************************************
    /* Configuration: other
    /***************************************************
     */

    /**
     * Method that will set the textual deserialization to use for
     * deserializing Dates (and Calendars). If null is passed, will
     * use {@link StdDateFormat}.
     */
    public void setDateFormat(DateFormat df) {
        _dateFormat = (df == null) ? StdDateFormat.instance : df;
    }
}
