package org.codehaus.jackson.map.type;

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

    public MapType(Class<?> mapType, JavaType keyT, JavaType valueT)
    {
        super(mapType);
        _keyType = keyT;
        _hashCode += keyT.hashCode();
        _valueType = valueT;
        _hashCode += valueT.hashCode();
    }

    @Override
        public String toString()
    {
        return "[map type, "+_keyType+" -> "+_valueType+"]";
    }

    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        MapType other = (MapType) o;
        return _keyType.equals(other._keyType)
            || _valueType.equals(other._valueType);
    }
}
