package org.codehaus.jackson.map.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.jackson.annotate.JacksonAnnotation;
import org.codehaus.jackson.map.JsonSerializer;

/**
 * Annotation used for configuring serialization aspects, by attaching
 * to "getter" methods or fields, or to value classes.
 * When annotating value classes, configuration is used for instances
 * of the value class but can be overridden by more specific annotations
 * (ones that attach to methods or fields).
 *<p>
 * An example annotation would be:
 *<pre>
 *  &#64;JsonSerialize(using=MySerializer.class,
 *    as=MySubClass.class,
 *    include=JsonSerialize.Inclusion.NON_NULL,
 *    typing=JsonSerialize.Typing.STATIC
 *  )
 *</pre>
 * (which would be redundant, since some properties block others:
 * specifically, 'using' has precedence over 'as', which has precedence
 * over 'typing' setting)
 *<p>
 * NOTE: since version 1.2, annotation has also been applicable
 * to (constructor) parameters
 *
 * @since 1.1
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonSerialize
{
    // // // Annotations for explicitly specifying deserializer

    /**
     * Serializer class to use for
     * serializing associated value. Depending on what is annotated,
     * value is either an instance of annotated class (used globablly
     * anywhere where class serializer is needed); or only used for
     * serializing property access via a getter method.
     */
    public Class<? extends JsonSerializer<?>> using() default JsonSerializer.None.class;

    // // // Annotations for type handling, explicit declaration
    // // // (type used for choosing deserializer, if not explicitly
    // // // specified)

    /**
     * Subtype (of declared type, which itself is subtype of runtime type)
     * to use as type when locating serializer to use.
     *<p>
     * Bogus type {@link NoClass} can be used to indicate that declared
     * type is used as is (i.e. this annotation property has no setting);
     * this since annotation properties are not allowed to have null value.
     *<p>
     * Note: if {@link #using} is also used it has precedence
     * (since it directly specified
     * serializer, whereas this would only be used to locate the
     * serializer)
     * and value of this annotation property is ignored.
     */
    public Class<?> as() default NoClass.class;

    /**
     * Whether type detection used is dynamic or static: that is,
     * whether actual runtime type is used (dynamic), or just the
     * declared type (static).
     *
     * @since 1.2
     */
    public Typing typing() default Typing.DYNAMIC;

    // // // Annotation(s) for inclusion criteria

    /**
     * Which properties of annotated Bean are
     * to be included in serialization (has no effect on other types
     * like enums, primitives or collections).
     * Choices are "all", "properties that have value other than null"
     * and "properties that have non-default value" (i.e. default value
     * being property setting for a Bean constructed with default no-arg
     * constructor, often null).
     *
     */
    public Inclusion include() default Inclusion.ALWAYS;

    /*
    /**********************************************************
    /* Value enumerations needed
    /**********************************************************
     */

    /**
     * Enumeration used with {@link JsonSerialize#include} property
     * to define which properties
     * of Java Beans are to be included in serialization
     */
    public enum Inclusion
    {
        /**
         * Value that indicates that properties are to be always included,
         * independent of value
         */
        ALWAYS,

        /**
         * Value that indicates that only properties with non-null
         * values are to be included.
         */
        NON_NULL,

        /**
         * Value that indicates that only properties that have values
         * that differ from default settings (meaning values they have
         * when Bean is constructed with its no-arguments constructor)
         * are to be included. Value is generally not useful with
         * {@link java.util.Map}s, since they have no default values;
         * and if used, works same as {@link #ALWAYS}.
         */
        NON_DEFAULT;
    }

    /**
     * Enumeration used with {@link JsonSerialize#typing} property
     * to define whether type detection is based on dynamic runtime
     * type (DYNAMIC) or declared type (STATIC).
     */
    public enum Typing
    {
        /**
         * Value that indicates that the actual dynamic runtime type is to
         * be used.
         */
        DYNAMIC,

        /**
         * Value that indicates that the static declared type is to
         * be used.
         */
        STATIC
        ;
    }
}
