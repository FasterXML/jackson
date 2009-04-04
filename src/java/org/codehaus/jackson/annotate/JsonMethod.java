package org.codehaus.jackson.annotate;

/**
 * Enumeration used to define kinds of methods that annotations like
 * {@link JsonAutoDetect} apply to.
 *<p>
 * In addition to actual method types (GETTER, SETTER, CREATOR), 2 pseudo-types
 * are defined for convenience: <code>ALL</code> and <code>NONE</code>. These
 * can be used to indicate, all or none of available method types (respectively),
 * for use by annotations that takes <code>JsonMethod</code> argument.
 */
public enum JsonMethod {
    /**
     * Getters are methods used to get a POJO field value for serialization
     */
    GETTER,

    /**
     * Getters are methods used to set a POJO value for deserialization
     */
        SETTER,

        /**
         * Creators are constructors and (static) factory methods used to construct
         * POJO instances for deserialization
         */
        CREATOR,

        /**
         * Indicates method that returns a Collection or Map type that is to be
         * used for "setting" value like JAXB does. It means that no set method
         * will be used.
         */
        GETTER_AS_SETTER,

        /**
         * This pseudo-type indicates that none of real types is included
         */
        NONE,

        /**
         * This pseudo-type indicates that all of real types are included
         */
        ALL
        ;

    private JsonMethod() { }

    public boolean getterEnabled() {
        return (this == GETTER) || (this == ALL);
    }

    public boolean setterEnabled() {
        return (this == SETTER) || (this == ALL);
    }
}
