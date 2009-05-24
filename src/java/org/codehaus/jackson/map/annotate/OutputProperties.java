package org.codehaus.jackson.map.annotate;

/**
 * Enumeration used with {@link JsonSerialize} annotation (and specifically
 * its {@link JsonSerialize#include} property) to define which properties
 * of Java Beans and {@link java.util.Map}s are to be included in
 * serialization. Default choice is usually {@link OutputProperties#ALL}.
 *
 * @since 1.1
 */
public enum OutputProperties
{
    /**
     * Value that indicates that all properties are to be included,
     * independent of value
     */
    ALL,

        /**
         * Value that indicates that only properties with non-null
         * values are to be included.
         */
        NON_NULL,

        /**
         * Value that indicates that only properties that have values
         * that differ from default settings (meaning values they have
         * when Bean is constructed with its no-arguments constructor)
         * are to be included. Value is generally not useful with
         * {@link java.util.Map}s, since they have no default values;
         * and if used, works same as {@link #ALL}.
         */
        NON_DEFAULT;
}


