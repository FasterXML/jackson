package org.codehaus.jackson.map;

import java.text.DateFormat;
import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion; // for javadocs
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.SubtypeResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.jsontype.impl.StdSubtypeResolver;
import org.codehaus.jackson.map.ser.FilterProvider;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.util.StdDateFormat;
import org.codehaus.jackson.type.JavaType;

/**
 * Object that contains baseline configuration for serialization
 * process. An instance is owned by {@link ObjectMapper}, which makes
 * a copy that is passed during serialization process to
 * {@link SerializerProvider} and {@link SerializerFactory}.
 *<p>
 * Note: although configuration settings can be changed at any time
 * (for factories and instances), they are not guaranteed to have
 * effect if called after constructing relevant mapper or serializer
 * instance. This because some objects may be configured, constructed and
 * cached first time they are needed.
 */
public class SerializationConfig
    implements MapperConfig<SerializationConfig>
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
         * Feature that determines whether regualr "getter" methods are
         * automatically detected based on standard Bean naming convention
         * or not. If yes, then all public zero-argument methods that
         * start with prefix "get" 
         * are considered as getters.
         * If disabled, only methods explicitly  annotated are considered getters.
         *<p>
         * Note that since version 1.3, this does <b>NOT</b> include
         * "is getters" (see {@link #AUTO_DETECT_IS_GETTERS} for details)
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_GETTERS(true),

        /**
         * Feature that determines whether "is getter" methods are
         * automatically detected based on standard Bean naming convention
         * or not. If yes, then all public zero-argument methods that
         * start with prefix "is", and whose return type is boolean
         * are considered as "is getters".
         * If disabled, only methods explicitly annotated are considered getters.
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_IS_GETTERS(true),

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
         * Generic output features
        /******************************************************
         */

        /**
         * Feature that determines the default settings of whether Bean
         * properties with null values are to be written out.
         *<p>
         * Feature is enabled by default (null properties written).
         *<p>
         * Note too that there is annotation
         * {@link org.codehaus.jackson.annotate.JsonWriteNullProperties}
         * that can be used for more granular control (annotates bean
         * classes or individual property access methods).
         *
         * @deprecated As of 1.1, use {@link SerializationConfig#setSerializationInclusion}
         *    instead
         */
        @Deprecated
        WRITE_NULL_PROPERTIES(true),

        /**
         * Feature that determines whether the type detection for
         * serialization should be using actual dynamic runtime type,
         * or declared static type.
         * Default value is false, to use dynamic runtime type.
         *<p>
         * This global default value can be overridden at class, method
         * or field level by using {@link JsonSerialize#typing} annotation
         * property
         */
        USE_STATIC_TYPING(false),

        /**
         * Feature that determines whether properties that have no view
         * annotations are included in JSON serialization views (see
         * {@link org.codehaus.jackson.map.annotate.JsonView} for more
         * details on JSON Views).
         * If enabled, non-annotated properties will be included;
         * when disabled, they will be excluded. So this feature
         * changes between "opt-in" (feature disabled) and
         * "opt-out" (feature enabled) modes.
         *<p>
         * Default value is enabled, meaning that non-annotated
         * properties are included in all views if there is no
         * {@link org.codehaus.jackson.map.annotate.JsonView} annotation.
         * 
         * @since 1.5
         */
        DEFAULT_VIEW_INCLUSION(true),
        
        /**
         * Feature that can be enabled to make root value (usually JSON
         * Object but can be any type) wrapped within a single property
         * JSON object, where key as the "root name", as determined by
         * annotation introspector (esp. for JAXB that uses
         * <code>@XmlRootElement.name</code>) or fallback (non-qualified
         * class name).
         * Feature is mostly intended for JAXB compatibility.
         *<p>
         * Default setting is false, meaning root value is not wrapped.
         *
         * @since 1.7
         */
        WRAP_ROOT_VALUE(false),

        /**
         * Feature that allows enabling (or disabling) indentation
         * for the underlying generator, using the default pretty
         * printer (see
         * {@link org.codehaus.jackson.JsonGenerator#useDefaultPrettyPrinter}
         * for details).
         *<p>
         * Note that this only affects cases where
         * {@link org.codehaus.jackson.JsonGenerator}
         * is constructed implicitly by ObjectMapper: if explicit
         * generator is passed, its configuration is not changed.
         *<p>
         * Also note that if you want to configure details of indentation,
         * you need to directly configure the generator: there is a
         * method to use any <code>PrettyPrinter</code> instance.
         * This feature will only allow using the default implementation.
         */
        INDENT_OUTPUT(false),
        
        /*
        /******************************************************
         *  Error handling features
        /******************************************************
         */
        
        /**
         * Feature that determines what happens when no accessors are
         * found for a type (and there are no annotations to indicate
         * it is meant to be serialized). If enabled (default), an
         * exception is thrown to indicate these as non-serializable
         * types; if disabled, they are serialized as empty Objects,
         * i.e. without any properties.
         *<p>
         * Note that empty types that this feature has only effect on
         * those "empty" beans that do not have any recognized annotations
         * (like <code>@JsonSerialize</code>): ones that do have annotations
         * do not result in an exception being thrown.
         *
         * @since 1.4
         */
        FAIL_ON_EMPTY_BEANS(true),

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
         * Output life cycle features
        /******************************************************
         */
        
         /**
          * Feature that determines whether <code>close</code> method of
          * serialized <b>root level</b> objects (ones for which <code>ObjectMapper</code>'s
          * writeValue() (or equivalent) method is called)
          * that implement {@link java.io.Closeable} 
          * is called after serialization or not. If enabled, <b>close()</b> will
          * be called after serialization completes (whether succesfully, or
          * due to an error manifested by an exception being thrown). You can
          * think of this as sort of "finally" processing.
          *<p>
          * NOTE: only affects behavior with <b>root</b> objects, and not other
          * objects reachable from the root object. Put another way, only one
          * call will be made for each 'writeValue' call.
          * 
          * @since 1.6 (see [JACKSON-282 for details])
          */
        CLOSE_CLOSEABLE(false),

        /**
         * Feature that determines whether <code>JsonGenerator.flush()</code> is
         * called after <code>writeValue()</code> method <b>that takes JsonParser
         * as an argument</b> completes or not (i.e. does NOT affect methods
         * that use other destinations); same for methods in {@link ObjectWriter}.
         * This usually makes sense; but there are cases where flushing
         * should not be forced: for example when underlying stream is
         * compressing and flush() causes compression state to be flushed
         * (which occurs with some compression codecs).
          * 
          * @since 1.6 (see [JACKSON-401 for details])
         */
        FLUSH_AFTER_WRITE_VALUE(true),
         
        /*
        /******************************************************
         * Datatype-specific serialization configuration
        /******************************************************
         */

        /**
         * Feature that determines whether {@link java.util.Date}s
         * (and Date-based things like {@link java.util.Calendar}s) are to be
         * serialized as numeric timestamps (true; the default),
         * or as something else (usually textual representation).
         * If textual representation is used, the actual format is
         * one returned by a call to {@link #getDateFormat}.
         *<p>
         * Note: whether this feature affects handling of other date-related
         * types depend on handlers of those types.
         */
        WRITE_DATES_AS_TIMESTAMPS(true),

        /**
         * Feature that determines how type <code>char[]</code> is serialized:
         * when enabled, will be serialized as an explict JSON array (with
         * single-character Strings as values); when disabled, defaults to
         * serializing them as Strings (which is more compact).
         * 
         * @since 1.6 (see [JACKSON-289 for details])
         */
        WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS(false),

        /**
         * Feature that determines standard serialization mechanism used for
         * Enum values: if enabled, return value of <code>Enum.toString()</code>
         * is used; if disabled, return value of <code>Enum.name()</code> is used.
         * Since pre-1.6 method was to use Enum name, this is the default.
         *<p>
         * Note: this feature should usually have same value
         * as {@link DeserializationConfig.Feature#READ_ENUMS_USING_TO_STRING}.
         *<p>
         * For further details, check out [JACKSON-212]
         * 
         * @since 1.6
         */
        WRITE_ENUMS_USING_TO_STRING(false),

        /**
         * Feature that determines whether Map entries with null values are
         * to be serialized (true) or not (false).
         *<p>
         * For further details, check out [JACKSON-314]
         * 
         * @since 1.6
         */
        WRITE_NULL_MAP_VALUES(true)           

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
    /* Configuration settings
    /**********************************************************
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

    protected int _featureFlags = DEFAULT_FEATURE_FLAGS;

    /**
     * Textual date format to use for serialization (if enabled by
     * {@link Feature#WRITE_DATES_AS_TIMESTAMPS} being set to false).
     * Defaults to a ISO-8601 compliant format used by
     * {@link StdDateFormat}.
     *<p>
     * Note that format object is <b>not to be used as is</b> by caller:
     * since date format objects are not thread-safe, caller has to
     * create a clone first.
     */
    protected DateFormat _dateFormat = StdDateFormat.instance;

    /**
     * Which Bean/Map properties are to be included in serialization?
     * Default settings is to include all regardless of value; can be
     * changed to only include non-null properties, or properties
     * with non-default values.
     *<p>
     * Defaults to null for backwards compatibility; if left as null,
     * will check
     * deprecated {@link Feature#WRITE_NULL_PROPERTIES}
     * to choose between {@link Inclusion#ALWAYS}
     * and {@link Inclusion#NON_NULL}.
     */
    protected JsonSerialize.Inclusion _serializationInclusion = null;

    /**
     * View to use for filtering out properties to serialize.
     * Null if none (will also be assigned null if <code>Object.class</code>
     * is defined), meaning that all properties are to be included.
     */
    protected Class<?> _serializationView;

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
    protected HashMap<ClassKey,Class<?>> _mixInAnnotations;

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

    /**
     * Registered concrete subtypes that can be used instead of (or
     * in addition to) ones declared using annotations.
     * 
     * @since 1.6
     */
    protected SubtypeResolver _subtypeResolver;

    /**
     * Object used for resolving filter ids to filter instances.
     * Non-null if explicitly defined; null by default.
     * 
     * @since 1.7
     */
    protected FilterProvider _filterProvider;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public SerializationConfig(ClassIntrospector<? extends BeanDescription> intr,
            AnnotationIntrospector annIntr, VisibilityChecker<?> vc,
            SubtypeResolver subtypeResolver)
    {
        _classIntrospector = intr;
        _annotationIntrospector = annIntr;
        _typer = null;
        _visibilityChecker = vc;
        _subtypeResolver = subtypeResolver;
        _filterProvider = null;
    }

    /**
     * @since 1.7
     */
    protected SerializationConfig(SerializationConfig src,
            HashMap<ClassKey,Class<?>> mixins,
            TypeResolverBuilder<?> typer,
            VisibilityChecker<?> vc,
            SubtypeResolver subtypeResolver,
            FilterProvider filterProvider)
    {
        _classIntrospector = src._classIntrospector;
        _annotationIntrospector = src._annotationIntrospector;
        _featureFlags = src._featureFlags;
        _dateFormat = src._dateFormat;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _mixInAnnotations = mixins;
        _typer = typer;
        _visibilityChecker = vc;
        _subtypeResolver = subtypeResolver;
        _filterProvider = filterProvider;
    }

    /**
     * Copy constructor used for creating a new instance that is same as
     * this one, but with different <code>FilterProvider</code>.
     * 
     * @since 1.7
     */
    protected SerializationConfig(SerializationConfig src, FilterProvider filterProvider)
    {
        _classIntrospector = src._classIntrospector;
        _annotationIntrospector = src._annotationIntrospector;
        _featureFlags = src._featureFlags;
        _dateFormat = src._dateFormat;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _mixInAnnotations = src._mixInAnnotations;
        _typer = src._typer;
        _visibilityChecker = src._visibilityChecker;
        _subtypeResolver = src._subtypeResolver;
        _filterProvider = filterProvider;
    }
    
    public SerializationConfig withFilters(FilterProvider filterProvider) {
        return new SerializationConfig(this, filterProvider);
    }
    
    /**
     * SerializationConfig-specific version for constructing unshared
     * configuration object.
     * 
     * @since 1.7
     */
    public SerializationConfig createUnshared(TypeResolverBuilder<?> typer,
                VisibilityChecker<?> vc, SubtypeResolver subtypeResolver,
                FilterProvider filterProvider)
    {
        HashMap<ClassKey,Class<?>> mixins = _mixInAnnotations;
        _mixInAnnotationsShared = true;
        return new SerializationConfig(this, mixins, typer, vc, subtypeResolver, filterProvider);
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
     * Serialization annotations that are known to have effect are:
     *<ul>
     * <li>{@link JsonWriteNullProperties}</li>
     * <li>{@link JsonAutoDetect}</li>
     * <li>{@link JsonSerialize#typing}</li>
     *</ul>
     * 
     * @param cls Class of which class annotations to use
     *   for changing configuration settings
     */
    //@Override
    public void fromAnnotations(Class<?> cls)
    {
        /* 10-Jul-2009, tatu: Should be able to just pass null as
         *    'MixInResolver'; no mix-ins set at this point
         * 29-Jul-2009, tatu: Also, we do NOT ignore annotations here, even
         *    if Feature.USE_ANNOTATIONS was disabled, since caller
         *    specifically requested annotations to be added with this call
         */
        AnnotatedClass ac = AnnotatedClass.construct(cls, _annotationIntrospector, null);
        _visibilityChecker = _annotationIntrospector.findAutoDetectVisibility(ac, _visibilityChecker);

        // How about writing null property values?
        JsonSerialize.Inclusion incl = _annotationIntrospector.findSerializationInclusion(ac, null);
        if (incl != _serializationInclusion) {
            setSerializationInclusion(incl);
    	}

        JsonSerialize.Typing typing = _annotationIntrospector.findSerializationTyping(ac);
        if (typing != null) {
            set(Feature.USE_STATIC_TYPING, (typing == JsonSerialize.Typing.STATIC));
        }
    }

    /**
     * Method that is called to create a non-shared copy of the configuration
     * to be used for a serialization operation.
     * Note that if sub-classing
     * and sub-class has additional instance methods,
     * this method <b>must</b> be overridden to produce proper sub-class
     * instance.
     */
    //@Override
    public SerializationConfig createUnshared(TypeResolverBuilder<?> typer,
            VisibilityChecker<?> vc, SubtypeResolver subtypeResolver)
    {
        HashMap<ClassKey,Class<?>> mixins = _mixInAnnotations;
        _mixInAnnotationsShared = true;
        return new SerializationConfig(this, mixins, typer, vc, subtypeResolver, null);
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
        return AnnotationIntrospector.nopInstance();
    }

    //@Override
    public void setAnnotationIntrospector(AnnotationIntrospector ai) {
        _annotationIntrospector = ai;
    }

    //@Override
    public void insertAnnotationIntrospector(AnnotationIntrospector introspector)
    {
        _annotationIntrospector = AnnotationIntrospector.Pair.create(introspector, _annotationIntrospector);
    }

    //@Override
    public void appendAnnotationIntrospector(AnnotationIntrospector introspector)
    {
        _annotationIntrospector = AnnotationIntrospector.Pair.create(_annotationIntrospector, introspector);
    }
    
    /**
     * Method to use for defining mix-in annotations to use for augmenting
     * annotations that serializable classes have.
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

    //@Override
    public DateFormat getDateFormat() { return _dateFormat; }

    /**
     * Method that will set the specific date format to use for
     * serializing Dates (and Calendars); or if null passed, simply
     * disable textual serialization and use timestamp.
     * In addition to setting format, will also enable/disable feature
     * {@link Feature#WRITE_DATES_AS_TIMESTAMPS}: enable, if argument
     * is null; disable if non-null.
     */
    //@Override
    public void setDateFormat(DateFormat df) {
        _dateFormat = df;
        // Also: enable/disable usage of 
        set(Feature.WRITE_DATES_AS_TIMESTAMPS, (df == null));
    }

    //@Override
    public TypeResolverBuilder<?> getDefaultTyper(JavaType baseType) {
        return _typer;
    }

    //@Override
    public VisibilityChecker<?> getDefaultVisibilityChecker() {
        return _visibilityChecker;
    }

    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     *<p>
     * Note: part of {@link MapperConfig} since 1.7
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectClassAnnotations(Class<?> cls) {
        return (T) _classIntrospector.forClassAnnotations(this, cls, this);
    }

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     *<p>
     * Note: part of {@link MapperConfig} since 1.7
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectDirectClassAnnotations(Class<?> cls) {
        return (T) _classIntrospector.forDirectClassAnnotations(this, cls, this);
    }
    
    /*
    /**********************************************************
    /* Configuration: on/off features
    /**********************************************************
     */

    /**
     * Method for enabling specified feature.
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
    
    /**
     * Method for checking whether given feature is enabled or not
     */
    public final boolean isEnabled(Feature f) {
        return (_featureFlags & f.getMask()) != 0;
    }
    
    /*
    /**********************************************************
    /* Introspection methods
    /**********************************************************
     */

    /**
     * Method that will introspect full bean properties for the purpose
     * of building a bean serializer
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspect(JavaType type) {
        return (T) _classIntrospector.forSerialization(this, type, this);
    }
    
    /*
    /**********************************************************
    /* Polymorphic type handling configuration
    /**********************************************************
     */
    
    /**
     * @since 1.6
     */
    public SubtypeResolver getSubtypeResolver() {
        if (_subtypeResolver == null) {
            _subtypeResolver = new StdSubtypeResolver();
        }
        return _subtypeResolver;
    }

    /**
     * @since 1.6
     */
    public void setSubtypeResolver(SubtypeResolver r) {
        _subtypeResolver = r;
    }
    
    /*
    /**********************************************************
    /* Configuration: other
    /**********************************************************
     */

    /**
     * Method for checking which serialization view is being used,
     * if any; null if none.
     *
     * @since 1.4
     */
    public Class<?> getSerializationView() { return _serializationView; }

    public JsonSerialize.Inclusion getSerializationInclusion()
    {
        if (_serializationInclusion != null) {
            return _serializationInclusion;
        }
        return isEnabled(Feature.WRITE_NULL_PROPERTIES) ?
            JsonSerialize.Inclusion.ALWAYS : JsonSerialize.Inclusion.NON_NULL;
    }
    
    /**
     * Method that will define global setting of which
     * bean/map properties are to be included in serialization.
     * Can be overridden by class annotations (overriding
     * settings to use for instances of that class) and
     * method/field annotations (overriding settings for the value
     * bean for that getter method or field)
     */
    public void setSerializationInclusion(JsonSerialize.Inclusion props)
    {
        _serializationInclusion = props;
        // And for some level of backwards compatibility, also...
        if (props == JsonSerialize.Inclusion.NON_NULL) {
            disable(Feature.WRITE_NULL_PROPERTIES);
        } else {
            enable(Feature.WRITE_NULL_PROPERTIES);
        }
    }

    /**
     * Method for checking which serialization view is being used,
     * if any; null if none.
     *
     * @since 1.4
     */
    public void setSerializationView(Class<?> view)
    {
        _serializationView = view;
    }
    
    /**
     * Method for getting provider used for locating filters given
     * id (which is usually provided with filter annotations).
     * Will be null if no provided was set for {@link ObjectWriter}
     * (or if serialization directly called from {@link ObjectMapper})
     * 
     * @since 1.7
     */
    public FilterProvider getFilterProvider() {
        return _filterProvider;
    }
    
    /*
    /**********************************************************
    /* Debug support
    /**********************************************************
     */

    @Override public String toString()
    {
        return "[SerializationConfig: flags=0x"+Integer.toHexString(_featureFlags)+"]";
    }
}
