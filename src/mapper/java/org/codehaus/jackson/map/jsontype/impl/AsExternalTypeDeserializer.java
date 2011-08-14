package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.type.JavaType;

/**
 * Type deserializer used with {@link As#EXTERNAL_PROPERTY} inclusion mechanism.
 * Functioning is actually very similar, but just because of pre-processing
 * done by {@link org.codehaus.jackson.map.deser.BeanDeserializer}; to basically
 * "move" external type identifier to look like internal one.
 * 
 * @since 1.9
 */
public class AsExternalTypeDeserializer extends AsPropertyTypeDeserializer
{
    public AsExternalTypeDeserializer(JavaType bt, TypeIdResolver idRes, BeanProperty property,
            Class<?> defaultImpl,
            String typePropName)
    {
        super(bt, idRes, property, defaultImpl, typePropName);
    }

    @Override
    public As getTypeInclusion() {
        return As.EXTERNAL_PROPERTY;
    }

}
