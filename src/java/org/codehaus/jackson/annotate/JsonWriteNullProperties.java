package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to define whether object properties
 * that have null values are to be written out when serializing
 * content as Json. This affects Bean and Map serialization.
 *<p>
 * !!! 13-Mar-2009, tatus:
 * Note: can currently only be used with Classes (all instances of
 * given class), but could conceivably add support for Method
 * annotations as well
 *<p>
 * Default value for this property is 'true', meaning that null
 * properties are written.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonWriteNullProperties
{
    /**
     * We
     */
    boolean value() default true;
}
