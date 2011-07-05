package org.codehaus.jackson.map.deser.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.deser.CreatorProperty;
import org.codehaus.jackson.map.deser.ValueInstantiator;
import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Object that is used to collect arguments for non-default creator
 * (non-default-constructor, or argument-taking factory method)
 * before creator can be called.
 * Since ordering of JSON properties is not guaranteed, this may
 * require buffering of values other than ones being passed to
 * creator.
 */
public final class PropertyBasedCreator
{
    protected final ValueInstantiator _valueInstantiator;
    
    /**
     * Map that contains property objects for either constructor or factory
     * method (whichever one is null: one property for each
     * parameter for that one), keyed by logical property name
     */
    protected final HashMap<String, CreatorProperty> _properties;

    /**
     * If some property values must always have a non-null value (like
     * primitive types do), this array contains such default values.
     */
    protected final Object[]  _defaultValues;
    
    public PropertyBasedCreator(ValueInstantiator valueInstantiator)
    {
        _valueInstantiator = valueInstantiator;
        _properties = new HashMap<String, CreatorProperty>();
        // [JACKSON-372]: primitive types need extra care
        Object[] defValues = null;
        CreatorProperty[] creatorProps = valueInstantiator.getFromObjectArguments();
        for (int i = 0, len = creatorProps.length; i < len; ++i) {
            CreatorProperty prop = creatorProps[i];
            _properties.put(prop.getName(), prop);
            if (prop.getType().isPrimitive()) {
                if (defValues == null) {
                    defValues = new Object[len];
                }
                defValues[i] = ClassUtil.defaultValue(prop.getType().getRawClass());
            }
        }
        _defaultValues = defValues;
    }

    public Collection<CreatorProperty> getCreatorProperties() {
        return _properties.values();
    }
    
    public CreatorProperty findCreatorProperty(String name) {
        return _properties.get(name);
    }
    
    /**
     * Method called when starting to build a bean instance.
     */
    public PropertyValueBuffer startBuilding(JsonParser jp, DeserializationContext ctxt)
    {
        return new PropertyValueBuffer(jp, ctxt, _properties.size());
    }
    
    public Object build(PropertyValueBuffer buffer) throws IOException
    {
        Object bean = _valueInstantiator.createFromObjectWith(buffer.getParameters(_defaultValues));
        // Anything buffered?
        for (PropertyValue pv = buffer.buffered(); pv != null; pv = pv.next) {
            pv.assign(bean);
        }
        return bean;
    }
}
