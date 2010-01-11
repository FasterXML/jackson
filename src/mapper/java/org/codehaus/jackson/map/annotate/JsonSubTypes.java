package org.codehaus.jackson.map.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.jackson.annotate.JacksonAnnotation;

/**
 * Annotation used with {@link JsonTypeInfo} to indicate subtypes that
 * can be serialized, so that type names can be resolved (as indicated by
 * {@link JsonTypeName})
 * 
 * @since 1.5
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonSubTypes {
    public Class<?>[] value();
}
