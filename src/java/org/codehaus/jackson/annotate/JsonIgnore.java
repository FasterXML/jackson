package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation similar to
 * {@link javax.xml.bind.annotation.XmlTransient} that indicates that
 * the annotated method is to be ignored by introspection-based
 * serialization and deserialization functionality. That is, it should
 * not be consider a "getter", "setter" or "creator".
 * <p>
 * For example,
 * a "getter" method that would otherwise denote
 * a property (like, say, "getValue" to suggest property "value")
 * to serialize, would be ignored and no such property would
 * be output unless another annotation defines alternative method
 * to use.
 *<p>
 * Note that this annotation works purely on method-by-method basis;
 * annotation on one method does not imply ignoring other methods.
 * Specifically, marking a "setter" candidate does not change handling
 * of matching "getter" method (or vice versa).
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnore
{
}
