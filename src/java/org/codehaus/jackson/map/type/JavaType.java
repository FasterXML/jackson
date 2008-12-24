package org.codehaus.jackson.map.type;

/**
 * Base class for type token classes used both to contain information
 * and as keys for deserializers.
 */
public abstract class JavaType
{
    /**
     * This is the nominal type-erased Class that would be close to the
     * type represented (but not exactly type, due to type erasure: type
     * instance may have more information on this).
     * May be an interface or abstract class, so instantiation
     * may not be possible.
     */
    final Class<?> _class;

    protected int _hashCode;

    protected JavaType(Class<?> clz)
    {
        _class = clz;
        String name = clz.getName();
        _hashCode = name.hashCode();
    }

    /*
    ///////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////
     */

    public final Class<?> getRawClass() { return _class; }

    /**
     * Method that can be used to check whether this type has
     * specified Class as its type erasure. Put another way, returns
     * true if instantiation of this Type is given (type-erased) Class.
     */
    public final boolean hasRawClass(Class<?> clz) {
        return _class == clz;
    }

    /**
     * Method that can be used to check whether type described is
     * fully typed, regarding the way JavaTypes are used for
     * deserialization. For this to be true, all Collection and Map
     * types must have proper element/value/key type information,
     * and can not be plain classes.
     */
    public abstract boolean isFullyTyped();

    /**
     * @return True if type represented is a container type; this includes
     *    array, Map and Collection types.
     */
    public abstract boolean isContainerType();

    /*
    ///////////////////////////////////////////////////////////////
    // Standard methods; let's make them abstract to force override
    ///////////////////////////////////////////////////////////////
     */

    public abstract String toString();

    public abstract boolean equals(Object o);

    public final int hashCode() { return _hashCode; }
}
