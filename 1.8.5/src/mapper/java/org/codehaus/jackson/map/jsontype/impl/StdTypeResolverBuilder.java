package org.codehaus.jackson.map.jsontype.impl;

import java.util.Collection;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.type.JavaType;

/**
 * Default {@link TypeResolverBuilder} implementation.
 *
 * @author tatu
 * @since 1.5
 */
public class StdTypeResolverBuilder
    implements TypeResolverBuilder<StdTypeResolverBuilder>
{
    // Configuration settings:

    protected JsonTypeInfo.Id _idType;

    protected JsonTypeInfo.As _includeAs;

    protected String _typeProperty;

    // Objects
    
    protected TypeIdResolver _customIdResolver;
    
    /*
    /**********************************************************
    /* Construction, initialization, actual building
    /**********************************************************
     */

    public StdTypeResolverBuilder() { }
    
    public StdTypeResolverBuilder init(JsonTypeInfo.Id idType, TypeIdResolver idRes)
    {
        // sanity checks
        if (idType == null) {
            throw new IllegalArgumentException("idType can not be null");
        }
        _idType = idType;
        _customIdResolver = idRes;
        // Let's also initialize property name as per idType default
        _typeProperty = idType.getDefaultPropertyName();
        return this;
    }
    
    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config,
            JavaType baseType, Collection<NamedType> subtypes, BeanProperty property)
    {
        TypeIdResolver idRes = idResolver(config, baseType, subtypes, true, false);
        switch (_includeAs) {
        case WRAPPER_ARRAY:
            return new AsArrayTypeSerializer(idRes, property);
        case PROPERTY:
            return new AsPropertyTypeSerializer(idRes, property, _typeProperty);
        case WRAPPER_OBJECT:
            return new AsWrapperTypeSerializer(idRes, property);
        }
        throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: "+_includeAs);
    }
    
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config,
            JavaType baseType, Collection<NamedType> subtypes, BeanProperty property)
    {
        TypeIdResolver idRes = idResolver(config, baseType, subtypes, false, true);
        
        // First, method for converting type info to type id:
        switch (_includeAs) {
        case WRAPPER_ARRAY:
            return new AsArrayTypeDeserializer(baseType, idRes, property);
        case PROPERTY:
            return new AsPropertyTypeDeserializer(baseType, idRes, property, _typeProperty);
        case WRAPPER_OBJECT:
            return new AsWrapperTypeDeserializer(baseType, idRes, property);
        }
        throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: "+_includeAs);
    }
    
    /*
    /**********************************************************
    /* Construction, configuration
    /**********************************************************
     */

    public StdTypeResolverBuilder inclusion(JsonTypeInfo.As includeAs) {
        if (includeAs == null) {
            throw new IllegalArgumentException("includeAs can not be null");
        }
        _includeAs = includeAs;
        return this;
    }

    /**
     * Method for constructing an instance with specified type property name
     * (property name to use for type id when using "as-property" inclusion).
     */
    public StdTypeResolverBuilder typeProperty(String typeIdPropName)
    {
        // ok to have null/empty; will restore to use defaults
        if (typeIdPropName == null || typeIdPropName.length() == 0) {
            typeIdPropName = _idType.getDefaultPropertyName();
        }
        _typeProperty = typeIdPropName;
        return this;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public String getTypeProperty() { return _typeProperty; }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    /**
     * Helper method that will either return configured custom
     * type id resolver, or construct a standard resolver
     * given configuration.
     */
    protected TypeIdResolver idResolver(MapperConfig<?> config,
            JavaType baseType, Collection<NamedType> subtypes,
            boolean forSer, boolean forDeser)
    {
        // Custom id resolver?
        if (_customIdResolver != null) {
            return _customIdResolver;
        }
        if (_idType == null) {
            throw new IllegalStateException("Can not build, 'init()' not yet called");
        }
        switch (_idType) {
        case CLASS:
            return new ClassNameIdResolver(baseType, config.getTypeFactory());
        case MINIMAL_CLASS:
            return new MinimalClassNameIdResolver(baseType, config.getTypeFactory());
        case NAME:
            return TypeNameIdResolver.construct(config, baseType, subtypes, forSer, forDeser);

        case CUSTOM: // need custom resolver...
        case NONE: // hmmh. should never get this far with 'none'
        }
        throw new IllegalStateException("Do not know how to construct standard type id resolver for idType: "+_idType);
    }
}
