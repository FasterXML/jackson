package org.codehaus.jackson.map.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to mark "setter" methods to indicate the
 * actual type of the property associted with the method. This is usually
 * done if the declared type is abstract or too generic; annotation can
 * denote actual concrete type to instantiate when deserializing the
 * property.
 *<p>
 * Note that for container types (arrays, Lists/Collections/Maps) this
 * indicates the type of container itself; for contained type, use
 * {@link JsonContainedType} instead
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonType
{
    /**
     * Class that is the expected concrete type of the property associated
     * with the annotated method. Will be used by deserializer to
     * instantiate the type.
     *<p>
     * Note: if a non-property method is annotated with this annotation,
     * deserializer will throw an exception to denote invalid annotation.
     */
    public Class<?> value();
}
