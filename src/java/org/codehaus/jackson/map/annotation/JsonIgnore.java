package org.codehaus.jackson.map.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation similar to
 * {@link javax.xml.bind.annotation.XmlTransient} that indicates that
 * the annotated method is to be ignored by introspection-based
 * serialization and deserialization functionality. For example,
 * a "getter" method that would otherwise denote
 * a property (like, say, "getValue" to suggest property "value")
 * to serialize, would be ignored and no such property would
 * be output (unless another annotation defines alternative method
 * to use).
 *<p>
 * Note that in case there are both accessors ("getter") and
 * mutators ("setter") for a property, annotations are handled
 * separately: so it is possible that a property will get serialized
 * (if there is a getter) but not deserialized (if matching setter
 * is marked to be ignored).
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnore
{
}
