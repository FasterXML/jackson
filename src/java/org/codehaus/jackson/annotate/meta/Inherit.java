package org.codehaus.jackson.annotate.meta;

/**
 * Enumeration for values to use with {@link Inheritance}
 * meta-annotation. Specifies types of things with which
 * inheritance is enabled, as well as 2 special values that
 * indicate "all" and "none" cases.
 */
public enum Inherit {
    /**
     * Value that indicates that the annotation is never
     * inherited.
     */
    NEVER,

        /**
         * Value that indicates that the annotations are inherited
         * when applied to methods.
         */
        FOR_METHOD,

        /**
         * Value that indicates that the annotations are inherited
         * when applied to classes.
         */
        FOR_CLASS,

        /**
         * Value that indicates that the annotations are inherited
         * independent of how they are applied.
         */
        ALWAYS
        ;
};
