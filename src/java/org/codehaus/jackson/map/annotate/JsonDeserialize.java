package org.codehaus.jackson.map.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.jackson.annotate.JacksonAnnotation;
import org.codehaus.jackson.annotate.NoClass;
import org.codehaus.jackson.map.JsonDeserializer;

/**
 * Annotation use for configuring deserialization aspects, by attaching
 * to "setter" methods or fields, or to value classes.
 * When annotating value classes, configuration is used for instances
 * of the value class but can be overridden by more specific annotations
 * (ones that attach to methods or fields).
 *
 * @since 1.1
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonDeserialize
{
    /**
     * Deserializer class to use for
     * deserializing associated value. Depending on what is annotated,
     * value is either an instance of annotated class (used globablly
     * anywhere where class deserializer is needed); or only used for
     * deserializing property access via a setter method.
     */
    public Class<? extends JsonDeserializer<?>> using()
        default JsonDeserializer.None.class;

    /**
     * Concrete type to deserialize values as, instead of type otherwise
     * declared. Must be a subtype of declared type; otherwise an
     * exception may be thrown by deserializer.
     *<p>
     * Bogus type {@link NoClass} can be used to indicate that declared
     * type is used as is (i.e. this annotation property has no setting);
     * this since annotation properties are not allowed to have null value.
     *<p>
     * Note: if {@link #using} is also used it has precedence
     * (since it directly specified
     * deserializer, whereas this would only be used to locate the
     * deserializer)
     * and value of this annotation property is ignored.
     */
    public Class<?> as() default NoClass.class;
}
