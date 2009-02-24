package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.jackson.annotate.meta.Inherit;
import org.codehaus.jackson.annotate.meta.Inheritance;

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
 * This annotation works purely on method-by-method basis;
 * annotation on one method does not imply ignoring other methods.
 * Specifically, marking a "setter" candidate does not change handling
 * of matching "getter" method (or vice versa).
 *<p>
 * Annotation is usually used just a like a marker annotation, that
 * is, without explicitly defining 'value' argument (which defaults
 * to <code>true</code>): but argument can be explicitly defined.
 * This can be done to override an existing JsonIgnore by explictly
 * defining one with 'false' argument.
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnore
{
    /**
     * Optional argument that defines whether this annotation is active
     * or not. The only use for value 'false' if for overriding purposes
     * (which is not needed often, since this is one of annotations that
     * is "not inheritable", i.e. overriding methods do not inherit it
     * from overridden method). However, it may be necessary when used
     * with "mix-in annotations" (aka "annotation overrides").
     * For most cases, however, default value of "true" is just fine
     * and should be omitted.
     */
    boolean value() default true;
}
