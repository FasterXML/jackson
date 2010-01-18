package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.annotate.JsonTypeInfo.As;

public class AsPropertyTypeDeserializer extends TypeDeserializerBase
{
    protected final String _propertyName;

    public AsPropertyTypeDeserializer(TypeConverter conv, String propName) {
        super(conv);
        _propertyName = propName;
    }

    @Override
    public As getTypeInclusion() {
        return As.PROPERTY;
    }
}
