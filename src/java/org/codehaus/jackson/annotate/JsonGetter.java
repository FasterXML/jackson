package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation that can be used to define a non-static,
 * no-argument value-returning (non-void) method to be used as a "getter"
 * for a logical property.
 * Getter means that when serializing Object instance of class that has
 * this method (possibly inherited from a super class), a call is made
 * through the method, and return value will be serialized as value of
 * the property.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonGetter
{
    /**
     * Optional default argument that defines logical property this
     * method is used to access ("get").
     */
    String value();
}
