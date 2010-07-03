package org.codehaus.jackson.map.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.jackson.annotate.JacksonAnnotation;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;

/**
 * Annotation that can be used to plug a custom type identifier handler
 * ({@link TypeIdResolver})
 * to be used by
 * {@link org.codehaus.jackson.map.TypeSerializer}s
 * and {@link org.codehaus.jackson.map.TypeDeserializer}s
 * for converting between java types and type id included in JSON content.
 * 
 * @author tatu
 * @since 1.5
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonTypeIdResolver
{
    public Class<? extends TypeIdResolver> value();
}
