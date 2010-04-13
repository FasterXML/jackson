package org.codehaus.jackson.jaxrs;

/**
 * Enumeration that defines standard annotation sets available for configuring
 * data binding aspects.
 */
public enum Annotations {
    /**
     * Standard Jackson annotations, defined in Jackson core and mapper
     * packages
     */
    JACKSON,

    /**
     * Standard JAXB annotations, used in a way that approximates expected
     * definitions (since JAXB defines XML aspects, not all features map
     * well to JSON handling)
     */
    JAXB
    ;
}
