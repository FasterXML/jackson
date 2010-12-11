package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;

/**
 * @since 1.5
 * @author tatus
 */
public abstract class TypeSerializerBase extends TypeSerializer
{
    protected final TypeIdResolver _idResolver;

    protected final AnnotatedMember _property;
    
    protected final String _propertyName;

    protected TypeSerializerBase(TypeIdResolver idRes,
            AnnotatedMember property, String propertyName)
    {
        _idResolver = idRes;
        _property = property;
        _propertyName = propertyName;
    }

    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();

    @Override
    public String getPropertyName() { return null; }
    
    @Override
    public TypeIdResolver getTypeIdResolver() { return _idResolver; }
}
