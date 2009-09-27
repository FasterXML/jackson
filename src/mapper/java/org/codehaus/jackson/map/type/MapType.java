package org.codehaus.jackson.map.type;

import org.codehaus.jackson.type.JavaType;

/**
 * Type that represents Java Map types.
 */
public final class MapType
    extends JavaType
{
    /**
     * Type of keys of Map.
     */
    final JavaType _keyType;

    /**
     * Type of values of Map.
     */
    final JavaType _valueType;

    final boolean _fullyTyped;

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    private MapType(Class<?> mapType, JavaType keyT, JavaType valueT,
                    boolean fullyTyped)
    {
        super(mapType);
        _keyType = keyT;
        _hashCode += keyT.hashCode();
        _valueType = valueT;
        _hashCode += valueT.hashCode();
        _fullyTyped = fullyTyped;
    }

    /**
     * Method called to construct a partially typed instance. Partial
     * means that we can not determine component types, due to type
     * erasure. Resulting type may or may not be acceptable to caller.
     */
    public static MapType untyped(Class<?> rawType, JavaType keyT, JavaType valueT)
    {
        // nominally component types will be just Object.class
        return new MapType(rawType, keyT, valueT, false);
    }

    public static MapType typed(Class<?> rawType, JavaType keyT, JavaType valueT)
    {
        return new MapType(rawType, keyT, valueT, keyT.isFullyTyped() & valueT.isFullyTyped());
    }

    protected JavaType _narrow(Class<?> subclass)
    {
        return new MapType(subclass, _keyType, _valueType, _fullyTyped);
    }

    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _valueType.getRawClass()) {
            return this;
        }
        JavaType newValueType = _valueType.narrowBy(contentClass);
        return new  MapType(_class, _keyType, newValueType, _fullyTyped);
    }

    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    public boolean isFullyTyped() { return _fullyTyped; }

    public boolean isContainerType() { return true; }

    public JavaType narrowKey(Class<?> keySubclass)
    {
        // Can do a quick check first:
        if (keySubclass == _keyType.getRawClass()) {
            return this;
        }
        JavaType newKeyType = _keyType.narrowBy(keySubclass);
        return new  MapType(_class, newKeyType, _valueType, _fullyTyped);
    }

    @Override
    public JavaType getKeyType() { return _keyType; }

    @Override
    public JavaType getContentType() { return _valueType; }

    /*
    //////////////////////////////////////////////////////////
    // Standard methods
    //////////////////////////////////////////////////////////
     */

    @Override
        public String toString()
    {
        return "[map type; class "+_class.getName()+", "+_keyType+" -> "+_valueType+"]";
    }

    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        MapType other = (MapType) o;
        return _keyType.equals(other._keyType)
            && _valueType.equals(other._valueType);
    }
}
