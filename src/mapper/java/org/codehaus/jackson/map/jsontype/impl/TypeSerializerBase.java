package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;

/**
 * @since 1.5
 * @author tatus
 */
public abstract class TypeSerializerBase extends TypeSerializer
{
    protected final JsonTypeInfo.Id _idType;
    protected final TypeIdResolver _idResolver;
    
    protected TypeSerializerBase(JsonTypeInfo.Id idType, TypeIdResolver idRes)
    {
        _idType = idType;
        _idResolver = idRes;
    }

    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();

    @Override
    public JsonTypeInfo.Id getTypeId() { return _idType; }

    // base implementation returns null; ones that use property name need to override
    @Override
    public String propertyName() { return null; }
}
