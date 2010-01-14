package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.annotate.JsonTypeInfo;

/**
 * Type serializer that includes fully-qualified class name as
 * type identifier
 * 
 * @since 1.5
 * @author tatus
 */
public class ClassNameTypeSerializer extends TypeSerializerBase
{
    public ClassNameTypeSerializer(JsonTypeInfo.As includeAs, String propName) {
        super(includeAs, propName);
    }

    @Override
    protected final String typeAsString(Object value) {
        return value.getClass().getName();
    }
}
