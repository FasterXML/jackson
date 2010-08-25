package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.type.JavaType;

public abstract class TypeIdResolverBase
    implements TypeIdResolver
{
    protected final JavaType _baseType;

    protected TypeIdResolverBase(JavaType baseType)
    {
        _baseType = baseType;
    }

    public void init(JavaType bt) {
        /* Standard type id resolvers do not need this;
         * only useful for custom ones.
         */
    }
}
