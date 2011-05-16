package org.codehaus.jackson.map;

import java.text.DateFormat;
import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion; // for javadocs
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.SubtypeResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.ser.FilterProvider;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.ClassUtil;
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
    extends MapperConfig<SerializationConfig>
{
    /**
     * Enumeration that defines togglable features that guide
     * the serialization feature.
     */
    public enum Feature {
        /*
        /******************************************************
        /*  Introspection features
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

        /**
         * Feature that defines default property serialization order:
         * if enabled, default ordering is alphabetic (similar to
         * how {@link org.codehaus.jackson.annotate.JsonPropertyOrder#alphabetic()}
         * works); if disabled, order is unspecified (based on what JDK gives
         * us, which may be declaration order, but not guaranteed).
         *<p>
         * Note that this is just the default behavior, and can be overridden by
         * explicit overrides in classes.
         *
         * @since 1.8
         */
        SORT_PROPERTIES_ALPHABETICALLY(false),
        
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

    protected int _featureFlags = DEFAULT_FEATURE_FLAGS;

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
     * Object used for resolving filter ids to filter instances.
     * Non-null if explicitly defined; null by default.
     * 
     * @since 1.7
     */
    protected FilterProvider _filterProvider;
    
    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    /**
     * Constructor used by ObjectMapper to create default configuration object instance.
     */
    public SerializationConfig(ClassIntrospector<? extends BeanDescription> intr,
            AnnotationIntrospector annIntr, VisibilityChecker<?> vc,
            SubtypeResolver subtypeResolver, PropertyNamingStrategy propertyNamingStrategy,
            TypeFactory typeFactory, HandlerInstantiator handlerInstantiator)
    {
        super(intr, annIntr, vc, subtypeResolver, propertyNamingStrategy, typeFactory, handlerInstantiator);
        _filterProvider = null;
    }

    /**
     * @since 1.8
     */
    protected SerializationConfig(SerializationConfig src) {
        this(src, src._base);
    }

    /**
     * Constructor used to make a private copy of specific mix-in definitions.
     * 
     * @since 1.8
     */
    protected SerializationConfig(SerializationConfig src,
            HashMap<ClassKey,Class<?>> mixins, SubtypeResolver str)
    {
        this(src, src._base);
        _mixInAnnotations = mixins;
        _subtypeResolver = str;
    }
    
    /**
     * @since 1.8
     */
    protected SerializationConfig(SerializationConfig src, MapperConfig.Base base)
    {
        super(src, base, src._subtypeResolver);
        _featureFlags = src._featureFlags;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = src._filterProvider;
    }

    /**
     * @since 1.8
     */
    protected SerializationConfig(SerializationConfig src, FilterProvider filters)
    {
        super(src);
        _featureFlags = src._featureFlags;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = src._serializationView;
        _filterProvider = filters;
    }

    /**
     * @since 1.8
     */
    protected SerializationConfig(SerializationConfig src, Class<?> view)
    {
        super(src);
        _featureFlags = src._featureFlags;
        _serializationInclusion = src._serializationInclusion;
        _serializationView = view;
        _filterProvider = src._filterProvider;
    }
    
    /*
    /**********************************************************
    /* Life-cycle, factory methods from MapperConfig
    /**********************************************************
     */

    @Override
    public SerializationConfig withClassIntrospector(ClassIntrospector<? extends BeanDescription> ci) {
        return new SerializationConfig(this, _base.withClassIntrospector(ci));
    }

    @Override
    public SerializationConfig withAnnotationIntrospector(AnnotationIntrospector ai) {
        return new SerializationConfig(this, _base.withAnnotationIntrospector(ai));
    }

    @Override
    public SerializationConfig withVisibilityChecker(VisibilityChecker<?> vc) {
        return new SerializationConfig(this, _base.withVisibilityChecker(vc));
    }

    @Override
    public SerializationConfig withTypeResolverBuilder(TypeResolverBuilder<?> trb) {
        return new SerializationConfig(this, _base.withTypeResolverBuilder(trb));
    }

    @Override
    public SerializationConfig withSubtypeResolver(SubtypeResolver str) {
        SerializationConfig cfg =  new SerializationConfig(this);
        cfg._subtypeResolver = str;
        return cfg;
    }
    
    @Override
    public SerializationConfig withPropertyNamingStrategy(PropertyNamingStrategy pns) {
        return new SerializationConfig(this, _base.withPropertyNamingStrategy(pns));
    }
    
    @Override
    public SerializationConfig withTypeFactory(TypeFactory tf) {
        return new SerializationConfig(this, _base.withTypeFactory(tf));
    }

    /**
     * In addition to constructing instance with specified date format,
     * will enable or disable <code>Feature.WRITE_DATES_AS_TIMESTAMPS</code>
     * (enable if format set as null; disable if non-null)
     */
    @Override
    public SerializationConfig withDateFormat(DateFormat df) {
        SerializationConfig cfg =  new SerializationConfig(this, _base.withDateFormat(df));
        // And also 
        cfg.set(Feature.WRITE_DATES_AS_TIMESTAMPS, (df == null));
        return cfg;
    }
    
    @Override
    public SerializationConfig withHandlerInstantiator(HandlerInstantiator hi) {
        return new SerializationConfig(this, _base.withHandlerInstantiator(hi));
    }
        
    /*
    /**********************************************************
    /* Life-cycle, SerializationConfig specific factory methods
    /**********************************************************
     */
    
    /**
     * @since 1.7
     */
    public SerializationConfig withFilters(FilterProvider filterProvider) {
        return new SerializationConfig(this, filterProvider);
    }

    /**
     * @since 1.8
     */
    public SerializationConfig withView(Class<?> view) {
        return new SerializationConfig(this, view);
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
    @Override
    public void fromAnnotations(Class<?> cls)
    {
        /* 10-Jul-2009, tatu: Should be able to just pass null as
         *    'MixInResolver'; no mix-ins set at this point
         * 29-Jul-2009, tatu: Also, we do NOT ignore annotations here, even
         *    if Feature.USE_ANNOTATIONS was disabled, since caller
         *    specifically requested annotations to be added with this call
         */
        AnnotationIntrospector ai = getAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.construct(cls, ai, null);
        _base = _base.withVisibilityChecker(ai.findAutoDetectVisibility(ac,
                getDefaultVisibilityChecker()));

        // How about writing null property values?
        JsonSerialize.Inclusion incl = ai.findSerializationInclusion(ac, null);
        if (incl != _serializationInclusion) {
            setSerializationInclusion(incl);
    	}

        JsonSerialize.Typing typing = ai.findSerializationTyping(ac);
        if (typing != null) {
            set(Feature.USE_STATIC_TYPING, (typing == JsonSerialize.Typing.STATIC));
        }
    }

    @Override
    public SerializationConfig createUnshared(SubtypeResolver subtypeResolver)
    {
        HashMap<ClassKey,Class<?>> mixins = _mixInAnnotations;
        _mixInAnnotationsShared = true;
        return new SerializationConfig(this, mixins, subtypeResolver);
    }
    
    @Override
    public AnnotationIntrospector getAnnotationIntrospector()
    {
        /* 29-Jul-2009, tatu: it's now possible to disable use of
         *   annotations; can be done using "no-op" introspector
         */
        if (isEnabled(Feature.USE_ANNOTATIONS)) {
            return super.getAnnotationIntrospector();
        }
        return AnnotationIntrospector.nopInstance();
    }

    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     *<p>
     * Note: part of {@link MapperConfig} since 1.7
     */
    @SuppressWarnings("unchecked")
    @Override
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
    @SuppressWarnings("unchecked")
    @Override
    public <T extends BeanDescription> T introspectDirectClassAnnotations(Class<?> cls) {
        return (T) getClassIntrospector().forDirectClassAnnotations(this, cls, this);
    }

    @Override
    public boolean isAnnotationProcessingEnabled() {
        return isEnabled(SerializationConfig.Feature.USE_ANNOTATIONS);
    }
    
    @Override
    public boolean canOverrideAccessModifiers() {
        return isEnabled(Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
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
    /* Introspection methods
    /**********************************************************
     */

    /**
     * Method that will introspect full bean properties for the purpose
     * of building a bean serializer
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspect(JavaType type) {
        return (T) getClassIntrospector().forSerialization(this, type, this);
    }

    /*
    /**********************************************************
    /* Extended API: serializer instantiation
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> serializerInstance(Annotated annotated, Class<? extends JsonSerializer<?>> serClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            JsonSerializer<?> ser = hi.serializerInstance(this, annotated, serClass);
            if (ser != null) {
                return (JsonSerializer<Object>) ser;
            }
        }
        return (JsonSerializer<Object>) ClassUtil.createInstance(serClass, canOverrideAccessModifiers());
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
    public SerializationConfig createUnshared(TypeResolverBuilder<?> typer,
            VisibilityChecker<?> vc, SubtypeResolver str)
    {
        return createUnshared(str)
            .withTypeResolverBuilder(typer)
            .withVisibilityChecker(vc);
    }
    
    /**
     * One thing to note is that this will set {@link Feature#WRITE_DATES_AS_TIMESTAMPS}
     * to false (if null format set), or true (if non-null format)
     * 
     * @deprecated Since 1.8, use {@link #withDateFormat} instead.
     */
    @Override
    @Deprecated
    public final void setDateFormat(DateFormat df) {
        super.setDateFormat(df);
        set(Feature.WRITE_DATES_AS_TIMESTAMPS, (df == null));
    }
    
    /**
     * Method for checking which serialization view is being used,
     * if any; null if none.
     *
     * @since 1.4
     * 
     * @deprecated Since 1.8, use {@link #withView} instead
     */
    @Deprecated
    public void setSerializationView(Class<?> view)
    {
        _serializationView = view;
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
