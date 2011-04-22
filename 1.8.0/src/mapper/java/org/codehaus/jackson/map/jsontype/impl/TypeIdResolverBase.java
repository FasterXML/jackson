package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

public abstract class TypeIdResolverBase
    implements TypeIdResolver
{
    protected final TypeFactory _typeFactory;

    /**
     * Common base type for all polymorphic instances handled.
     */
    protected final JavaType _baseType;

    protected TypeIdResolverBase(JavaType baseType, TypeFactory typeFactory)
    {
        _baseType = baseType;
        _typeFactory = typeFactory;
    }

    public void init(JavaType bt) {
        /* Standard type id resolvers do not need this;
         * only useful for custom ones.
         */
    }
}
