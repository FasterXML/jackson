package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class annotation that can be used to define which kinds of Methods
 * are to be detected by auto-detection.
 * Auto-detection means using name conventions
 * and/or signature templates to find methods to use for data binding. For example,
 * so-called "getters" can be auto-detected by looking for public member methods
 * that return a value, do not take argument, and have prefix "get" in their name.
 *<p>
 * Pseudo-value <code>NONE</code> means that all auto-detection is disabled
 * for the <b>specific</b> class that annotation is applied to (including
 * its super-types, but only when resolving that class).
 * Pseudo-value <code>ALL</code> means that auto-detection is enabled
 * for all method types for the class in similar way.
 *<p>
 * The default value is <code>ALL</code>: that is, by default, auto-detection
 * is enabled for all classes unless instructed otherwise.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonAutoDetect
{
    /**
     * Optional default argument that defines logical property this
     * method is used to access ("get").
     */
    JsonMethod[] value() default { JsonMethod.ALL };
}
