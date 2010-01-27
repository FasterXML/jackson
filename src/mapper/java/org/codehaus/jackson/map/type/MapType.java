package org.codehaus.jackson.map.type;

import org.codehaus.jackson.type.JavaType;

/**
 * Type that represents Java Map types.
 */
public final class MapType
    extends TypeBase
{
    /**
     * Type of keys of Map.
     */
    final JavaType _keyType;

    /**
     * Type of values of Map.
     */
    final JavaType _valueType;

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    private MapType(Class<?> mapType, JavaType keyT, JavaType valueT)
    {
        super(mapType);
        _keyType = keyT;
        _hashCode += keyT.hashCode();
        _valueType = valueT;
        _hashCode += valueT.hashCode();
    }

    public static MapType construct(Class<?> rawType, JavaType keyT, JavaType valueT)
    {
        // nominally component types will be just Object.class
        return new MapType(rawType, keyT, valueT);
    }

    protected JavaType _narrow(Class<?> subclass)
    {
        return new MapType(subclass, _keyType, _valueType);
    }

    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _valueType.getRawClass()) {
            return this;
        }
        JavaType newValueType = _valueType.narrowBy(contentClass);
        return new MapType(_class, _keyType, newValueType);
    }

	protected String buildCanonicalName() {
		StringBuilder sb = new StringBuilder();
		sb.append(_class.getName());
		if (_keyType != null) {
			sb.append('<');
			sb.append(_keyType.toCanonical());
			sb.append(',');
			sb.append(_valueType.toCanonical());
			sb.append('>');
		}
		return sb.toString();
	}
    
    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    public boolean isContainerType() { return true; }

    public JavaType narrowKey(Class<?> keySubclass)
    {
        // Can do a quick check first:
        if (keySubclass == _keyType.getRawClass()) {
            return this;
        }
        JavaType newKeyType = _keyType.narrowBy(keySubclass);
        return new MapType(_class, newKeyType, _valueType);
    }

    @Override
    public JavaType getKeyType() { return _keyType; }

    @Override
    public JavaType getContentType() { return _valueType; }

    @Override
    public int containedTypeCount() { return 2; }
    
    @Override
    public JavaType containedType(int index) {
        if (index == 0) return _keyType;
        if (index == 1) return _valueType;
        return null;
    }
    
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
        return (_class == other._class)
            && _keyType.equals(other._keyType)
            && _valueType.equals(other._valueType);
    }
}
