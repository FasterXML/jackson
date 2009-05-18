package org.codehaus.jackson.map;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;

/**
 * Abstract class that defines API used for introspecting annotation-based
 * configuration for serialization and deserialization. Separated
 * so that different sets of annotations can be supported, and support
 * plugged-in dynamically.
 */
public abstract class AnnotationIntrospector
{
    // // // Generic annotation properties

    /**
     * Method called by framework to determine whether given annotation
     * is handled by this introspector.
     */
    public abstract boolean isHandled(Annotation ann);

    /*
    ///////////////////////////////////////////////////////
    // Class annotations, general
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether there is a class annotation that
     * indicates that given method should be ignored for all
     * operations (serialization, deserialization).
     *
     * @return True, if an annotation is found to indicate that the
     *    method should be ignored; false if not.
     */
    public abstract boolean isIgnorableMethod(AnnotatedMethod m);

    /*
    ///////////////////////////////////////////////////////
    // Class annotations: serialization
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether there is a class annotation that
     * indicates whether getter-method auto detection should be enabled.
     *
     * @return null if no relevant annotation is located; Boolean.TRUE
     *   if enabling annotation found, Boolean.FALSE if disabling annotation
     */
    public abstract Boolean findGetterAutoDetection(AnnotatedClass ac);

    /*
    ///////////////////////////////////////////////////////
    // Class annotations: deserialization
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether there is a class annotation that
     * indicates whether setter-method auto detection should be enabled.
     *
     * @return null if no relevant annotation is located; Boolean.TRUE
     *   if enabling annotation found, Boolean.FALSE if disabling annotation
     */
    public abstract Boolean findSetterAutoDetection(AnnotatedClass ac);

    /*
    ///////////////////////////////////////////////////////
    // Method annotations: serialization
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether given method has an annotation
     * that suggests property name associated with method that
     * may be a "getter". Should return null if no annotation
     * is found; otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use standard bean name detection if applicable;
     * method name if not".
     */
    public abstract String findGettablePropertyName(AnnotatedMethod am);

    /*
    ///////////////////////////////////////////////////////
    // Method annotations: deserialization
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether given method has an annotation
     * that suggests property name associated with method that
     * may be a "setter". Should return null if no annotation
     * is found; otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use standard bean name detection if applicable;
     * method name if not".
     */
    public abstract String findSettablePropertyName(AnnotatedMethod am);
}
