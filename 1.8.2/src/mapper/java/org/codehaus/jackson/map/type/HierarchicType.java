package org.codehaus.jackson.map.type;

import java.lang.reflect.*;

/**
 * Simple replacement for {@link java.lang.Class} (and/or various Type subtypes)
 * that is used as part of single-path extends/implements chain to express
 * specific relationship between one subtype and one supertype. This is needed
 * for resolving type parameters. Instances are doubly-linked so that chain
 * can be traversed in both directions
 * 
 * @since 1.6
 */
public class HierarchicType
{
    /**
     * Type which will be either plain {@link java.lang.Class} or
     * {@link java.lang.reflect.ParameterizedType}.
     */
    protected final Type _actualType;
    
    protected final Class<?> _rawClass;

    protected final ParameterizedType _genericType;
    
    protected HierarchicType _superType;

    protected HierarchicType _subType;
    
    public HierarchicType(Type type)
    {
        this._actualType = type;
        if (type instanceof Class<?>) {
            _rawClass = (Class<?>) type;
            _genericType = null;
        } else if (type instanceof ParameterizedType) {
            _genericType = (ParameterizedType) type;
            _rawClass = (Class<?>) _genericType.getRawType();
        } else { // should never happen... can't extend GenericArrayType?
            throw new IllegalArgumentException("Type "+type.getClass().getName()+" can not be used to construct HierarchicType");
        }
    }
    
    public void setSuperType(HierarchicType sup) { _superType = sup; }
    public HierarchicType getSuperType() { return _superType; }
    public void setSubType(HierarchicType sub) { _subType = sub; }
    public HierarchicType getSubType() { return _subType; }
    
    public boolean isGeneric() { return _genericType != null; }
    public ParameterizedType asGeneric() { return _genericType; }

    public Class<?> getRawClass() { return _rawClass; }
    
    @Override
    public String toString() {
        if (_genericType != null) {
            return _genericType.toString();
        }
        return _rawClass.getName();
    }
    
}
