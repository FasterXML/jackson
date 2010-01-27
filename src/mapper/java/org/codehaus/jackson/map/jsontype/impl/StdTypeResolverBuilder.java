package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
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
    ********************************************************
    * Construction, initialization, actual building
    ********************************************************
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
    
    public TypeSerializer buildTypeSerializer(JavaType baseType)
    {
        TypeIdResolver idRes = idResolver(baseType);
        switch (_includeAs) {
        case WRAPPER_ARRAY:
            return new AsArrayTypeSerializer(_idType, idRes);
        case PROPERTY:
            return new AsPropertyTypeSerializer(_idType, idRes, _typeProperty);
        case WRAPPER_OBJECT:
            return new AsWrapperTypeSerializer(_idType, idRes);
        default:
            throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: "+_includeAs);
        }
    }

    public TypeDeserializer buildTypeDeserializer(JavaType baseType)
    {
        TypeIdResolver idRes = idResolver(baseType);
        
        // First, method for converting type info to type id:
        switch (_includeAs) {
        case WRAPPER_ARRAY:
            return new AsArrayTypeDeserializer(baseType, _idType, idRes);
        case PROPERTY:
            return new AsPropertyTypeDeserializer(baseType, _idType, idRes, _typeProperty);
        case WRAPPER_OBJECT:
            return new AsWrapperTypeDeserializer(baseType, _idType, idRes);
        default:
            throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: "+_includeAs);
        }
    }
    
    /*
     ********************************************************
     * Construction, configuration
     ********************************************************
      */

    /**
     * Method used to add name/class bindings for "Id.NAME" type
     * id method
     */
    public StdTypeResolverBuilder registerSubtype(Class<?> type, String name) {
        // TODO Auto-generated method stub
        return this;
    }

    public StdTypeResolverBuilder inclusion(JsonTypeInfo.As includeAs) {
        if (includeAs == null) {
            throw new IllegalArgumentException("includeAs can not be null");
        }
        _includeAs = includeAs;
        return this;
    }
    
    public StdTypeResolverBuilder typeProperty(String propName)
    {
        // ok to have null/empty; will restore to use defaults
        if (propName == null || propName.length() == 0) {
            propName = _idType.getDefaultPropertyName();
        }
        _typeProperty = propName;
        return this;
    }

    /*
     ********************************************************
     * Internal methods
     ********************************************************
      */

    /**
     * Helper method that will either return configured custom
     * type id resolver, or construct a standard resolver
     * given configuration.
     */
    protected TypeIdResolver idResolver(JavaType baseType)
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
            return new ClassNameIdResolver(baseType);
        case MINIMAL_CLASS:
            return new MinimalClassNameIdResolver(baseType);
        case NAME:
            // !!! @TODO: add name bindings
            return new TypeNameIdResolver(baseType);

        case CUSTOM: // need custom resolver...
        case NONE: // hmmh. should never get this far with 'none'
        }
        throw new IllegalStateException("Do not know how to construct standard type id resolver for idType: "+_idType);
    }
    
}
