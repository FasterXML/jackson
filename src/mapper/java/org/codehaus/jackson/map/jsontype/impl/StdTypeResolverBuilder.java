package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.JsonTypeResolverBuilder;

/**
 * Default {@link JsonTypeResolverBuilder} implementation.
 *
 * @author tatu
 * @since 1.5
 */
public class StdTypeResolverBuilder
    implements JsonTypeResolverBuilder<StdTypeResolverBuilder>
{
    // Configuration settings:

    protected Class<?> _baseType;
    
    protected JsonTypeInfo.Id _idType;

    protected JsonTypeInfo.As _includeAs;

    protected String _typeProperty;
    
    /*
    ********************************************************
    * Construction, initialization, actual building
    ********************************************************
     */

    public StdTypeResolverBuilder() {
    }
    
    public StdTypeResolverBuilder init(Class<?> baseType, JsonTypeInfo.Id idType)
    {
        _baseType = baseType;
        // sanity checks
        if (idType == null) {
            throw new IllegalArgumentException("idType can not be null");
        }
        _idType = idType;
        // Let's also initialize property name as per idType default
        _typeProperty = idType.getDefaultPropertyName();
        return this;
    }
    
    public TypeSerializer buildTypeSerializer()
    {
        if (_idType == null) {
            throw new IllegalStateException("Can not build, 'init()' not yet called");
        }

        // First, method for converting type info to type id:
        TypeSerializerBase.TypeConverter idConv;
        switch (_idType) {
        case CLASS:
            idConv = new TypeSerializerBase.ClassNameConverter();
            break;
        case MINIMAL_CLASS:
            idConv = new TypeSerializerBase.MinimalClassNameConverter(_baseType);
            break;
        case NAME:
            // !!! @TODO: add name bindings
            idConv = new TypeSerializerBase.TypeNameConverter();
            break;

        case NONE: // hmmh. should never get this far with 'none'
        case CUSTOM: // need custom resolver...
        default:
            throw new IllegalStateException("Do not know how to construct standard type serializer for idType: "+_idType);
        }

        // And then inclusion mechanism
        switch (_includeAs) {
        case WRAPPER_ARRAY:
            return new AsArrayTypeSerializer(idConv);
        case PROPERTY:
            return new AsPropertyTypeSerializer(idConv, _typeProperty);
        case WRAPPER_OBJECT:
            return new AsWrapperTypeSerializer(idConv);
        default:
            throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: "+_includeAs);
        }
    }

    public TypeDeserializer buildTypeDeserializer()
    {
        if (_idType == null) {
            throw new IllegalStateException("Can not build, 'init()' not yet called");
        }
        
        // First, method for converting type info to type id:
        TypeDeserializerBase.TypeConverter idConv;
        switch (_idType) {
        case CLASS:
            idConv = new TypeDeserializerBase.ClassNameConverter();
            break;
        case MINIMAL_CLASS:
            idConv = new TypeDeserializerBase.MinimalClassNameConverter(_baseType);
            break;
        case NAME:
            // !!! @TODO: add name bindings
            idConv = new TypeDeserializerBase.TypeNameConverter();
            break;

        case NONE: // hmmh. should never get this far with 'none'
        case CUSTOM: // need custom resolver...
        default:
            throw new IllegalStateException("Do not know how to construct standard type serializer for idType: "+_idType);
        }

        // And then inclusion mechanism
        switch (_includeAs) {
        case WRAPPER_ARRAY:
            return new AsArrayTypeDeserializer(_baseType, idConv);
        case PROPERTY:
            return new AsPropertyTypeDeserializer(_baseType, idConv, _typeProperty);
        case WRAPPER_OBJECT:
            return new AsWrapperTypeDeserializer(_baseType, idConv);
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
}
