package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.JsonTypeResolver;
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
    
    public void init(Class<?> baseType, JsonTypeInfo.Id idType) {
        // sanity checks
        if (idType == null) {
            throw new IllegalArgumentException("idType can not be null");
        }
        _idType = idType;
        // Let's also initialize property name as per idType default
        _typeProperty = idType.getDefaultPropertyName();
    }

    public JsonTypeResolver build() {
        // sanity checks
        if (_idType == null) {
            throw new IllegalStateException("Can not build, 'init()' not yet called");
        }
        // TODO Auto-generated method stub
        return null;
    }

    /*
     ********************************************************
     * Construction, configuration
     ********************************************************
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

    /*
     ********************************************************
     * Internal methods
     ********************************************************
      */

}
