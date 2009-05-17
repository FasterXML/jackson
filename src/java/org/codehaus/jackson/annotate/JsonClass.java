package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to mark "setter" methods to indicate the
 * actual type of the logical property associated with the method.
 * This is usually done if the declared type is abstract or too generic;
 * annotation can denote actual concrete type to instantiate when
 * deserializing the property.
 *<p>
 * Note that the indicated type must be compatible with the declared
 * type; that is, it has to be a sub-type or implementation of
 * the declared type. This is usually the case; and if it wasn't
 * then the call to associated "setter" method would fail with
 * a type-mismatch exception.
 *<p>
 * Note too that for container types (arrays, Lists/Collections/Maps) this
 * indicates the type of container itself; for contained Objects, use
 * {@link JsonContentClass} instead (or for Map keys,
 * {@link JsonKeyClass}).
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonClass
{
    /**
     * Class that is the expected concrete type of the property associated
     * with the annotated method. Will be used by deserializer to
     * instantiate the type.
     *<p>
     * Note: if a non-property related method is annotated with this
     * annotation,
     * deserializer will throw an exception to denote invalid annotation.
     * If used with a "getter" method, usage is not illegal, but will not
     * be used for anything.
     */
    public Class<?> value();
}
