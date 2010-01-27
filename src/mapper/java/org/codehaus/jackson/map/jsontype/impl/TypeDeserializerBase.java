package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;
import java.util.HashMap;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.type.JavaType;

/**
 * @since 1.5
 * @author tatus
 */
public abstract class TypeDeserializerBase extends TypeDeserializer
{
    protected final TypeIdResolver _idResolver;

    protected final JsonTypeInfo.Id _idType;
    
    protected final JavaType _baseType;

    /**
     * For efficient operation we will lazily build mappings from type ids
     * to actual deserializers, once needed.
     */
    protected final HashMap<String,JsonDeserializer<Object>> _deserializers;
    
    protected TypeDeserializerBase(JavaType baseType, JsonTypeInfo.Id idType,
            TypeIdResolver idRes)
    {
        _baseType = baseType;
        _idType = idType;
        _idResolver = idRes;
        _deserializers = new HashMap<String,JsonDeserializer<Object>>();
    }

    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();
    
    @Override
    public final JsonTypeInfo.Id getTypeId() { return _idType; }

    // base implementation returns null; ones that use property name need to override
    @Override
    public String propertyName() { return null; }

    public String baseTypeName() { return _baseType.getRawClass().getName(); }

    /*
     ************************************************************
     * Helper methods for sub-classes
     ************************************************************
     */

    protected final JsonDeserializer<Object> _findDeserializer(DeserializationContext ctxt, String typeId)
        throws IOException, JsonProcessingException
    {
        JsonDeserializer<Object> deser;
        synchronized (_deserializers) {
            deser = _deserializers.get(typeId);
            if (deser == null) {
                JavaType type = _idResolver.typeFromId(typeId);
                deser = ctxt.getDeserializerProvider().findValueDeserializer(ctxt.getConfig(), type, null, null);
                _deserializers.put(typeId, deser);
            }
        }
        return deser;
    }
}
