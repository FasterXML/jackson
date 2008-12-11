package org.codehaus.jackson.map.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation (similar to
 * {@link javax.xml.bind.annotation.XmlTransient}) that indicates that
 * the property instance associated with marked method should not be
 * considered transient; or in case of annotating a type (class, enum),
 * that any property with specified type will be considered transient.
 * Transient means that such a property will not be serialized or
 * deserialized; it will essentially be quietly ignored (skipped).
 *<p>
 * Note that in case there are both accessors ("getter") and
 * mutators ("setter") for a property, it is enough to mark just
 * one with this annotation.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonTransient
{
}
