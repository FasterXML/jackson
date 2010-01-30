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
    extends TypeBase
{
    /**
     * For generic types we need to keep track of mapping from formal
     * into actual types, to be able to resolve generic signatures.
     */
    protected LinkedHashMap<String,JavaType> _typeParameters;
    
    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    protected SimpleType(Class<?> cls)
    {
        super(cls);
        _typeParameters = null;
    }

    protected JavaType _narrow(Class<?> subclass)
    {
        // Should we check that there is a sub-class relationship?
        return new SimpleType(subclass);
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
        return new SimpleType(cls);
    }

    protected String buildCanonicalName() {
		StringBuilder sb = new StringBuilder();
		sb.append(_class.getName());
		if (_typeParameters != null && _typeParameters.size() > 0) {
			sb.append('<');
			boolean first = true;
			for (JavaType t : _typeParameters.values()) {
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
    
    @Override
    public void bindVariableType(String name, JavaType type)
    {
        if (_typeParameters == null) {
            _typeParameters = new LinkedHashMap<String,JavaType>();
        }
        _typeParameters.put(name, type);
    }
	
    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

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

    public int containedTypeCount() {
        return (_typeParameters == null) ? 0 : _typeParameters.size();
    }
    public JavaType containedType(int index)
    {
        if (index < 0 || _typeParameters == null) return null;
        for (Iterator<JavaType> it = _typeParameters.values().iterator(); it.hasNext(); --index) {
            JavaType t = it.next();
            if (index == 0) return t;
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
        StringBuilder sb = new StringBuilder(40);
        sb.append("[simple type, class ").append(_class.getName());
        if (_typeParameters != null) {
            sb.append('<');
            int count = 0;
            for (Map.Entry<String,JavaType> en : _typeParameters.entrySet()) {
                if (++count > 1) {
                    sb.append(',');
                }
                sb.append(en.getKey());
                sb.append('=');
                sb.append(en.getValue().toString());
            }
            sb.append('>');
        }
        sb.append("]");
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

        if (_typeParameters == null) {
            return (other._typeParameters == null);
        }
        return _typeParameters.equals(other._typeParameters);
    }
}
