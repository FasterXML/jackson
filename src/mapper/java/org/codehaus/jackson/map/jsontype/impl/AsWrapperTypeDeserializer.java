package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.annotate.JsonTypeInfo.As;

public class AsWrapperTypeDeserializer extends TypeDeserializerBase
{
    public AsWrapperTypeDeserializer(TypeConverter c) { super(c); }

    @Override
    public As getTypeInclusion() {
        return As.WRAPPER;
    }
}
