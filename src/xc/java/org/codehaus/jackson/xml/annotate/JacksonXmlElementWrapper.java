package org.codehaus.jackson.xml.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.jackson.annotate.JacksonAnnotation;

/**
 * Annotation that is similar to JAXB {@link javax.xml.bind.annotation.XmlElementWrapper},
 * to indicate wrapper element to use (if any) for Collection types (arrays,
 * <code>java.util.Collection</code>). If defined, a separate container (wrapper) element
 * is used; if not, entries are written without wrapping.
 * Name of wrapper element defaults to name of the property but can be explicitly defined
 * to something else.
 * 
 * @author tatu
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JacksonXmlElementWrapper
{
    String namespace() default "";
    String localName() default "";
}
