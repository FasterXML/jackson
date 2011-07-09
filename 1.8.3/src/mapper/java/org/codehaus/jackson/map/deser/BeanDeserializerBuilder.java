package org.codehaus.jackson.map.deser;

import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.impl.BeanPropertyMap;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;

/**
 * Builder class used for aggregating deserialization information about
 * a POJO, in order to build a {@link JsonDeserializer} for deserializing
 * intances.
 * 
 * @since 1.7
 */
public class BeanDeserializerBuilder
{
    
    /*
    /**********************************************************
    /* General information about POJO
    /**********************************************************
     */

    final protected BasicBeanDescription _beanDesc;
    
    /*
    /**********************************************************
    /* Accumulated information about properties
    /**********************************************************
     */
    
    /**
     * Properties to deserialize collected so far.
     */
    final protected HashMap<String, SettableBeanProperty> _properties = new HashMap<String, SettableBeanProperty>();

    /**
     * Back-reference properties this bean contains (if any)
     */
    protected HashMap<String, SettableBeanProperty> _backRefProperties;

    /**
     * Set of names of properties that are recognized but are to be ignored for deserialization
     * purposes (meaning no exception is thrown, value is just skipped).
     */
    protected HashSet<String> _ignorableProps;
    
    /**
     * Set of creators (constructors, factory methods) that 
     * bean type has.
     */
    protected CreatorContainer _creators;

    /**
     * Fallback setter used for handling any properties that are not
     * mapped to regular setters. If setter is not null, it will be
     * called once for each such property.
     */
    protected SettableAnyProperty _anySetter;

    /**
     * Flag that can be set to ignore and skip unknown properties.
     * If set, will not throw an exception for unknown properties.
     */
    protected boolean _ignoreAllUnknown;
    
    /*
    /**********************************************************
    /* Construction and setters
    /**********************************************************
     */
    
    public BeanDeserializerBuilder(BasicBeanDescription beanDesc)
    { 
        _beanDesc = beanDesc;
    }
 
    public void setCreators(CreatorContainer creators) {
        _creators = creators;
    }

    /**
     * Method for adding a new property or replacing a property.
     */
    public void addOrReplaceProperty(SettableBeanProperty prop, boolean allowOverride)
    {
        _properties.put(prop.getName(), prop);
    }

    /**
     * Method to add a property setter. Will ensure that there is no
     * unexpected override; if one is found will throw a
     * {@link IllegalArgumentException}.
     */
    public void addProperty(SettableBeanProperty prop)
    {
        SettableBeanProperty old =  _properties.put(prop.getName(), prop);
        if (old != null && old != prop) { // should never occur...
            throw new IllegalArgumentException("Duplicate property '"+prop.getName()+"' for "+_beanDesc.getType());
        }
    }
    
    public void  addBackReferenceProperty(String referenceName, SettableBeanProperty prop)
    {
        if (_backRefProperties == null) {
            _backRefProperties = new HashMap<String, SettableBeanProperty>(4);
        }
        _backRefProperties.put(referenceName, prop);
    }

    /**
     * Method that will add property name as one of properties that can
     * be ignored if not recognized.
     */
    public void addIgnorable(String propName)
    {
        if (_ignorableProps == null) {
            _ignorableProps = new HashSet<String>();
        }
        _ignorableProps.add(propName);
    }

    /**
     * Method that allows accessing all properties that this
     * builder currently contains.
     * 
     * @since 1.8.3
     */
    public Iterator<SettableBeanProperty> getProperties() {
        return _properties.values().iterator();
    }
    
    public boolean hasProperty(String propertyName) {
        return _properties.containsKey(propertyName);
    }
    
    public SettableBeanProperty removeProperty(String name)
    {
        return _properties.remove(name);
    }

    public void setAnySetter(SettableAnyProperty s)
    {
        if (_anySetter != null && s != null) {
            throw new IllegalStateException("_anySetter already set to non-null");
        }
        _anySetter = s;
    }

    public void setIgnoreUnknownProperties(boolean ignore) {
        _ignoreAllUnknown = ignore;
    }
    
    /*
    /**********************************************************
    /* Build method(s)
    /**********************************************************
     */

    public JsonDeserializer<?> build(BeanProperty forProperty)
    {
        BeanPropertyMap propertyMap = new BeanPropertyMap(_properties.values());
        propertyMap.assignIndexes();

        return new BeanDeserializer(_beanDesc.getClassInfo(), _beanDesc.getType(), forProperty,
                _creators, propertyMap, _backRefProperties, _ignorableProps, _ignoreAllUnknown,
                _anySetter);
    }
}
