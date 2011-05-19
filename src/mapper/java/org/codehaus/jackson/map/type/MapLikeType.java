package org.codehaus.jackson.map.type;

import java.util.*;

import org.codehaus.jackson.type.JavaType;

/**
 * Type that represents Map-like types; things that consist of key/value pairs but that
 * do not necessarily implement {@link java.util.Map}, but that do not have enough
 * introspection functionality to allow for some level of generic handling.
 * This specifically allows framework to check for configuration and annotation
 * settings used for Map types, and pass these to custom handlers that may be more
 * familiar with actual type.
 *
 * @since 1.8
 */
public class MapLikeType extends TypeBase
{
    /**
     * Type of keys of Map.
     */
    protected final JavaType _keyType;

    /**
     * Type of values of Map.
     */
    protected final JavaType _valueType;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected MapLikeType(Class<?> mapType, JavaType keyT, JavaType valueT)
    {
        super(mapType, keyT.hashCode() ^ valueT.hashCode());
        _keyType = keyT;
        _valueType = valueT;
    }

    public static MapLikeType construct(Class<?> rawType, JavaType keyT, JavaType valueT)
    {
        // nominally component types will be just Object.class
        return new MapLikeType(rawType, keyT, valueT);
    }

    @Override
    protected JavaType _narrow(Class<?> subclass)
    {
        return new MapLikeType(subclass, _keyType, _valueType);
    }

    @Override
    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _valueType.getRawClass()) {
            return this;
        }
        return new MapLikeType(_class, _keyType, _valueType.narrowBy(contentClass)).copyHandlers(this);
    }

    @Override
    public JavaType widenContentsBy(Class<?> contentClass)
    {
        if (contentClass == _valueType.getRawClass()) {
            return this;
        }
        return new MapLikeType(_class, _keyType, _valueType.widenBy(contentClass)).copyHandlers(this);
    }
    
    public JavaType narrowKey(Class<?> keySubclass)
    {
        // Can do a quick check first:
        if (keySubclass == _keyType.getRawClass()) {
            return this;
        }
        return new MapLikeType(_class, _keyType.narrowBy(keySubclass), _valueType).copyHandlers(this);
    }

    /**
     * @since 1.8
     */
    public JavaType widenKey(Class<?> keySubclass)
    {
        // Can do a quick check first:
        if (keySubclass == _keyType.getRawClass()) {
            return this;
        }
        return new MapLikeType(_class, _keyType.widenBy(keySubclass), _valueType).copyHandlers(this);
    }
    
    // Since 1.7:
    @Override
    public MapLikeType withTypeHandler(Object h)
    {
        MapLikeType newInstance = new MapLikeType(_class, _keyType, _valueType);
        newInstance._typeHandler = h;
        return newInstance;
    }

    // Since 1.7:
    @Override
    public MapLikeType withContentTypeHandler(Object h)
    {
        return new MapLikeType(_class, _keyType, _valueType.withTypeHandler(h));
    }
    
    @Override
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
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    @Override
    public boolean isContainerType() { return true; }

    @Override
    public boolean isMapLikeType() { return true; }
    
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

    /**
     * Not sure if we should count on this, but type names
     * for core interfaces are "K" and "V" respectively.
     * For now let's assume this should work.
     */
    @Override
    public String containedTypeName(int index) {
        if (index == 0) return "K";
        if (index == 1) return "V";
        return null;
    }

    @Override
    public StringBuilder getErasedSignature(StringBuilder sb) {
        return _classSignature(_class, sb, true);
    }
    
    @Override
    public StringBuilder getGenericSignature(StringBuilder sb)
    {
        _classSignature(_class, sb, false);
        sb.append('<');
        _keyType.getGenericSignature(sb);
        _valueType.getGenericSignature(sb);
        sb.append(">;");
        return sb;
    }
 
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Method that can be used for checking whether this type is a
     * "real" Collection type; meaning whether it represents a parameterized
     * subtype of {@link java.util.Collection} or just something that acts
     * like one.
     * 
     * @since 1.8
     */
    public boolean isTrueMapType() {
        return Map.class.isAssignableFrom(_class);
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public String toString()
    {
        return "[map-like type; class "+_class.getName()+", "+_keyType+" -> "+_valueType+"]";
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        MapLikeType other = (MapLikeType) o;
        return (_class == other._class)
            && _keyType.equals(other._keyType)
            && _valueType.equals(other._valueType);
    }
    
}
