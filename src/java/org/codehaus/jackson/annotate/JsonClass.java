package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used with "setter" and "getter" methods to
 * indicate the actual type of the logical property associated with the method.
 *<p>
 * With deserialization (which uses "setters")
 * this is usually done if the declared type is abstract or too generic;
 * annotation can denote actual concrete type to instantiate when
 * deserializing the property.
 *<p>
 * With serialization (which uses "getters") purpose is different:
 * intent is usually to use more generic type (super type) than what
 * the actual runtime type is. Using this annotation is one way to
 * limit specificity.
 *<p>
 * The indicated type must be compatible with the declared
 * type. For deserialization (setters) this means that
 * it has to be a sub-type or implementation of
 * the declared type.
 * For serialization (getters) type has to be a super type of
 * declared type, or declared type itself.
 * If these constraints are violated, an exception (usually
 * {@link IllegalArgumentException}) can be thrown by runtime.
 *<p>
 * Note that for container types (arrays, Lists/Collections/Maps) this
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
     * Class that is the type to use for the property associated
     * with the annotated method, when accessing (serialize, deserialize)
     * the property.
     * Will be used by deserializer to instantiate the type (during
     * deserialization) or to locate serializer to use (during
     * serialization).
     */
    public Class<?> value();
}
