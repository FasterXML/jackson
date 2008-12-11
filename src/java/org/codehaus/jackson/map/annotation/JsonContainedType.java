package org.codehaus.jackson.map.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to mark "setter" methods to indicate the
 * actual type of values contained in a container type that is value
 * of the property associted with the method.
 * (phew! that's a mouthful!).
 * This is usually done if the declared element type is abstract or
 * too generic; annotation can denote actual concrete type to
 * instantiate when deserializing contents of the container.
 * To define type of the actual container itself, use 
 * {@link JsonType} instead.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonContainedType
{
    /**
     * Class that is the expected concrete value type of the container
     * (which is value of the property associated
     * with the annotated method). Will be used by deserializer to
     * instantiate the type, using
     *<p>
     * Note: if a non-property method is annotated with this annotation,
     * deserializer will throw an exception to denote invalid annotation.
     */
    public Class value();
}
