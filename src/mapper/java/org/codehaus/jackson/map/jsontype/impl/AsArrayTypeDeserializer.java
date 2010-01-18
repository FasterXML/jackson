package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.annotate.JsonTypeInfo;

public class AsArrayTypeDeserializer extends TypeDeserializerBase
{
    public AsArrayTypeDeserializer(TypeConverter conv)
    {
        super(conv);
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() {
        return JsonTypeInfo.As.ARRAY;
    }
}
