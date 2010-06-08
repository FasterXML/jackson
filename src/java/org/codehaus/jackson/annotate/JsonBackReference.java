package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that associated property is part of
 * two-way linkage between fields; and that its role is "child" (or "back") link.
 * Value type of the property must be a bean: it can not be a Collection, Map,
 * Array or enumeration.
 * Linkage is handled such that the property
 * annotated with this annotation is not serialized; and during deserialization,
 * its value is set to instance that has the "managed" (forward) link.
 *<p>
 * Note: only methods and fields can be annotated with this annotation: constructor
 * arguments should NOT be annotated, as they can not be either managed or back
 * references.
 * 
 * @author tatu
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonBackReference {

}
