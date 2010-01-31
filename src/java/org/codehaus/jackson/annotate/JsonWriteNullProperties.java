package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to define whether object properties
 * that have null values are to be written out when serializing
 * content as JSON. This affects Bean and Map serialization.
 *<p>
 * Annotation can be used with Classes (all instances of
 * given class) and Methods.
 *<p>
 * Default value for this property is 'true', meaning that null
 * properties are written.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonWriteNullProperties
{
    /**
     * Whether properties for beans of annotated type will always be
     * written (true), or only if not null (false).
     */
    boolean value() default true;
}
