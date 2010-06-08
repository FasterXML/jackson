package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;
import java.util.HashMap;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.type.JavaType;

/**
 * @since 1.5
 * @author tatus
 */
public abstract class TypeDeserializerBase extends TypeDeserializer
{
    protected final TypeIdResolver _idResolver;
    
    protected final JavaType _baseType;

    /**
     * For efficient operation we will lazily build mappings from type ids
     * to actual deserializers, once needed.
     */
    protected final HashMap<String,JsonDeserializer<Object>> _deserializers;
    
    protected TypeDeserializerBase(JavaType baseType, TypeIdResolver idRes)
    {
        _baseType = baseType;
        _idResolver = idRes;
        _deserializers = new HashMap<String,JsonDeserializer<Object>>();
    }

    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();

    public String baseTypeName() { return _baseType.getRawClass().getName(); }

    @Override
    public String getPropertyName() { return null; }
    
    @Override    
    public TypeIdResolver getTypeIdResolver() { return _idResolver; }
    
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
                if (type == null) {
                    throw ctxt.unknownTypeException(_baseType, typeId);
                }
                deser = ctxt.getDeserializerProvider().findValueDeserializer(ctxt.getConfig(), type, null, null);
                _deserializers.put(typeId, deser);
            }
        }
        return deser;
    }
}
