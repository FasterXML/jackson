package org.codehaus.jackson.map;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.SubtypeResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.jsontype.impl.StdSubtypeResolver;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.type.TypeFactory;
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
    /* Configuration settings; introspection, related
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
    protected final TypeResolverBuilder<?> _typer;

    /**
     * Registered concrete subtypes that can be used instead of (or
     * in addition to) ones declared using annotations.
     * 
     * @since 1.6
     */
    protected SubtypeResolver _subtypeResolver;
    
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
    protected DateFormat _dateFormat;
    
    /*
    /**********************************************************
    /* Life-cycle: construction
    /**********************************************************
     */

    protected MapperConfig(ClassIntrospector<? extends BeanDescription> intr,
            AnnotationIntrospector annIntr, VisibilityChecker<?> vc, SubtypeResolver subtypeResolver,
            PropertyNamingStrategy propertyNamingStrategy, TypeFactory typeFactory)
    {
        _classIntrospector = intr;
        _annotationIntrospector = annIntr;
        _visibilityChecker = vc;
        _subtypeResolver = subtypeResolver;
        _propertyNamingStrategy = propertyNamingStrategy;
        _typeFactory = typeFactory;
        _typer = null;
        _dateFormat = DEFAULT_DATE_FORMAT;
    }

    protected MapperConfig(MapperConfig<?> src, HashMap<ClassKey,Class<?>> mixins,
            VisibilityChecker<?> vc, SubtypeResolver subtypeResolver,
            TypeResolverBuilder<?> typer)
    {
        _mixInAnnotations = mixins;
        _classIntrospector = src._classIntrospector;
        _annotationIntrospector = src._annotationIntrospector;
        _visibilityChecker = vc;
        _subtypeResolver = subtypeResolver;
        _propertyNamingStrategy = src._propertyNamingStrategy;
        _typeFactory = src._typeFactory;
        _typer = typer;
        _dateFormat = src._dateFormat;
    }

    /**
     * @since 1.8
     */
    protected MapperConfig(MapperConfig<?> src,
            DateFormat dateFormat, PropertyNamingStrategy naming, TypeFactory typeFactory)
    {
        _classIntrospector = src._classIntrospector;
        _annotationIntrospector = src._annotationIntrospector;
        _visibilityChecker = src._visibilityChecker;
        _subtypeResolver = src._subtypeResolver;
        _propertyNamingStrategy = naming;
        _typeFactory = typeFactory;
        _typer = src._typer;
        _dateFormat = dateFormat;
    }
    
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
     * between multiple operations but only used for a single one.
     */
    public abstract T createUnshared(TypeResolverBuilder<?> typer, VisibilityChecker<?> vc,
            SubtypeResolver subtypeResolver);

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
    
    /*
    /**********************************************************
    /* Configuration: introspectors, mix-ins
    /**********************************************************
     */

    /**
     * Method for replacing existing {@link ClassIntrospector} with
     * specified replacement.
     */
    public final void setIntrospector(ClassIntrospector<? extends BeanDescription> i) {
        _classIntrospector = i;
    }
    
    /**
     * Method for getting {@link AnnotationIntrospector} configured
     * to introspect annotation values used for configuration.
     *<p>
     * Non-final since it is actually overridden by sub-classes (for now?)
     */
    public abstract AnnotationIntrospector getAnnotationIntrospector();

    /**
     * Method for replacing existing annotation introspector(s) with specified
     * introspector.
     */
    public final void setAnnotationIntrospector(AnnotationIntrospector ai) {
        _annotationIntrospector = ai;
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
        _annotationIntrospector = AnnotationIntrospector.Pair.create(introspector, _annotationIntrospector);
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
        _annotationIntrospector = AnnotationIntrospector.Pair.create(_annotationIntrospector, introspector);
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
        return _visibilityChecker;
    }
    
    /**
     * @since 1.8
     */
    public final PropertyNamingStrategy getPropertyNamingStrategy() {
        return _propertyNamingStrategy;
    }
    
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
        if (_mixInAnnotations == null || _mixInAnnotationsShared) {
            _mixInAnnotationsShared = false;
            _mixInAnnotations = new HashMap<ClassKey,Class<?>>();
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
        return _typer;
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
     * Method for overriding subtype resolver used.
     * 
     * @since 1.6
     */
    public final void setSubtypeResolver(SubtypeResolver r) {
        _subtypeResolver = r;
    }

    /**
     * @since 1.8
     */
    public final TypeFactory getTypeFactory() {
        return _typeFactory;
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
    public final DateFormat getDateFormat() { return _dateFormat; }

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
    /* Deprecated methods
    /**********************************************************
     */

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
        _dateFormat = (df == null) ? StdDateFormat.instance : df;
    }
}
