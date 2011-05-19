package org.codehaus.jackson.map;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.SubtypeResolver;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.jsontype.impl.StdSubtypeResolver;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.map.util.StdDateFormat;
import org.codehaus.jackson.type.JavaType;

/**
 * Interface that defines functionality accessible through both
 * serialization and deserialization configuration objects;
 * accessors to mode-independent configuration settings
 * and such.
 *
 * @since 1.2 -- major change in 1.8, changed from interface to
 *   abstract class
 */
public abstract class MapperConfig<T extends MapperConfig<T>>
    implements ClassIntrospector.MixInResolver
{
    /*
    /**********************************************************
    /* Constants, default values
    /**********************************************************
     */

    /**
     * This is the default {@link DateFormat} used unless overridden by
     * custom implementation.
     */
    protected final static DateFormat DEFAULT_DATE_FORMAT = StdDateFormat.instance;

    /*
    /**********************************************************
    /* Simple immutable basic settings
    /**********************************************************
     */

    /**
     * Immutable container object for simple configuration settings.
     *<p>
     * Note: ideally this would be final, but until we can eliminate
     * mutators, must keep it mutable.
     */
    protected Base _base;
    
    /*
    /**********************************************************
    /* Mix-in annotations
    /**********************************************************
     */
    
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
     * fresh copy is made
     *
     * @since 1.2
     */
    protected boolean _mixInAnnotationsShared;

    /*
    /**********************************************************
    /* "Late bound" settings
    /**********************************************************
     */

    /**
     * Registered concrete subtypes that can be used instead of (or
     * in addition to) ones declared using annotations.
     * Unlike most other settings, it is not configured as early
     * as it is set, but rather only when a non-shared instance
     * is constructed by <code>ObjectMapper</code> (or -Reader
     * or -Writer)
     *<p>
     * Note: this is the only property left as non-final, to allow
     * lazy construction of the instance as necessary.
     * 
     * @since 1.6
     */
    protected SubtypeResolver _subtypeResolver;
    
    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    protected MapperConfig(ClassIntrospector<? extends BeanDescription> ci, AnnotationIntrospector ai,
            VisibilityChecker<?> vc, SubtypeResolver str, PropertyNamingStrategy pns, TypeFactory tf,
            HandlerInstantiator hi)
    {
        _base = new Base(ci, ai, vc, pns, tf, null, DEFAULT_DATE_FORMAT, hi);
        _subtypeResolver = str;
        // by default, assumed to be shared; only cleared when explicit copy is made
        _mixInAnnotationsShared = true;
    }

    /**
     * Simple copy constructor
     * 
     * @since 1.8
     */
    protected MapperConfig(MapperConfig<?> src) {
        this(src, src._base, src._subtypeResolver);
    }

    /**
     * @since 1.8
     */
    protected MapperConfig(MapperConfig<?> src, MapperConfig.Base base, SubtypeResolver str)
    {
        _base = base;
        _subtypeResolver = str;
        // by default, assumed to be shared; only cleared when explicit copy is made
        _mixInAnnotationsShared = true;
        _mixInAnnotations = src._mixInAnnotations;
    }
    
    /*
    /**********************************************************
    /* Life-cycle: factory methods
    /**********************************************************
     */
    
    /**
     * Method that checks class annotations that the argument Object has,
     * and modifies settings of this configuration object accordingly,
     * similar to how those annotations would affect actual value classes
     * annotated with them, but with global scope. Note that not all
     * annotations have global significance, and thus only subset of
     * Jackson annotations will have any effect.
     */
    public abstract void fromAnnotations(Class<?> cls);

    /**
     * Method to use for constructing an instance that is not shared
     * between multiple operations but only used for a single one
     * (which may be this instance, if it is immutable; if not, a copy
     * is constructed with same settings)
     * 
     * @since 1.8
     */
    public abstract T createUnshared(SubtypeResolver subtypeResolver);

    /**
     * Method for constructing and returning a new instance with different
     * {@link ClassIntrospector}
     * to use.
     * 
     * @since 1.8
     */
    public abstract T withClassIntrospector(ClassIntrospector<? extends BeanDescription> ci);

    /**
     * Method for constructing and returning a new instance with different
     * {@link AnnotationIntrospector} 
     * to use (replacing old one).
     * 
     * @since 1.8
     */
    public abstract T withAnnotationIntrospector(AnnotationIntrospector ai);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link VisibilityChecker}
     * to use.
     * 
     * @since 1.8
     */
    public abstract T withVisibilityChecker(VisibilityChecker<?> vc);

    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeResolverBuilder}
     * to use.
     * 
     * @since 1.8
     */
    public abstract T withTypeResolverBuilder(TypeResolverBuilder<?> trb);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link SubtypeResolver}
     * to use.
     * 
     * @since 1.8
     */
    public abstract T withSubtypeResolver(SubtypeResolver str);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link PropertyNamingStrategy}
     * to use.
     * 
     * @since 1.8
     */
    public abstract T withPropertyNamingStrategy(PropertyNamingStrategy strategy);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link TypeFactory}
     * to use.
     * 
     * @since 1.8
     */
    public abstract T withTypeFactory(TypeFactory typeFactory);
    
    /**
     * Method for constructing and returning a new instance with different
     * {@link DateFormat}
     * to use.
     * 
     * @since 1.8
     */
    public abstract T withDateFormat(DateFormat df);

    /**
     * Method for constructing and returning a new instance with different
     * {@link HandlerInstantiator}
     * to use.
     * 
     * @since 1.8
     */
    public abstract T withHandlerInstantiator(HandlerInstantiator hi);
    
    /*
    /**********************************************************
    /* Configuration: introspectors, mix-ins
    /**********************************************************
     */
    
    public ClassIntrospector<? extends BeanDescription> getClassIntrospector() {
        return _base.getClassIntrospector();
    }

    /**
     * Method for getting {@link AnnotationIntrospector} configured
     * to introspect annotation values used for configuration.
     *<p>
     * Non-final since it is actually overridden by sub-classes (for now?)
     */
    public AnnotationIntrospector getAnnotationIntrospector() {
        return _base.getAnnotationIntrospector();
    }

    /**
     * Method for registering specified {@link AnnotationIntrospector} as the highest
     * priority introspector (will be chained with existing introspector(s) which
     * will be used as fallbacks for cases this introspector does not handle)
     * 
     * @param introspector Annotation introspector to register.
     * 
     * @since 1.7
     */
    public final void insertAnnotationIntrospector(AnnotationIntrospector introspector) {
        _base = _base.withAnnotationIntrospector(AnnotationIntrospector.Pair.create(introspector,
                getAnnotationIntrospector()));
    }

    /**
     * Method for registering specified {@link AnnotationIntrospector} as the lowest
     * priority introspector, chained with existing introspector(s) and called
     * as fallback for cases not otherwise handled.
     * 
     * @param introspector Annotation introspector to register.
     * 
     * @since 1.7
     */
    public final void appendAnnotationIntrospector(AnnotationIntrospector introspector) {
        _base = _base.withAnnotationIntrospector(AnnotationIntrospector.Pair.create(getAnnotationIntrospector(),
                introspector));
    }

    /**
     * Accessor for object used for determining whether specific property elements
     * (method, constructors, fields) can be auto-detected based on
     * their visibility (access modifiers). Can be changed to allow
     * different minimum visibility levels for auto-detection. Note
     * that this is the global handler; individual types (classes)
     * can further override active checker used (using
     * {@link JsonAutoDetect} annotation)
     * 
     * @since 1.5
     */    
    public final VisibilityChecker<?> getDefaultVisibilityChecker() {
        return _base.getVisibilityChecker();
    }
    
    /**
     * @since 1.8
     */
    public final PropertyNamingStrategy getPropertyNamingStrategy() {
        return _base.getPropertyNamingStrategy();
    }

    /**
     * @since 1.8
     */
    public final HandlerInstantiator getHandlerInstantiator() {
        return _base.getHandlerInstantiator();
    }
    
    /*
    /**********************************************************
    /* Configuration: mix-in annotations
    /**********************************************************
     */
    
    /**
     * Method to use for defining mix-in annotations to use for augmenting
     * annotations that processable (serializable / deserializable)
     * classes have.
     * Mixing in is done when introspecting class annotations and properties.
     * Map passed contains keys that are target classes (ones to augment
     * with new annotation overrides), and values that are source classes
     * (have annotations to use for augmentation).
     * Annotations from source classes (and their supertypes)
     * will <b>override</b>
     * annotations that target classes (and their super-types) have.
     *
     * @since 1.2
     */
    public final void setMixInAnnotations(Map<Class<?>, Class<?>> sourceMixins)
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

    /**
     * Method to use for adding mix-in annotations to use for augmenting
     * specified class or interface. All annotations from
     * <code>mixinSource</code> are taken to override annotations
     * that <code>target</code> (or its supertypes) has.
     *
     * @since 1.2
     *
     * @param target Class (or interface) whose annotations to effectively override
     * @param mixinSource Class (or interface) whose annotations are to
     *   be "added" to target's annotations, overriding as necessary
     */
    public final void addMixInAnnotations(Class<?> target, Class<?> mixinSource)
    {
        if (_mixInAnnotations == null) {
            _mixInAnnotationsShared = false;
            _mixInAnnotations = new HashMap<ClassKey,Class<?>>();
        } else if (_mixInAnnotationsShared) {
            _mixInAnnotationsShared = false;
            _mixInAnnotations = new HashMap<ClassKey,Class<?>>(_mixInAnnotations);
        }
        _mixInAnnotations.put(new ClassKey(target), mixinSource);
    }

    // ClassIntrospector.MixInResolver impl:

    /**
     * Method that will check if there are "mix-in" classes (with mix-in
     * annotations) for given class
     * 
     * @since 1.2
     */
    public final Class<?> findMixInClassFor(Class<?> cls) {
        return (_mixInAnnotations == null) ? null : _mixInAnnotations.get(new ClassKey(cls));
    }

    /**
     * @since 1.8.1
     */
    public final int mixInCount() {
        return (_mixInAnnotations == null) ? 0 : _mixInAnnotations.size();
    }
    
    /*
    /**********************************************************
    /* Configuration: type and subtype handling
    /**********************************************************
     */

    /**
     * Method called to locate a type info handler for types that do not have
     * one explicitly declared via annotations (or other configuration).
     * If such default handler is configured, it is returned; otherwise
     * null is returned.
     * 
     * @since 1.5
     */
    public final TypeResolverBuilder<?> getDefaultTyper(JavaType baseType) {
        return _base.getTypeResolverBuilder();
    }
    
    /**
     * Accessor for object used for finding out all reachable subtypes
     * for supertypes; needed when a logical type name is used instead
     * of class name (or custom scheme).
     * 
     * @since 1.6
     */
    public final SubtypeResolver getSubtypeResolver() {
        if (_subtypeResolver == null) {
            _subtypeResolver = new StdSubtypeResolver();
        }
        return _subtypeResolver;
    }

    /**
     * @since 1.8
     */
    public final TypeFactory getTypeFactory() {
        return _base.getTypeFactory();
    }

    /**
     * Helper method that will construct {@link JavaType} for given
     * raw class.
     * This is a simple short-cut for:
     *<pre>
     *    getTypeFactory().constructType(cls);
     *</pre>
     * 
     * @since 1.8
     */
    public final JavaType constructType(Class<?> cls) {
        return getTypeFactory().constructType(cls);
    }
    
    /*
    /**********************************************************
    /* Configuration: other
    /**********************************************************
     */

    /**
     * Method for accessing currently configured (textual) date format
     * that will be used for reading or writing date values (in case
     * of writing, only if textual output is configured; not if dates
     * are to be serialized as time stamps).
     *<p>
     * Note that typically {@link DateFormat} instances are <b>not thread-safe</b>
     * (at least ones provided by JDK):
     * this means that calling code should clone format instance before
     * using it.
     *<p>
     * This method is usually only called by framework itself, since there
     * are convenience methods available via
     * {@link DeserializationContext} and {@link SerializerProvider} that
     * take care of cloning and thread-safe reuse.
     */
    public final DateFormat getDateFormat() { return _base.getDateFormat(); }

    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     * 
     * @since 1.7
     */
    public abstract <DESC extends BeanDescription> DESC introspectClassAnnotations(Class<?> cls);

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     */
    public abstract <DESC extends BeanDescription> DESC introspectDirectClassAnnotations(Class<?> cls);

    /**
     * Method for determining whether annotation processing is enabled or not
     * (default settings are typically that it is enabled; must explicitly disable).
     * 
     * @return True if annotation processing is enabled; false if not
     * 
     * @since 1.8
     */
    public abstract boolean isAnnotationProcessingEnabled();

    /**
     * Accessor for determining whether it is ok to try to force override of access
     * modifiers to be able to get or set values of non-public Methods, Fields;
     * to invoke non-public Constructors, Methods; or to instantiate non-public
     * Classes. By default this is enabled, but on some platforms it needs to be
     * prevented since if this would violate security constraints and cause failures.
     * 
     * @return True if access modifier overriding is allowed (and may be done for
     *   any Field, Method, Constructor or Class); false to prevent any attempts
     *   to override.
     * 
     * @since 1.8
     */
    public abstract boolean canOverrideAccessModifiers();

    /*
    /**********************************************************
    /* Methods for instantiating handlers
    /**********************************************************
     */

    /**
     * Method that can be called to obtain an instance of <code>TypeIdResolver</code> of
     * specified type.
     * 
     * @since 1.8
     */
    public TypeResolverBuilder<?> typeResolverBuilderInstance(Annotated annotated,
            Class<? extends TypeResolverBuilder<?>> builderClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            TypeResolverBuilder<?> builder = hi.typeResolverBuilderInstance(this, annotated, builderClass);
            if (builder != null) {
                return builder;
            }
        }
        return (TypeResolverBuilder<?>) ClassUtil.createInstance(builderClass, canOverrideAccessModifiers());
    }

    /**
     * Method that can be called to obtain an instance of <code>TypeIdResolver</code> of
     * specified type.
     * 
     * @since 1.8
     */
    public TypeIdResolver typeIdResolverInstance(Annotated annotated,
            Class<? extends TypeIdResolver> resolverClass)
    {
        HandlerInstantiator hi = getHandlerInstantiator();
        if (hi != null) {
            TypeIdResolver builder = hi.typeIdResolverInstance(this, annotated, resolverClass);
            if (builder != null) {
                return builder;
            }
        }
        return (TypeIdResolver) ClassUtil.createInstance(resolverClass, canOverrideAccessModifiers());
    }
    
    /*
    /**********************************************************
    /* Deprecated methods
    /**********************************************************
     */

    /**
     * @deprecated Since 1.8, use variant that does not take arguments
     */
    @Deprecated
    public abstract T createUnshared(TypeResolverBuilder<?> typer, VisibilityChecker<?> vc,
            SubtypeResolver subtypeResolver);
    
    /**
     * Method for replacing existing {@link ClassIntrospector} with
     * specified replacement.
     * 
     * @deprecated Since 1.8, use {@link #withClassIntrospector(ClassIntrospector)} instead
     */
    @Deprecated
    public final void setIntrospector(ClassIntrospector<? extends BeanDescription> ci) {
        _base = _base.withClassIntrospector(ci);
    }

    /**
     * Method for replacing existing annotation introspector(s) with specified
     * introspector.
     * 
     * @deprecated Since 1.8, use either
     *  {@link #withAnnotationIntrospector(AnnotationIntrospector)} or
     *  Module API instead
     */
    @Deprecated
    public final void setAnnotationIntrospector(AnnotationIntrospector ai) {
        _base = _base.withAnnotationIntrospector(ai);
    }
    
    /**
     * Method that will define specific date format to use for reading/writing
     * Date and Calendar values.
     * If null is passed, will use {@link StdDateFormat}.
     * Instance is used as is, without creating a clone.
     * Format object in use can be accessed using {@link #getDateFormat}.
     * 
     * @deprecated As of version 1.8, it is preferable to call method in
     *   {@link ObjectMapper} instead; or construct new instance with
     *   {@link #withDateFormat(DateFormat)}
     */
    @Deprecated
    public void setDateFormat(DateFormat df) {
        if (df == null) {
            df = StdDateFormat.instance;
        }
        _base = _base.withDateFormat(df);
    }        

    /**
     * Method for overriding subtype resolver used.
     * 
     * @since 1.6
     * 
     * @deprecated since 1.8, use {@link #withSubtypeResolver(SubtypeResolver)} instead.
     */
    @Deprecated
    public final void setSubtypeResolver(SubtypeResolver str) {
        _subtypeResolver = str;
    }
    
    /*
    /**********************************************************
    /* Helper class to contain basic state needed to implement
    /* MapperConfig.
    /**********************************************************
     */

    /**
     * Immutable container class used to store simple configuration
     * settings. Since instances are fully immutable, instances can
     * be freely shared and used without synchronization.
     */
    public static class Base
    {
        /*
        /**********************************************************
        /* Configuration settings; introspection, related
        /**********************************************************
         */
        
        /**
         * Introspector used to figure out Bean properties needed for bean serialization
         * and deserialization. Overridable so that it is possible to change low-level
         * details of introspection, like adding new annotation types.
         */
        protected final ClassIntrospector<? extends BeanDescription> _classIntrospector;

        /**
         * Introspector used for accessing annotation value based configuration.
         */
        protected final AnnotationIntrospector _annotationIntrospector;

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
        protected final VisibilityChecker<?> _visibilityChecker;

        /**
         * Custom property naming strategy in use, if any.
         * 
         * @since 1.8
         */
        protected final PropertyNamingStrategy _propertyNamingStrategy;

        /**
         * Specific factory used for creating {@link JavaType} instances;
         * needed to allow modules to add more custom type handling
         * (mostly to support types of non-Java JVM languages)
         */
        protected final TypeFactory _typeFactory;

        /*
        /**********************************************************
        /* Configuration settings; type resolution
        /**********************************************************
         */

        /**
         * Type information handler used for "untyped" values (ones declared
         * to have type <code>Object.class</code>)
         * 
         * @since 1.5
         */
        protected final TypeResolverBuilder<?> _typeResolverBuilder;
        
        /*
        /**********************************************************
        /* Configuration settings; other
        /**********************************************************
         */
        
        /**
         * Custom date format to use for de-serialization. If specified, will be
         * used instead of {@link org.codehaus.jackson.map.util.StdDateFormat}.
         *<p>
         * Note that the configured format object will be cloned once per
         * deserialization process (first time it is needed)
         */
        protected final DateFormat _dateFormat;

        /**
         * Object used for creating instances of handlers (serializers, deserializers,
         * type and type id resolvers), given class to instantiate. This is typically
         * used to do additional configuration (with dependency injection, for example)
         * beyond simply construction of instances; or to use alternative constructors.
         */
        protected final HandlerInstantiator _handlerInstantiator;
        
        /*
        /**********************************************************
        /* Construction
        /**********************************************************
         */

        public Base(ClassIntrospector<? extends BeanDescription> ci, AnnotationIntrospector ai,
                VisibilityChecker<?> vc, PropertyNamingStrategy pns, TypeFactory tf,
                TypeResolverBuilder<?> typer, DateFormat dateFormat, HandlerInstantiator hi)
        {
            _classIntrospector = ci;
            _annotationIntrospector = ai;
            _visibilityChecker = vc;
            _propertyNamingStrategy = pns;
            _typeFactory = tf;
            _typeResolverBuilder = typer;
            _dateFormat = dateFormat;
            _handlerInstantiator = hi;
        }

        /*
        /**********************************************************
        /* Factory methods
        /**********************************************************
         */

        public Base withClassIntrospector(ClassIntrospector<? extends BeanDescription> ci) {
            return new Base(ci, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                    _typeResolverBuilder, _dateFormat, _handlerInstantiator);
        }
        
        public Base withAnnotationIntrospector(AnnotationIntrospector ai) {
            return new Base(_classIntrospector, ai, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                    _typeResolverBuilder, _dateFormat, _handlerInstantiator);
        }

        public Base withVisibilityChecker(VisibilityChecker<?> vc) {
            return new Base(_classIntrospector, _annotationIntrospector, vc, _propertyNamingStrategy, _typeFactory,
                    _typeResolverBuilder, _dateFormat, _handlerInstantiator);
        }

        public Base withPropertyNamingStrategy(PropertyNamingStrategy pns) {
            return new Base(_classIntrospector, _annotationIntrospector, _visibilityChecker, pns, _typeFactory,
                    _typeResolverBuilder, _dateFormat, _handlerInstantiator);
        }

        public Base withTypeFactory(TypeFactory tf) {
            return new Base(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, tf,
                    _typeResolverBuilder, _dateFormat, _handlerInstantiator);
        }

        public Base withTypeResolverBuilder(TypeResolverBuilder<?> typer) {
            return new Base(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                    typer, _dateFormat, _handlerInstantiator);
        }
        
        public Base withDateFormat(DateFormat df) {
            return new Base(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                    _typeResolverBuilder, df, _handlerInstantiator);
        }

        public Base withHandlerInstantiator(HandlerInstantiator hi) {
            return new Base(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
                    _typeResolverBuilder, _dateFormat, hi);
        }
        
        /*
        /**********************************************************
        /* API
        /**********************************************************
         */

        public ClassIntrospector<? extends BeanDescription> getClassIntrospector() {
            return _classIntrospector;
        }
        
        public AnnotationIntrospector getAnnotationIntrospector() {
            return _annotationIntrospector;
        }


        public VisibilityChecker<?> getVisibilityChecker() {
            return _visibilityChecker;
        }

        public PropertyNamingStrategy getPropertyNamingStrategy() {
            return _propertyNamingStrategy;
        }

        public TypeFactory getTypeFactory() {
            return _typeFactory;
        }

        public TypeResolverBuilder<?> getTypeResolverBuilder() {
            return _typeResolverBuilder;
        }
        
        public DateFormat getDateFormat() {
            return _dateFormat;
        }

        public HandlerInstantiator getHandlerInstantiator() {
            return _handlerInstantiator;
        }
    }
}
