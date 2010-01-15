package org.codehaus.jackson.map.jsontype.impl;

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
    implements JsonTypeResolverBuilder
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
    
    public void init(Class<?> baseType, JsonTypeInfo.Id idType)
    {
        _baseType = baseType;
        // sanity checks
        if (idType == null) {
            throw new IllegalArgumentException("idType can not be null");
        }
        _idType = idType;
        // Let's also initialize property name as per idType default
        _typeProperty = idType.getDefaultPropertyName();
    }
        // sanity checks

    public TypeSerializer buildTypeSerializer() {
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
        case ARRAY:
            return new AsArrayTypeSerializer(idConv);
        case PROPERTY:
            return new AsPropertyTypeSerializer(idConv, _typeProperty);
        case WRAPPER:
            return new AsWrapperTypeSerializer(idConv);
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
    public void registerSubtype(Class<?> type, String name) {
        // TODO Auto-generated method stub
        
    }

    public void setInclusion(JsonTypeInfo.As includeAs) {
        if (includeAs == null) {
            throw new IllegalArgumentException("includeAs can not be null");
        }
        _includeAs = includeAs;
    }

    public void setTypeProperty(String propName) {
        // ok to have null/empty; will restore to use defaults
        if (propName == null || propName.length() == 0) {
            propName = _idType.getDefaultPropertyName();
        }
        _typeProperty = propName;
    }

}
