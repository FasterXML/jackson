package org.codehaus.jackson.map.type;

import java.util.*;

import org.codehaus.jackson.type.JavaType;

/**
 * Simple types are defined as anything other than one of recognized
 * container types (arrays, Collections, Maps). For our needs we
 * need not know anything further, since we have no way of dealing
 * with generic types other than Collections and Maps.
 */
public final class SimpleType
    extends JavaType
{
    /**
     * This type token is used if the type is unknown due to
     * type erasure.
     */
    public final static SimpleType TYPE_UNSPECIFIED = new SimpleType(Object.class);

    /**
     * This type token is used if the underlying type is only known as
     * unqualified wildcard ("?").
     */
    public final static SimpleType TYPE_WILDCARD = new SimpleType(Object.class);

    /**
     * For generic types we need to keep track of mapping from formal
     * into actual types, to be able to resolve generic signatures.
     */
    protected Map<String,JavaType> _typeParameters;
    
    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    private SimpleType(Class<?> cls) { this(cls, null); }

    protected SimpleType(Class<?> cls, Map<String,JavaType> typeParams)
    {
        super(cls);
        _typeParameters = typeParams;
    }

    protected JavaType _narrow(Class<?> subclass)
    {
        return new SimpleType(subclass);
    }

    public JavaType narrowContentsBy(Class<?> subclass)
    {
        // should never get called
        throw new IllegalArgumentException("Internal error: SimpleType.narrowContentsBy() should never be called");
    }

    public static SimpleType construct(Class<?> cls, Map<String,JavaType> typeParams)
    {
        /* Let's add sanity checks, just to ensure no
         * Map/Collection entries are constructed
         */
        if (Map.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Can not construct SimpleType for a Map (class: "+cls.getName()+")");
        }
        if (Collection.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Can not construct SimpleType for a Collection (class: "+cls.getName()+")");
        }
        // ... and while we are at it, not array types either
        if (cls.isArray()) {
            throw new IllegalArgumentException("Can not construct SimpleType for an array (class: "+cls.getName()+")");
        }
        return new SimpleType(cls, typeParams);
    }

    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    /**
     * Simple types are always fully typed, except for special
     * placeholder types. Maintaining this constrain requires that
     * no instances are ever created for Collection/Map types.
     */
    @Override
    public boolean isFullyTyped() {
        return (this != TYPE_UNSPECIFIED && this != TYPE_WILDCARD);
    }

    @Override
    public boolean isContainerType() { return false; }

    @Override
    public JavaType findVariableType(String name)
    {
        if (_typeParameters != null) {
            return _typeParameters.get(name);
        }
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
        return "[simple type, class "+_class.getName()+"]";
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        // Also, we have some canonical instances that do not match
        if (o == TYPE_UNSPECIFIED || o == TYPE_WILDCARD) return false;

        SimpleType other = (SimpleType) o;

        // Classes must be identical... 
        return (other._class == this._class);
    }
}
