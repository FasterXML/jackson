package org.codehaus.jackson.map;

import java.text.DateFormat;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.SubtypeResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Interface that defines functionality accessible through both
 * serialization and deserialization configuration objects;
 * accessors to mode-independent configuration settings
 * and such.
 *
 * @since 1.2
 */
public interface MapperConfig<T extends MapperConfig<T>>
    extends ClassIntrospector.MixInResolver
{
    // // // Accessors

    // // // Life-cycle methods

    /**
     * Method that checks class annotations that the argument Object has,
     * and modifies settings of this configuration object accordingly,
     * similar to how those annotations would affect actual value classes
     * annotated with them, but with global scope. Note that not all
     * annotations have global significance, and thus only subset of
     * Jackson annotations will have any effect.
     */
    public void fromAnnotations(Class<?> cls);

    /**
     * Method to use for constructing an instance that is not shared
     * between multiple operations but only used for a single one.
     */
    public T createUnshared(TypeResolverBuilder<?> typer, VisibilityChecker<?> vc,
            SubtypeResolver subtypeResolver);

    // // // Configuration

    public AnnotationIntrospector getAnnotationIntrospector();

    /**
     * Method for replacing existing annotation introspector(s) with specified
     * introspector.
     */
    public void setAnnotationIntrospector(AnnotationIntrospector introspector);

    /**
     * Method for registering specified {@link AnnotationIntrospector} as the highest
     * priority introspector (will be chained with existing introspector(s) which
     * will be used as fallbacks for cases this introspector does not handle)
     * 
     * @param introspector Annotation introspector to register.
     * 
     * @since 1.7
     */
    public void insertAnnotationIntrospector(AnnotationIntrospector introspector);

    /**
     * Method for registering specified {@link AnnotationIntrospector} as the lowest
     * priority introspector, chained with existing introspector(s) and called
     * as fallback for cases not otherwise handled.
     * 
     * @param ai Annotation introspector to register.
     * 
     * @since 1.7
     */
    public void appendAnnotationIntrospector(AnnotationIntrospector ai);
    
    /**
     * Method for replacing existing {@link ClassIntrospector} with
     * specified replacement.
     */
    public void setIntrospector(ClassIntrospector<? extends BeanDescription> i);

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
    public void setMixInAnnotations(Map<Class<?>, Class<?>> mixins);

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
    public void addMixInAnnotations(Class<?> target, Class<?> mixinSource);

    // ClassIntrospector.MixInResolver impl:

    /**
     * Method that will check if there are "mix-in" classes (with mix-in
     * annotations) for given class
     */
    public Class<?> findMixInClassFor(Class<?> cls);

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
    public DateFormat getDateFormat();

    /**
     * Method that wiTll define specific date format to use for reading/writing
     * Date and Calendar values; instance is used as is, without creating
     * a clone.
     * Format object can be access using
     * {@link #getDateFormat}.
     */
    public void setDateFormat(DateFormat df);
    
    /**
     * Method called to locate a type info handler for types that do not have
     * one explicitly declared via annotations (or other configuration).
     * If such default handler is configured, it is returned; otherwise
     * null is returned.
     * 
     * @since 1.5
     */
    public TypeResolverBuilder<?> getDefaultTyper(JavaType baseType);

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
    public VisibilityChecker<?> getDefaultVisibilityChecker();

    /**
     * Accessor for object used for finding out all reachable subtypes
     * for supertypes; needed when a logical type name is used instead
     * of class name (or custom scheme).
     * 
     * @since 1.6
     */
    public SubtypeResolver getSubtypeResolver();

    /**
     * Method for overriding subtype resolver used.
     * 
     * @since 1.6
     */
    public void setSubtypeResolver(SubtypeResolver r);

    /**
     * @since 1.8
     */
    public PropertyNamingStrategy getPropertyNamingStrategy();

    /**
     * @since 1.8
     */
    public TypeFactory getTypeFactory();
    
    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     * 
     * @since 1.7
     */
    public <DESC extends BeanDescription> DESC introspectClassAnnotations(Class<?> cls);

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     */
    public <DESC extends BeanDescription> DESC introspectDirectClassAnnotations(Class<?> cls);

    /**
     * Specific accessor for determining whether annotation processing is enabled or not
     * (default settings are typically that it is enabled; must explicitly disable).
     * 
     * @return True if annotation processing is enabled; false if not
     * 
     * @since 1.8
     */
    public boolean isAnnotationProcessingEnabled();
}
