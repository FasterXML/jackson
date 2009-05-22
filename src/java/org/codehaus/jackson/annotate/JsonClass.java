package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used with "setter" methods to
 * indicate the actual type to use for deserializing value
 * of the associated logical property.
 * This is usually done if the declared type is abstract or too generic;
 * annotation can denote actual concrete type to instantiate when
 * deserializing the property.
 *<p>
 * The indicated type must be compatible with the declared
 * type. For deserialization (setters) this means that
 * it has to be a sub-type or implementation of
 * the declared type.
 * If this constraint is violated, an exception (usually
 * {@link IllegalArgumentException}) can be thrown by runtime.
 *<p>
 * Note that for container types (arrays, Lists/Collections/Maps) this
 * indicates the type of container itself; for contained Objects, use
 * {@link JsonContentClass} instead (or for Map keys,
 * {@link JsonKeyClass}).
 *
 * @deprecated As of version 1.1, use {@link org.codehaus.jackson.map.annotate.JsonDeserialize#as} instead
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonClass
{
    /**
     * Class that is the type to use for deserializating value of
     * the property associated
     * with the annotated method.
     */
    public Class<?> value();
}
