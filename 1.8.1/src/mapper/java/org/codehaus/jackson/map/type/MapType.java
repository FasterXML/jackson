package org.codehaus.jackson.map.type;

import org.codehaus.jackson.type.JavaType;

/**
 * Type that represents "true" Java Map types.
 */
public final class MapType extends MapLikeType
{
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    private MapType(Class<?> mapType, JavaType keyT, JavaType valueT) {
        super(mapType, keyT, valueT);
    }

    public static MapType construct(Class<?> rawType, JavaType keyT, JavaType valueT) {
        // nominally component types will be just Object.class
        return new MapType(rawType, keyT, valueT);
    }

    @Override
    protected JavaType _narrow(Class<?> subclass) {
        return new MapType(subclass, _keyType, _valueType);
    }

    @Override
    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _valueType.getRawClass()) {
            return this;
        }
        return new MapType(_class, _keyType, _valueType.narrowBy(contentClass)).copyHandlers(this);
    }

    @Override
    public JavaType widenContentsBy(Class<?> contentClass)
    {
        if (contentClass == _valueType.getRawClass()) {
            return this;
        }
        return new MapType(_class, _keyType, _valueType.widenBy(contentClass)).copyHandlers(this);
    }
    
    @Override
    public JavaType narrowKey(Class<?> keySubclass)
    {
        // Can do a quick check first:
        if (keySubclass == _keyType.getRawClass()) {
            return this;
        }
        return new MapType(_class, _keyType.narrowBy(keySubclass), _valueType).copyHandlers(this);
    }

    /**
     * @since 1.8
     */
    @Override
    public JavaType widenKey(Class<?> keySubclass)
    {
        // Can do a quick check first:
        if (keySubclass == _keyType.getRawClass()) {
            return this;
        }
        return new MapType(_class, _keyType.widenBy(keySubclass), _valueType).copyHandlers(this);
    }
    
    // Since 1.7:
    @Override
    public MapType withTypeHandler(Object h)
    {
        MapType newInstance = new MapType(_class, _keyType, _valueType);
        newInstance._typeHandler = h;
        return newInstance;
    }

    // Since 1.7:
    @Override
    public MapType withContentTypeHandler(Object h)
    {
        return new MapType(_class, _keyType, _valueType.withTypeHandler(h));
    }
    
    
    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public String toString()
    {
        return "[map type; class "+_class.getName()+", "+_keyType+" -> "+_valueType+"]";
    }
}
