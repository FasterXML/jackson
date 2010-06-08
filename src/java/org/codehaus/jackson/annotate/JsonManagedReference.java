package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that associated property is part of
 * two-way linkage between fields; and that its role is "parent" (or "forward") link.
 * Value type of reference must have a single compatible property annotated with
 * {@link JsonBackReference}. Linkage is handled such that the property
 * annotated with this annotation is handled normally (serialized normally, no
 * special handling for deserialization); it is the matching back reference
 * that requires special handling
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
public @interface JsonManagedReference
{

}
