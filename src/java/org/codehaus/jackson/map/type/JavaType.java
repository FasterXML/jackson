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
    // Standard methods; let's make them abstract to force override
    ///////////////////////////////////////////////////////////////
     */

    public abstract String toString();

    public abstract boolean equals(Object o);

    public final int hashCode() { return _hashCode; }
}
