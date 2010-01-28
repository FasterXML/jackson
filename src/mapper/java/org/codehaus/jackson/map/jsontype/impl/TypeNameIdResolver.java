package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.type.JavaType;

public class TypeNameIdResolver
    extends TypeIdResolverBase
{
    protected TypeNameIdResolver(JavaType baseType)
    {
        super(baseType);
    }

    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.NAME; }
    
    public String idFromValue(Object value)
    {
        // !!! Placeholder impl
        String n = value.getClass().getName();
        int ix = n.lastIndexOf('.');
        return (ix < 0) ? n : n.substring(ix+1);
    }

    public JavaType typeFromId(String id)
    {
        throw new IllegalStateException("Not Yet Implemented");
    }    
}
