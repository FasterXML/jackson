package org.codehaus.jackson.map;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.AnnotatedField;
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

    /**
     * Method for checking whether methods of given class should
     * output null values; given default value for behavior (true
     * meaning do output nulls, false not).
     *
     * @return True if nulls are to be output; false if not.
     */
    public abstract boolean willWriteNullProperties(AnnotatedClass am, boolean defValue);

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
    // Method annotations, general
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether there is an annotation that
     * indicates that given method should be ignored for all
     * operations (serialization, deserialization).
     *
     * @return True, if an annotation is found to indicate that the
     *    method should be ignored; false if not.
     */
    public abstract boolean isIgnorableMethod(AnnotatedMethod m);

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

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the return value of annotated method
     * should be used as "the value" of the object instance; usually
     * serialized as a primitive value such as String or number.
     *
     * @return True if such annotation is found (and is not disabled);
     *   false if no enabled annotation is found
     */
    public abstract boolean hasAsValueAnnotation(AnnotatedMethod am);

    /**
     * Method for checking whether property associated with given method
     * should output null values; given default value for behavior (true
     * meaning do output nulls, false not).
     *
     * @return True if nulls are to be output; false if not.
     */
    public abstract boolean willWriteNullProperties(AnnotatedMethod am, boolean defValue);

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

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the method is to serve as "any setter";
     * method to be used for setting values of any properties for
     * which no dedicated setter method is found.
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     */
    public abstract boolean hasAnySetterAnnotation(AnnotatedMethod am);

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the method is a "creator" (aka factory)
     * method to be used for construct new instances of deserialized
     * values.
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     */
    public abstract boolean hasCreatorAnnotation(AnnotatedMethod am);

    /*
    ////////////////////////////////////////////////////
    // Field annotations: general
    ////////////////////////////////////////////////////
     */

    /**
     * Method for checking whether there is an annotation that
     * indicates that given field should be ignored for all
     * operations (serialization, deserialization).
     *
     * @return True, if an annotation is found to indicate that the
     *    field should be ignored; false if not.
     */
    public abstract boolean isIgnorableField(AnnotatedField f);
}
