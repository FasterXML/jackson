package org.codehaus.jackson.type;

import java.lang.reflect.Modifier;

/**
 * Base class for type token classes used both to contain information
 * and as keys for deserializers.
 *<p>
 * Instances can (only) be constructed by
 * {@link org.codehaus.jackson.map.type.TypeFactory}.
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

    /**
     * Optional handler (codec) that can be attached to indicate 
     * what to use for this specific type.
     *<p>
     * Note: untyped (i.e. caller has to cast) because it is used for
     * different kinds of handlers, with unrelated types.
     *
     * @since 1.3
     */
    Object _handler;

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
        JavaType result = _narrow(subclass);
        if (_handler != null) {
            result.setHandler(_handler);
        }
        return result;
    }

    /**
     * Method that can be called to do a "widening" conversions; that is,
     * to return a type with a raw class that could be assigned from this
     * type.
     * If such conversion is not possible, an
     * {@link IllegalArgumentException} is thrown.
     * If class is same as the current raw class, instance itself is
     * returned.
     */
    public final JavaType widenBy(Class<?> superclass)
    {
        // First: if same raw class, just return this instance
        if (superclass == _class) {
            return this;
        }
        // Otherwise, ensure compatibility
        _assertSubclass(_class, superclass);
        return _widen(superclass);
    }

    protected abstract JavaType _narrow(Class<?> subclass);

    /**
     *<p>
     * Default implementation is just to call {@link #_narrow}, since
     * underlying type construction is usually identical
     */
    protected JavaType _widen(Class<?> superclass) {
        return _narrow(superclass);
    }

    public abstract JavaType narrowContentsBy(Class<?> contentClass);

    /**
     * Method for assigning handler to associate with this type; or
     * if null passed, to remove such assignment
     * 
     * @since 1.3
     */
    public void setHandler(Object h) {
        // sanity check, should be assigned just once
        if (h != null && _handler != null) {
            throw new IllegalStateException("Trying to reset handler for type ["+toString()+"]; old handler of type "+_handler.getClass().getName()+", new handler of type "+h.getClass().getName());
        }
        _handler = h;
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

    public final boolean isAbstract() {
        return Modifier.isAbstract(_class.getModifiers());
    }

    public final boolean isArrayType() { return _class.isArray(); }

    public final boolean isEnumType() { return _class.isEnum(); }

    public final boolean isInterface() { return _class.isInterface(); }

    public final boolean isPrimitive() { return _class.isPrimitive(); }

    /**
     * Method that can be called to see if this type has generic type
     * binding information for type variables, for given formal
     * type parameter name.
     *
     * @return Type given formal type parameter name maps to, if any;
     *   null if this type knows of no binding for name
     */
    public JavaType findVariableType(String name) { return null; }

    /**
     * Method for accessing key type for this type, assuming type
     * has such a concept (only Map types do)
     */
    public JavaType getKeyType() { return null; }

    /**
     * Method for accessing content type of this type, if type has
     * such a thing: simple types do not, structured types do
     * (like arrays, Collections and Maps)
     */
    public JavaType getContentType() { return null; }

    /**
     * Method for accessing handler associated with this type, if any
     * 
     * @since 1.3
     */
    public Object getHandler() { return _handler; }

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
