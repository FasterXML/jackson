package org.codehaus.jackson.map;

import java.util.Map;

/**
 * Interface that defines functionality accessible through both
 * serialization and deserialization configuration objects;
 * accessors to mode-independent configuration settings
 * and such.
 *
 * @since 1.2
 */
public interface MapperConfig
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
    public MapperConfig createUnshared();

    // // // Configuration

    public AnnotationIntrospector getAnnotationIntrospector();

    public void setAnnotationIntrospector(AnnotationIntrospector introspector);

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
}
