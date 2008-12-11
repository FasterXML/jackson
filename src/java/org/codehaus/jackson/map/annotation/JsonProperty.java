package org.codehaus.jackson.map.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to associate annotated method with
 * logical property name. Usually only used if the method does not
 * conform to expected "Bean" naming convention (including cases where
 * pluralization should be added or removed).
 *<p>
 * Note that the actual role of the method (accessor ["getter"] or
 * mutator ["setter"]) is determined based on signature.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonProperty
{
    /**
     * Name of the property the annotated method should be associated with.
     */
    public String value();
}
