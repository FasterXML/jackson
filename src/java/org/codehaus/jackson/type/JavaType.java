package org.codehaus.jackson.type;

import java.lang.reflect.Modifier;

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
    protected final Class<?> _class;

    protected int _hashCode;

    /*
    ///////////////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////////////
     */

    protected JavaType(Class<?> clz)
    {
        _class = clz;
        String name = clz.getName();
        _hashCode = name.hashCode();
    }


    /**
     * Method that can be called to do a "narrowing" conversions; that is,
     * to return a type with a raw class that is assignable to the raw
     * class of this type. If this is not possible, an
     * {@link IllegalArgumentException} is thrown.
     * If class is same as the current raw class, instance itself is
     * returned.
     */
    public final JavaType narrowBy(Class<?> subclass)
    {
        // First: if same raw class, just return this instance
        if (subclass == _class) {
            return this;
        }
        // Otherwise, ensure compatibility
        _assertSubclass(subclass, _class);
        return _narrow(subclass);
    }

    protected abstract JavaType _narrow(Class<?> subclass);

    public abstract JavaType narrowContentsBy(Class<?> contentClass);

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

    public final boolean isAbstract() {
        return Modifier.isAbstract(_class.getModifiers());
    }

    public final boolean isArrayType() { return _class.isArray(); }

    public final boolean isEnumType() { return _class.isEnum(); }

    public final boolean isInterface() { return _class.isInterface(); }

    public final boolean isPrimitive() { return _class.isPrimitive(); }

    /*
    ///////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////
     */

    protected void _assertSubclass(Class<?> subclass, Class<?> superClass)
    {
        if (!_class.isAssignableFrom(subclass)) {
            throw new IllegalArgumentException("Class "+subclass.getName()+" is not assignable to "+_class.getName());
        }
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
