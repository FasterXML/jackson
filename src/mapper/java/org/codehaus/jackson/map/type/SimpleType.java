package org.codehaus.jackson.map.type;

/*
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
*/

import java.util.*;

import org.codehaus.jackson.type.JavaType;

/**
 * Simple types are defined as anything other than one of recognized
 * container types (arrays, Collections, Maps). For our needs we
 * need not know anything further, since we have no way of dealing
 * with generic types other than Collections and Maps.
 */
public final class SimpleType
    extends TypeBase
{
    /**
     * Need to keep track of generic type parametrization as well.
     */
    protected JavaType[] _typeParameters;
    
    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    protected SimpleType(Class<?> cls, JavaType[] typeParams) {
        super(cls);
        _typeParameters = (typeParams == null || typeParams.length == 0)
            ? null : typeParams;
    }

    protected static SimpleType construct(Class<?> cls, Collection<JavaType> paramTypes)
    {
        JavaType[] tp;
        if (paramTypes == null || paramTypes.isEmpty()) {
            tp = null;
        } else {
            tp = paramTypes.toArray(new JavaType[paramTypes.size()]);
        }
        return new SimpleType(cls, tp);
    }
    
    protected JavaType _narrow(Class<?> subclass)
    {
        // Should we check that there is a sub-class relationship?
        return new SimpleType(subclass, _typeParameters);
    }

    public JavaType narrowContentsBy(Class<?> subclass)
    {
        // should never get called
        throw new IllegalArgumentException("Internal error: SimpleType.narrowContentsBy() should never be called");
    }

    public static SimpleType construct(Class<?> cls)
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
        return new SimpleType(cls, null);
    }

    protected String buildCanonicalName()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(_class.getName());
        if (_typeParameters != null && _typeParameters.length > 0) {
            sb.append('<');
            boolean first = true;
            for (JavaType t : _typeParameters) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(t.toCanonical());
            }
            sb.append('>');
        }
        return sb.toString();
    }
    
    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    @Override
    public boolean isContainerType() { return false; }

    public int containedTypeCount() {
        return (_typeParameters == null) ? 0 : _typeParameters.length;
    }

    public JavaType containedType(int index)
    {
        if (index < 0 || _typeParameters == null || index >= _typeParameters.length) {
            return null;
        }
        return _typeParameters[index];
    }
    
    /*
    //////////////////////////////////////////////////////////
    // Standard methods
    //////////////////////////////////////////////////////////
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(40);
        sb.append("[simple type, class ").append(buildCanonicalName()).append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        SimpleType other = (SimpleType) o;

        // Classes must be identical... 
        if (other._class != this._class) return false;

        // And finally, generic bindings, if any
        JavaType[] p1 = _typeParameters;
        JavaType[] p2 = other._typeParameters;
        if (p1 == null) {
            return (p2 == null) || p2.length == 0;
        }
        if (p2 == null) return false;

        if (p1.length != p2.length) return false;
        for (int i = 0, len = p1.length; i < len; ++i) {
            if (!p1[i].equals(p2[i])) {
                return false;
            }
        }
        return true;
    }
}
